package aedifi.bene.command;

import static aedifi.bene.api.command.CommandModel.*;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;

public final class CommandTreeCompiler {
    public List<LiteralArgumentBuilder<CommandExecutionContext>> compile(
            final CommandDefinition definition,
            final CommandActionHandler actionHandler) {
        final List<LiteralArgumentBuilder<CommandExecutionContext>> roots = new ArrayList<>();
        roots.add(buildRoot(definition.root(), definition, actionHandler));
        for (final String alias : definition.aliases()) {
            if (!alias.contains("*")) {
                roots.add(buildRoot(alias, definition, actionHandler));
            }
        }
        return List.copyOf(roots);
    }

    private LiteralArgumentBuilder<CommandExecutionContext> buildRoot(
            final String literal,
            final CommandDefinition definition,
            final CommandActionHandler actionHandler) {
        final LiteralArgumentBuilder<CommandExecutionContext> root = LiteralArgumentBuilder.literal(literal);
        root.executes(ctx -> actionHandler.execute(definition, ctx));

        ArgumentBuilder<CommandExecutionContext, ?> cursor = root;
        for (final CommandArgumentSpec argument : definition.arguments()) {
            final RequiredArgumentBuilder<CommandExecutionContext, ?> next = RequiredArgumentBuilder.argument(
                    argument.name(),
                    argumentType(argument.type()));
            if (!argument.suggestions().isEmpty()) {
                next.suggests(staticSuggestions(argument.suggestions()));
            }
            next.executes(ctx -> actionHandler.execute(definition, ctx));
            cursor.then(next);
            cursor = next;
        }
        return root;
    }

    private static SuggestionProvider<CommandExecutionContext> staticSuggestions(final List<String> suggestions) {
        return (ctx, builder) -> {
            for (final String suggestion : suggestions) {
                builder.suggest(suggestion);
            }
            return builder.buildFuture();
        };
    }

    private static ArgumentType<?> argumentType(final CommandArgumentType type) {
        return switch (type) {
            case WORD -> StringArgumentType.word();
            case GREEDY_STRING -> StringArgumentType.greedyString();
            case INTEGER -> IntegerArgumentType.integer();
            case BOOLEAN -> BoolArgumentType.bool();
        };
    }
}
