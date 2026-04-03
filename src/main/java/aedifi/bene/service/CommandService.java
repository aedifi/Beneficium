package aedifi.bene.service;

import aedifi.bene.api.command.CommandModel.CommandActionSpec;
import aedifi.bene.api.command.CommandModel.CommandArgumentSpec;
import aedifi.bene.api.command.CommandModel.CommandArgumentType;
import aedifi.bene.api.command.CommandModel.CommandDefinition;
import aedifi.bene.api.command.CommandModel.CommandExecutionContext;
import aedifi.bene.api.command.CommandModel.CommandFeedbackSpec;
import aedifi.bene.api.command.CommandModel.ScriptLanguage;
import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Commands;
import aedifi.bene.api.service.Logging;
import aedifi.bene.service.LoggingService;
import aedifi.bene.command.CommandInvocation;
import aedifi.bene.command.CommandTreeCompiler;
import aedifi.bene.script.LuaCommandRuntime;
import aedifi.bene.util.MessageUtil;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandService implements Commands {
    private final CommandTreeCompiler compiler;
    private final Map<ModuleId, List<CommandDefinition>> moduleDefinitions = new HashMap<>();
    private final Map<ModuleId, List<LiteralArgumentBuilder<CommandExecutionContext>>> compiledTrees = new HashMap<>();
    private final Map<String, ToIntFunction<CommandInvocation>> hostActions = new HashMap<>();
    private final LuaCommandRuntime luaRuntime;
    private final Logging logging;

    public CommandService(final JavaPlugin plugin, final Logging logging) {
        this.compiler = new CommandTreeCompiler();
        this.luaRuntime = plugin == null ? null : new LuaCommandRuntime(plugin, logging);
        this.logging = logging;
    }

    public static CommandService forTests() {
        return new CommandService(null, LoggingService.forTests());
    }

    @Override
    public void registerDefinition(final ModuleId owner, final CommandDefinition definition) {
        final List<LiteralArgumentBuilder<CommandExecutionContext>> compiled =
                compiler.compile(definition, this::executeDefinition);
        moduleDefinitions.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(definition);
        compiledTrees.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).addAll(compiled);
    }

    @Override
    public void unregisterOwnerCommands(final ModuleId owner) {
        moduleDefinitions.remove(owner);
        compiledTrees.remove(owner);
    }

    public void clearAllDefinitions() {
        moduleDefinitions.clear();
        compiledTrees.clear();
    }

    @Override
    public Map<ModuleId, List<CommandDefinition>> definitionsByModule() {
        return Map.copyOf(moduleDefinitions);
    }

    public void shutdown() {
        hostActions.clear();
        if (luaRuntime != null) {
            luaRuntime.shutdown();
        }
    }

    public void registerHostAction(final String actionRef, final ToIntFunction<CommandInvocation> handler) {
        if (actionRef == null || actionRef.isBlank()) {
            throw new IllegalArgumentException("Host action reference cannot be blank.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Host action handler cannot be null.");
        }
        hostActions.put(normalizeActionRef(actionRef), handler);
    }

    public void unregisterHostAction(final String actionRef) {
        if (actionRef == null || actionRef.isBlank()) {
            return;
        }
        hostActions.remove(normalizeActionRef(actionRef));
    }

    public int executeHostAction(
            final String actionRef,
            final CommandSender sender,
            final Map<String, Object> arguments) {
        final Map<String, Object> invocationArguments = arguments == null
                ? Map.of()
                : new LinkedHashMap<>(arguments);
        return executeHostAction(actionRef, new CommandInvocation(null, sender, invocationArguments));
    }

    private int executeDefinition(
            final CommandDefinition definition,
            final CommandContext<CommandExecutionContext> context) {
        final CommandSender sender = context.getSource().sender();
        final CommandFeedbackSpec feedback = definition.feedback() == null
                ? CommandFeedbackSpec.empty()
                : definition.feedback();
        if (definition.permission() != null
                && !definition.permission().isBlank()
                && !sender.hasPermission(definition.permission())) {
            sender.sendMessage(MessageUtil.errorOrDefault(
                    feedback.noPermissionMessage(),
                    MessageUtil.DEFAULT_NO_PERMISSION));
            return 0;
        }

        if (missingRequiredArguments(definition, context)) {
            final String usage = feedback.usageMessage();
            sender.sendMessage(MessageUtil.component(
                    (usage == null || usage.isBlank()) ? defaultUsage(definition) : usage));
            return 0;
        }

        if (isUsageShortcutRequested(definition, context)) {
            final String usage = feedback.usageMessage();
            sender.sendMessage(MessageUtil.component(
                    (usage == null || usage.isBlank()) ? defaultUsage(definition) : usage));
            return 1;
        }
        final CommandActionSpec action = definition.action();
        if (action == null) {
            return 1;
        }
        if ((action.staticText() == null || action.staticText().isBlank())
                && action.scriptRef() == null
                && action.hostActionRef() == null) {
            return 1;
        }

        if (action.scriptLanguage() == ScriptLanguage.LUA && action.scriptRef() != null) {
            final Map<String, Object> arguments = resolveArguments(definition.arguments(), context);
            final CommandInvocation invocation = new CommandInvocation(
                    definition,
                    sender,
                    arguments);
            if (luaRuntime == null) {
                logging.warn("command", "Lua runtime unavailable in test mode.");
                return 0;
            }
            final boolean ok = luaRuntime.execute(action.scriptRef(), invocation);
            return ok ? 1 : 0;
        }

        if (action.hostActionRef() != null && !action.hostActionRef().isBlank()) {
            final Map<String, Object> arguments = resolveArguments(definition.arguments(), context);
            final CommandInvocation invocation = new CommandInvocation(definition, sender, arguments);
            return executeHostAction(action.hostActionRef(), invocation);
        }

        if (action.staticText() != null && !action.staticText().isBlank()) {
            sender.sendMessage(MessageUtil.component(action.staticText()));
            return 1;
        }

        return 1;
    }

    private boolean missingRequiredArguments(
            final CommandDefinition definition,
            final CommandContext<CommandExecutionContext> context) {
        if (definition.arguments().isEmpty()) {
            return false;
        }
        for (final CommandArgumentSpec spec : definition.arguments()) {
            if (!spec.required()) {
                continue;
            }
            final String name = spec.name();
            final Class<?> typeClass = argumentClass(spec.type());
            try {
                context.getArgument(name, typeClass);
            } catch (final IllegalArgumentException ex) {
                return true;
            }
        }
        return false;
    }

    private String defaultUsage(final CommandDefinition definition) {
        return MessageUtil.usageForArguments(definition.root(), definition.arguments());
    }

    private boolean isUsageShortcutRequested(
            final CommandDefinition definition,
            final CommandContext<CommandExecutionContext> context) {
        if (definition.arguments().isEmpty()) {
            return false;
        }
        if (firstArgumentReservesHelpTokens(definition.arguments().getFirst())) {
            return false;
        }

        final String[] tokens = tokenize(context.getInput());
        if (tokens.length != 2) {
            return false;
        }
        final String token = tokens[1].toLowerCase(Locale.ROOT);
        return token.equals("?") || token.equals("help");
    }

    private boolean firstArgumentReservesHelpTokens(final CommandArgumentSpec firstArg) {
        for (final String suggestion : firstArg.suggestions()) {
            final String normalized = suggestion.toLowerCase(Locale.ROOT);
            if (normalized.equals("?") || normalized.equals("help")) {
                return true;
            }
        }
        return false;
    }

    private String[] tokenize(final String input) {
        final String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toArray(String[]::new);
    }

    private Class<?> argumentClass(final CommandArgumentType type) {
        return switch (type) {
            case WORD, GREEDY_STRING -> String.class;
            case INTEGER -> Integer.class;
            case BOOLEAN -> Boolean.class;
        };
    }

    private Map<String, Object> resolveArguments(
            final List<CommandArgumentSpec> specs,
            final CommandContext<CommandExecutionContext> context) {
        final Map<String, Object> args = new LinkedHashMap<>();
        for (final CommandArgumentSpec spec : specs) {
            final String name = spec.name();
            try {
                final Object value = switch (spec.type()) {
                    case WORD, GREEDY_STRING -> context.getArgument(name, String.class);
                    case INTEGER -> context.getArgument(name, Integer.class);
                    case BOOLEAN -> context.getArgument(name, Boolean.class);
                };
                args.put(name, value);
            } catch (final IllegalArgumentException ex) {
                if (spec.required()) {
                    throw ex;
                }
            }
        }
        return args;
    }

    private int executeHostAction(final String actionRef, final CommandInvocation invocation) {
        if (actionRef == null || actionRef.isBlank()) {
            logging.warn("command", "Host action reference was blank.");
            if (invocation.sender() != null) {
                invocation.sender().sendMessage(MessageUtil.error("This command action is unavailable."));
            }
            return 0;
        }
        final ToIntFunction<CommandInvocation> handler = hostActions.get(normalizeActionRef(actionRef));
        if (handler == null) {
            logging.warn("command", "Host action is not registered: " + actionRef);
            if (invocation.sender() != null) {
                invocation.sender().sendMessage(MessageUtil.error("This command action is unavailable."));
            }
            return 0;
        }
        try {
            return handler.applyAsInt(invocation);
        } catch (final Exception ex) {
            logging.error("command", "Host action failed: " + actionRef, ex);
            if (invocation.sender() != null) {
                invocation.sender().sendMessage(MessageUtil.error("Command execution failed. Check server logs."));
            }
            return 0;
        }
    }

    private static String normalizeActionRef(final String actionRef) {
        return actionRef.toLowerCase(Locale.ROOT);
    }
}
