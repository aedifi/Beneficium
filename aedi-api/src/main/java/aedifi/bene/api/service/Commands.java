package aedifi.bene.api.service;

import aedifi.bene.api.module.ModuleId;
import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public interface Commands {
    record Registration(
            String name,
            List<String> aliases,
            String description,
            String usage,
            String permission) {
        public Registration(final String name) {
            this(name, List.of(), null, null, null);
        }

        public Registration {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Command name cannot be blank.");
            }
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    default void register(final ModuleId owner, final String commandName, final CommandExecutor executor) {
        register(owner, new Registration(commandName), executor, null);
    }

    default void register(
            final ModuleId owner,
            final String commandName,
            final CommandExecutor executor,
            final TabCompleter tabCompleter) {
        register(owner, new Registration(commandName), executor, tabCompleter);
    }

    default void register(final ModuleId owner, final Registration registration, final CommandExecutor executor) {
        register(owner, registration, executor, null);
    }

    void register(ModuleId owner, Registration registration, CommandExecutor executor, TabCompleter tabCompleter);

    void unregisterOwnerCommands(ModuleId owner);
}
