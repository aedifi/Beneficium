package aedifi.bene.util;

import aedifi.bene.api.command.CommandModel.CommandArgumentSpec;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    public static final String DEFAULT_NO_PERMISSION = "You do not have permission to use this command.";
    public static final String DEFAULT_INVALID_SUBCOMMAND = "That is not a valid argument: {subcommand}";
    private static final String STANDARD_PERSONAL_PREFIX = "&7";
    private static final String STANDARD_BROADCAST_PREFIX = "&7&o";

    private MessageUtil() {}

    public static Component component(final String message) {
        if (message == null) {
            return Component.empty();
        }
        return LEGACY.deserialize(message);
    }

    public static Component errorOrDefault(final String customMessage, final String fallback) {
        final String message = (customMessage == null || customMessage.isBlank()) ? fallback : customMessage;
        return component(prefixError(message));
    }

    public static Component error(final String message) {
        return component(prefixError(message));
    }

    public static Component invalidSubcommand(final String subcommand, final String customMessage) {
        final String template = (customMessage == null || customMessage.isBlank())
                ? DEFAULT_INVALID_SUBCOMMAND
                : customMessage;
        final String message = template
                .replace("{subcommand}", subcommand)
                .replace("{sub}", subcommand);
        return component(prefixError(message));
    }

    public static Component personal(final String message) {
        return component(prefixIfPlain(message, STANDARD_PERSONAL_PREFIX));
    }

    public static Component broadcast(final String message) {
        return component(prefixIfPlain(message, STANDARD_BROADCAST_PREFIX));
    }

    public static String usageForArguments(final String root, final List<CommandArgumentSpec> arguments) {
        final StringBuilder usage = new StringBuilder("&r/").append(root);
        for (final CommandArgumentSpec spec : arguments) {
            if (spec.required()) {
                usage.append(" <").append(spec.name()).append(">");
            } else {
                usage.append(" [").append(spec.name()).append("]");
            }
        }
        return usage.toString();
    }

    public static String usageForSubcommands(final String root, final List<String> subcommands) {
        final String joined = subcommands.stream()
                .distinct()
                .sorted()
                .reduce((a, b) -> a + " | " + b)
                .orElse("");
        if (joined.isEmpty()) {
            return "&r/" + root;
        }
        return "&r/" + root + " <" + joined + ">";
    }

    private static String prefixError(final String message) {
        if (message.startsWith("&")) {
            return message;
        }
        return "&c" + message;
    }

    private static String prefixIfPlain(final String message, final String prefix) {
        if (message == null) {
            return prefix;
        }
        if (message.startsWith("&")) {
            return message;
        }
        return prefix + message;
    }
}
