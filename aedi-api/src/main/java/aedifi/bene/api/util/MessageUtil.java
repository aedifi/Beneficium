package aedifi.bene.api.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    public static final String DEFAULT_NO_PERMISSION = "You do not have permission to use this command.";
    public static final String DEFAULT_INVALID_SUBCOMMAND = "That is not a valid argument: {subcommand}";

    private MessageUtil() {}

    public static Component component(final String message) {
        if (message == null) {
            return Component.empty();
        }
        return LEGACY.deserialize(message);
    }

    public static Component personal(final String message) {
        return component("&7" + (message == null ? "" : message));
    }

    public static Component error(final String message) {
        return component("&c" + (message == null ? "" : message));
    }

    public static Component success(final String message) {
        return component("&3" + (message == null ? "" : message));
    }

    public static Component broadcast(final String message) {
        return component("&7&o" + (message == null ? "" : message));
    }

    public static Component errorOrDefault(final String customMessage, final String fallback) {
        final String message = (customMessage == null || customMessage.isBlank()) ? fallback : customMessage;
        return error(message);
    }

    public static Component invalidSubcommand(final String subcommand, final String customMessage) {
        final String template = (customMessage == null || customMessage.isBlank())
                ? DEFAULT_INVALID_SUBCOMMAND
                : customMessage;
        final String message = template
                .replace("{subcommand}", subcommand)
                .replace("{sub}", subcommand);
        return error(message);
    }
}
