package aedifi.bene.module.core;

import static aedifi.bene.api.command.CommandModel.*;

import aedifi.bene.api.PluginContext;
import aedifi.bene.api.module.AbstractModule;
import aedifi.bene.api.module.ModuleId;
import java.util.List;
import java.util.Set;

public final class CoreCommandsModule extends AbstractModule {
    private static final ModuleId MODULE_ID = ModuleId.of("core.commands");
    private static final ModuleId BOOTSTRAP_ID = ModuleId.of("core.bootstrap");

    @Override
    public ModuleId id() {
        return MODULE_ID;
    }

    @Override
    public Set<ModuleId> dependencies() {
        return Set.of(BOOTSTRAP_ID);
    }

    @Override
    public boolean onEnable(final PluginContext context) {
        context.commands().registerDefinition(
                id(),
                new CommandDefinition(
                        "core.ping",
                        "benecore",
                        List.of("bene*"),
                        CommandSenderType.ANY,
                        context.permissions().node(id(), "ping"),
                        List.of(new CommandArgumentSpec("echo", CommandArgumentType.WORD, false, List.of())),
                        new CommandActionSpec("Core command layer is active.")));
        return true;
    }

    @Override
    public void onDisable(final PluginContext context) {
        context.commands().unregisterOwnerCommands(id());
    }
}
