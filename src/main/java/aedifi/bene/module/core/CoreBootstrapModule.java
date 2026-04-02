package aedifi.bene.module.core;

import aedifi.bene.core.PluginContext;
import aedifi.bene.module.AbstractModule;
import aedifi.bene.module.ModuleId;

public final class CoreBootstrapModule extends AbstractModule {
    private static final ModuleId MODULE_ID = ModuleId.of("core.bootstrap");

    @Override
    public ModuleId id() {
        return MODULE_ID;
    }

    @Override
    public boolean onEnable(final PluginContext context) {
        info(context, "Core bootstrap module is active.");
        return true;
    }

    @Override
    public void onDisable(final PluginContext context) {
        info(context, "Core bootstrap module is shutting down.");
    }
}
