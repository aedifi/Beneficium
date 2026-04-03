package aedifi.bene.command;

import aedifi.bene.api.command.CommandModel.CommandDefinition;
import aedifi.bene.api.command.CommandModel.CommandExecutionContext;
import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface CommandActionHandler {
    int execute(CommandDefinition definition, CommandContext<CommandExecutionContext> context);
}
