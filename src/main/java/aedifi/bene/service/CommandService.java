package aedifi.bene.service;

import aedifi.bene.command.CommandModel.CommandDefinition;
import aedifi.bene.command.CommandModel.CommandExecutionContext;
import aedifi.bene.command.CommandTreeCompiler;
import aedifi.bene.module.ModuleId;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommandService {
    private final CommandTreeCompiler compiler;
    private final Map<ModuleId, List<CommandDefinition>> moduleDefinitions = new HashMap<>();
    private final Map<ModuleId, List<LiteralArgumentBuilder<CommandExecutionContext>>> compiledTrees = new HashMap<>();

    public CommandService() {
        this.compiler = new CommandTreeCompiler();
    }

    public void registerDefinition(final ModuleId owner, final CommandDefinition definition) {
        final List<LiteralArgumentBuilder<CommandExecutionContext>> compiled = compiler.compile(definition);
        moduleDefinitions.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(definition);
        compiledTrees.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).addAll(compiled);
    }

    public void unregisterOwnerCommands(final ModuleId owner) {
        moduleDefinitions.remove(owner);
        compiledTrees.remove(owner);
    }

    public void clearAllDefinitions() {
        moduleDefinitions.clear();
        compiledTrees.clear();
    }

    public Map<ModuleId, List<CommandDefinition>> definitionsByModule() {
        return Map.copyOf(moduleDefinitions);
    }
}
