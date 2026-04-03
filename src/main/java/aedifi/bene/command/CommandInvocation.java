package aedifi.bene.command;

import aedifi.bene.api.command.CommandModel.CommandDefinition;
import java.util.Map;
import org.bukkit.command.CommandSender;

public record CommandInvocation(
        CommandDefinition definition,
        CommandSender sender,
        Map<String, Object> arguments) {}
