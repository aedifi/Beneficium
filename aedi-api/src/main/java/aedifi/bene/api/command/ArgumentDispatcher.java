package aedifi.bene.api.command;

import aedifi.bene.api.util.MessageUtil;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class ArgumentDispatcher {
    @FunctionalInterface
    public interface ArgumentHandler {
        boolean execute(CommandSender sender, String[] args);
    }

    public record ArgumentSpec(
            String name,
            List<String> aliases,
            String permission,
            ArgumentHandler handler) {
        public ArgumentSpec {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Argument name cannot be blank.");
            }
            if (handler == null) {
                throw new IllegalArgumentException("Argument handler cannot be null.");
            }
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    private final String root;
    private final String usagePermission;
    private final List<ArgumentSpec> arguments;
    private final Map<String, ArgumentSpec> indexed = new LinkedHashMap<>();

    public ArgumentDispatcher(
            final String root,
            final String usagePermission,
            final List<ArgumentSpec> arguments) {
        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException("Root command cannot be blank.");
        }
        this.root = root;
        this.usagePermission = usagePermission;
        this.arguments = arguments == null ? List.of() : List.copyOf(arguments);
        indexArguments(this.arguments);
    }

    public boolean execute(final CommandSender sender, final String[] rawArgs) {
        final String[] args = rawArgs == null ? new String[0] : rawArgs;
        if (args.length == 0) {
            return sendUsage(sender);
        }

        final String token = normalize(args[0]);
        final ArgumentSpec argument = indexed.get(token);
        if (argument == null) {
            if (!hasAnyPermission(sender)) {
                sender.sendMessage(MessageUtil.errorOrDefault(null, MessageUtil.DEFAULT_NO_PERMISSION));
                return true;
            }
            sender.sendMessage(MessageUtil.invalidSubcommand(token, null));
            return true;
        }

        if (!hasPermission(sender, argument.permission())) {
            sender.sendMessage(MessageUtil.errorOrDefault(null, MessageUtil.DEFAULT_NO_PERMISSION));
            return true;
        }

        final String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        return argument.handler().execute(sender, remaining);
    }

    public List<String> complete(final CommandSender sender, final String[] rawArgs) {
        final String[] args = rawArgs == null ? new String[0] : rawArgs;
        if (args.length == 0 || args.length == 1) {
            final String prefix = args.length == 0 ? "" : normalize(args[0]);
            return arguments.stream()
                    .filter(argument -> hasPermission(sender, argument.permission()))
                    .map(ArgumentSpec::name)
                    .filter(name -> normalize(name).startsWith(prefix))
                    .sorted()
                    .toList();
        }
        return List.of();
    }

    private boolean sendUsage(final CommandSender sender) {
        if (!hasPermission(sender, usagePermission)) {
            sender.sendMessage(MessageUtil.errorOrDefault(null, MessageUtil.DEFAULT_NO_PERMISSION));
            return true;
        }
        sender.sendMessage(MessageUtil.component(formatUsageForSubcommands(
                root,
                arguments.stream()
                        .map(ArgumentSpec::name)
                        .toList())));
        return true;
    }

    private boolean hasAnyPermission(final CommandSender sender) {
        if (isPublic(usagePermission)) {
            return true;
        }
        for (final ArgumentSpec argument : arguments) {
            if (isPublic(argument.permission()) || sender.hasPermission(argument.permission())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPermission(final CommandSender sender, final String permission) {
        if (isPublic(permission)) {
            return true;
        }
        return sender.hasPermission(permission);
    }

    private static boolean isPublic(final String permission) {
        return permission == null || permission.isBlank();
    }

    private void indexArguments(final List<ArgumentSpec> specs) {
        for (final ArgumentSpec spec : specs) {
            registerToken(spec.name(), spec);
            for (final String alias : spec.aliases()) {
                registerToken(alias, spec);
            }
        }
    }

    private void registerToken(final String token, final ArgumentSpec argument) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Argument token cannot be blank.");
        }
        final String normalized = normalize(token);
        final ArgumentSpec previous = indexed.putIfAbsent(normalized, argument);
        if (previous != null && previous != argument) {
            throw new IllegalArgumentException("Duplicate argument token: " + token);
        }
    }

    public static String formatUsageForSubcommands(final String root, final List<String> subcommands) {
        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException("Root command cannot be blank.");
        }
        final List<String> tokens = subcommands == null ? List.of() : subcommands;
        final String joined = tokens.stream()
                .distinct()
                .sorted()
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
        if (joined.isEmpty()) {
            return "&r/" + root;
        }
        return "&r/" + root + " <" + joined + ">";
    }

    private static String normalize(final String token) {
        return token.toLowerCase(Locale.ROOT);
    }
}
