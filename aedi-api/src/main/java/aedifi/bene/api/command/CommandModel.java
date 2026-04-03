package aedifi.bene.api.command;

import java.util.List;
import org.bukkit.command.CommandSender;

public final class CommandModel {
    private CommandModel() {}

    public record CommandDefinition(
            String id,
            String root,
            List<String> aliases,
            CommandSenderType senderType,
            String permission,
            List<CommandArgumentSpec> arguments,
            CommandActionSpec action,
            CommandFeedbackSpec feedback) {
        public CommandDefinition(
                final String id,
                final String root,
                final List<String> aliases,
                final CommandSenderType senderType,
                final String permission,
                final List<CommandArgumentSpec> arguments,
                final CommandActionSpec action) {
            this(id, root, aliases, senderType, permission, arguments, action, CommandFeedbackSpec.empty());
        }
    }

    public record CommandArgumentSpec(
            String name, CommandArgumentType type, boolean required, List<String> suggestions) {}

    public record CommandActionSpec(
            String staticText,
            String scriptRef,
            ScriptLanguage scriptLanguage,
            String hostActionRef) {
        public CommandActionSpec(
                final String staticText,
                final String scriptRef,
                final ScriptLanguage scriptLanguage) {
            this(staticText, scriptRef, scriptLanguage, null);
        }

        public CommandActionSpec(final String staticText) {
            this(staticText, null, ScriptLanguage.NONE, null);
        }

        public static CommandActionSpec script(final ScriptLanguage language, final String scriptRef) {
            return new CommandActionSpec(null, scriptRef, language, null);
        }

        public static CommandActionSpec host(final String hostActionRef) {
            return new CommandActionSpec(null, null, ScriptLanguage.NONE, hostActionRef);
        }
    }

    public record CommandFeedbackSpec(
            String noPermissionMessage,
            String usageMessage,
            String invalidSubcommandMessage) {
        private static final CommandFeedbackSpec EMPTY = new CommandFeedbackSpec(null, null, null);

        public static CommandFeedbackSpec empty() {
            return EMPTY;
        }
    }

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

    public enum ScriptLanguage {
        NONE,
        LUA
    }

    public static final class ParseException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public ParseException(final String message) {
            super(message);
        }
    }
}
