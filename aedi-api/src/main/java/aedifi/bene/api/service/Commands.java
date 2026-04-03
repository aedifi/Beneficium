package aedifi.bene.api.service;

import aedifi.bene.api.command.CommandModel.CommandDefinition;
import aedifi.bene.api.module.ModuleId;
import java.util.List;
import java.util.Map;

public interface Commands {
    void registerDefinition(ModuleId owner, CommandDefinition definition);

    void unregisterOwnerCommands(ModuleId owner);

    Map<ModuleId, List<CommandDefinition>> definitionsByModule();
}
