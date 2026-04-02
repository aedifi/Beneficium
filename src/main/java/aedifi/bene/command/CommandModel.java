package aedifi.bene.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Command-layer types: definitions, Brigadier context, and parse errors.
 */
public final class CommandModel {
    private CommandModel() {}

    public record CommandDefinition(
            String id,
            String root,
            List<String> aliases,
            CommandSenderType senderType,
            String permission,
            List<CommandArgumentSpec> arguments,
            CommandActionSpec action) {}

    public record CommandArgumentSpec(
            String name, CommandArgumentType type, boolean required, List<String> suggestions) {}

    public record CommandActionSpec(String staticText) {}

    public record CommandExecutionContext(CommandSender sender) {}

    public enum CommandSenderType {
        ANY,
        PLAYER_ONLY,
        CONSOLE_ONLY
    }

    public enum CommandArgumentType {
        WORD,
        GREEDY_STRING,
        INTEGER,
        BOOLEAN
    }

    public static final class ParseException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public ParseException(final String message) {
            super(message);
        }
    }
}
