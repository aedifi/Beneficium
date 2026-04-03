package aedifi.bene.command;

import static aedifi.bene.api.command.CommandModel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CommandDefinitionParser {
    public List<CommandDefinition> parse(final String yamlSource, final String sourceName) {
        final YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(yamlSource);
        } catch (final InvalidConfigurationException ex) {
            throw new ParseException("Invalid YAML in " + sourceName + ": " + ex.getMessage());
        }

        final List<Map<?, ?>> commandMaps = yaml.getMapList("commands");
        final List<CommandDefinition> definitions = new ArrayList<>(commandMaps.size());
        for (int i = 0; i < commandMaps.size(); i++) {
            definitions.add(parseCommandMap(commandMaps.get(i), i, sourceName));
        }
        return List.copyOf(definitions);
    }

    private CommandDefinition parseCommandMap(final Map<?, ?> rawMap, final int index, final String sourceName) {
        final String base = "commands[" + index + "]";
        final String id = requireString(rawMap, "id", base, sourceName);
        final String root = requireString(rawMap, "root", base, sourceName);
        final List<String> aliases = listOfStrings(rawMap.get("aliases"), base + ".aliases", sourceName);
        final CommandSenderType senderType = parseSenderType(rawMap.get("sender"), base + ".sender", sourceName);
        final String permission = optionalString(rawMap.get("permission"), base + ".permission", sourceName);
        final List<CommandArgumentSpec> arguments = parseArguments(rawMap.get("arguments"), base + ".arguments", sourceName);
        final CommandActionSpec action = parseAction(rawMap.get("action"), base + ".action", sourceName);
        final CommandFeedbackSpec feedback = parseFeedback(rawMap.get("messages"), base + ".messages", sourceName);
        return new CommandDefinition(id, root, aliases, senderType, permission, arguments, action, feedback);
    }

    private List<CommandArgumentSpec> parseArguments(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw parseError(sourceName, keyPath, "Expected a YAML list.");
        }
        final List<CommandArgumentSpec> specs = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final Object item = list.get(i);
            final String base = keyPath + "[" + i + "]";
            if (!(item instanceof Map<?, ?> map)) {
                throw parseError(sourceName, base, "Expected a YAML object.");
            }
            final String name = requireString(map, "name", base, sourceName);
            final String typeRaw = requireString(map, "type", base, sourceName);
            final CommandArgumentType type = parseEnum(CommandArgumentType.class, typeRaw, sourceName, base + ".type");
            final boolean required = optionalBoolean(map.get("required"), base + ".required", sourceName, true);
            final List<String> suggestions = listOfStrings(map.get("suggestions"), base + ".suggestions", sourceName);
            specs.add(new CommandArgumentSpec(name, type, required, suggestions));
        }
        return List.copyOf(specs);
    }

    private CommandActionSpec parseAction(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return new CommandActionSpec("");
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw parseError(sourceName, keyPath, "Expected a YAML object.");
        }
        final String staticText = optionalString(map.get("text"), keyPath + ".text", sourceName);
        final String scriptRef = optionalString(map.get("script"), keyPath + ".script", sourceName);
        final String languageRaw = optionalString(map.get("language"), keyPath + ".language", sourceName);
        final String hostActionRef = optionalString(map.get("host"), keyPath + ".host", sourceName);
        if (scriptRef != null && scriptRef.isBlank()) {
            throw parseError(sourceName, keyPath + ".script", "Expected a non-blank string.");
        }
        if (hostActionRef != null && hostActionRef.isBlank()) {
            throw parseError(sourceName, keyPath + ".host", "Expected a non-blank string.");
        }
        if (languageRaw != null && scriptRef == null) {
            throw parseError(sourceName, keyPath + ".language", "Script language requires a script reference.");
        }
        final int actionKinds = (staticText != null && !staticText.isBlank() ? 1 : 0)
                + (scriptRef != null ? 1 : 0)
                + (hostActionRef != null ? 1 : 0);
        if (actionKinds > 1) {
            throw parseError(sourceName, keyPath, "Action must define only one of text, script, or host.");
        }
        final ScriptLanguage language = scriptRef == null
                ? ScriptLanguage.NONE
                : (languageRaw == null
                        ? ScriptLanguage.LUA
                        : parseEnum(ScriptLanguage.class, languageRaw, sourceName, keyPath + ".language"));
        return new CommandActionSpec(staticText, scriptRef, language, hostActionRef);
    }

    private CommandFeedbackSpec parseFeedback(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return CommandFeedbackSpec.empty();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw parseError(sourceName, keyPath, "Expected a YAML object.");
        }
        final String noPermission = optionalString(map.get("no-permission"), keyPath + ".no-permission", sourceName);
        final String usage = optionalString(map.get("usage"), keyPath + ".usage", sourceName);
        final String invalidSub = optionalString(map.get("invalid-subcommand"), keyPath + ".invalid-subcommand", sourceName);
        return new CommandFeedbackSpec(noPermission, usage, invalidSub);
    }

    private CommandSenderType parseSenderType(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return CommandSenderType.ANY;
        }
        if (!(raw instanceof String senderType)) {
            throw parseError(sourceName, keyPath, "Expected a string enum value.");
        }
        return parseEnum(CommandSenderType.class, senderType, sourceName, keyPath);
    }

    private static <T extends Enum<T>> T parseEnum(
            final Class<T> enumType,
            final String rawValue,
            final String sourceName,
            final String keyPath) {
        try {
            return Enum.valueOf(enumType, rawValue.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            throw parseError(sourceName, keyPath, "Unsupported value: " + rawValue);
        }
    }

    private String optionalString(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String value)) {
            throw parseError(sourceName, keyPath, "Expected a string.");
        }
        return value;
    }

    private String requireString(final Map<?, ?> map, final String key, final String keyPath, final String sourceName) {
        final Object value = map.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw parseError(sourceName, keyPath + "." + key, "Expected a non-blank string.");
        }
        return text;
    }

    private boolean optionalBoolean(final Object raw, final String keyPath, final String sourceName, final boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        if (!(raw instanceof Boolean value)) {
            throw parseError(sourceName, keyPath, "Expected a boolean.");
        }
        return value;
    }

    private List<String> listOfStrings(final Object raw, final String keyPath, final String sourceName) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw parseError(sourceName, keyPath, "Expected a YAML list.");
        }
        final List<String> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final Object item = list.get(i);
            if (!(item instanceof String value)) {
                throw parseError(sourceName, keyPath + "[" + i + "]", "Expected a string.");
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private static ParseException parseError(
            final String sourceName,
            final String keyPath,
            final String message) {
        return new ParseException(sourceName + " at " + keyPath + ": " + message);
    }
}
