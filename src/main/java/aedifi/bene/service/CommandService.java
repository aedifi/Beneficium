package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Commands;
import aedifi.bene.api.service.Logging;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandService implements Commands {
    private final JavaPlugin plugin;
    private final Logging logging;
    private final CommandMap commandMap;
    private final Constructor<PluginCommand> pluginCommandConstructor;
    private final Field knownCommandsField;
    private final Map<ModuleId, List<RegisteredCommand>> commandsByOwner = new HashMap<>();
    private final Map<String, RegisteredCommand> commandsByName = new HashMap<>();

    public CommandService(final JavaPlugin plugin, final Logging logging) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logging = Objects.requireNonNull(logging, "logging");
        this.commandMap = resolveCommandMap(plugin);
        this.pluginCommandConstructor = resolvePluginCommandConstructor();
        this.knownCommandsField = resolveKnownCommandsField(commandMap);
    }

    @Override
    public void register(
            final ModuleId owner,
            final Registration registration,
            final CommandExecutor executor,
            final TabCompleter tabCompleter) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(registration, "registration");
        Objects.requireNonNull(executor, "executor");

        final String commandName = normalizeCommandName(registration.name());
        final RegisteredCommand previous = commandsByName.get(commandName);
        if (previous != null) {
            if (!previous.owner().equals(owner)) {
                throw new IllegalStateException("Command already registered: " + commandName);
            }
            unregisterRegisteredCommand(previous);
            removeOwnerEntry(owner, previous);
        }

        final ResolvedCommand resolved = resolveCommand(registration);
        applyRegistration(resolved.command(), registration, executor, tabCompleter);

        final RegisteredCommand registered = new RegisteredCommand(
                owner,
                commandName,
                captureLabels(resolved.command()),
                resolved.command(),
                resolved.dynamic());
        commandsByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(registered);
        commandsByName.put(commandName, registered);

        logging.info("command", "Registered /" + commandName + " for " + owner.value() + ".");
    }

    @Override
    public void unregisterOwnerCommands(final ModuleId owner) {
        final List<RegisteredCommand> registeredCommands = commandsByOwner.remove(owner);
        if (registeredCommands == null) {
            return;
        }
        for (final RegisteredCommand registeredCommand : registeredCommands) {
            unregisterRegisteredCommand(registeredCommand);
        }
    }

    public void shutdown() {
        final List<ModuleId> owners = new ArrayList<>(commandsByOwner.keySet());
        for (final ModuleId owner : owners) {
            unregisterOwnerCommands(owner);
        }
    }

    private ResolvedCommand resolveCommand(final Registration registration) {
        final String commandName = normalizeCommandName(registration.name());
        final Command existing = commandMap.getCommand(commandName);
        if (existing != null) {
            if (!(existing instanceof PluginCommand pluginCommand)) {
                throw new IllegalStateException("Command name is already claimed: " + commandName);
            }
            if (!plugin.equals(pluginCommand.getPlugin())) {
                throw new IllegalStateException("Command name is already claimed by another plugin: " + commandName);
            }
            return new ResolvedCommand(pluginCommand, false);
        }

        final PluginCommand pluginCommand = createPluginCommand(commandName);
        pluginCommand.setAliases(registration.aliases());
        commandMap.register(fallbackPrefix(), pluginCommand);
        return new ResolvedCommand(pluginCommand, true);
    }

    private PluginCommand createPluginCommand(final String commandName) {
        try {
            return pluginCommandConstructor.newInstance(commandName, plugin);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create Bukkit command: " + commandName, ex);
        }
    }

    private void applyRegistration(
            final PluginCommand command,
            final Registration registration,
            final CommandExecutor executor,
            final TabCompleter tabCompleter) {
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
        if (!registration.aliases().isEmpty()) {
            command.setAliases(registration.aliases());
        }
        if (registration.description() != null && !registration.description().isBlank()) {
            command.setDescription(registration.description());
        }
        if (registration.usage() != null && !registration.usage().isBlank()) {
            command.setUsage(registration.usage());
        }
        if (registration.permission() != null && !registration.permission().isBlank()) {
            command.setPermission(registration.permission());
        }
    }

    private Set<String> captureLabels(final PluginCommand command) {
        final Set<String> labels = new LinkedHashSet<>();
        labels.add(normalizeCommandName(command.getName()));
        for (final String alias : command.getAliases()) {
            labels.add(normalizeCommandName(alias));
        }
        return Set.copyOf(labels);
    }

    private void unregisterRegisteredCommand(final RegisteredCommand registeredCommand) {
        commandsByName.remove(registeredCommand.name());
        registeredCommand.command().setExecutor(null);
        registeredCommand.command().setTabCompleter(null);

        if (!registeredCommand.dynamic()) {
            return;
        }

        registeredCommand.command().unregister(commandMap);
        final Map<String, Command> knownCommands = knownCommands();
        if (knownCommands == null) {
            return;
        }
        for (final String label : registeredCommand.labels()) {
            knownCommands.remove(label);
            knownCommands.remove(fallbackPrefix() + ":" + label);
        }
    }

    private void removeOwnerEntry(final ModuleId owner, final RegisteredCommand registeredCommand) {
        final List<RegisteredCommand> ownerCommands = commandsByOwner.get(owner);
        if (ownerCommands == null) {
            return;
        }
        ownerCommands.remove(registeredCommand);
        if (ownerCommands.isEmpty()) {
            commandsByOwner.remove(owner);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> knownCommands() {
        if (knownCommandsField == null) {
            return null;
        }
        try {
            return (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (final IllegalAccessException ex) {
            throw new IllegalStateException("Failed to access known command registry.", ex);
        }
    }

    private static CommandMap resolveCommandMap(final JavaPlugin plugin) {
        try {
            final Method method = plugin.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(plugin.getServer());
        } catch (final ReflectiveOperationException ignored) {
            try {
                final Object pluginManager = plugin.getServer().getPluginManager();
                final Field field = pluginManager.getClass().getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(pluginManager);
            } catch (final ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to locate Bukkit command map.", ex);
            }
        }
    }

    private static Constructor<PluginCommand> resolvePluginCommandConstructor() {
        try {
            final Constructor<PluginCommand> constructor =
                    PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to access PluginCommand constructor.", ex);
        }
    }

    private static Field resolveKnownCommandsField(final CommandMap commandMap) {
        Class<?> type = commandMap.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return field;
            } catch (final NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private String fallbackPrefix() {
        return plugin.getName().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCommandName(final String commandName) {
        return commandName.toLowerCase(Locale.ROOT).trim();
    }

    private record ResolvedCommand(PluginCommand command, boolean dynamic) {}

    private record RegisteredCommand(
            ModuleId owner,
            String name,
            Set<String> labels,
            PluginCommand command,
            boolean dynamic) {}
}
