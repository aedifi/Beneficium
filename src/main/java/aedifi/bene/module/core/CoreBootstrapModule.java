package aedifi.bene.module.core;

import aedifi.bene.api.PluginContext;
import aedifi.bene.api.module.AbstractModule;
import aedifi.bene.api.module.ModuleId;

public final class CoreBootstrapModule extends AbstractModule {
    private static final ModuleId MODULE_ID = ModuleId.of("core.bootstrap");

    @Override
    public ModuleId id() {
        return MODULE_ID;
    }

    @Override
    public boolean onEnable(final PluginContext context) {
        return true;
    }

    @Override
    public void onDisable(final PluginContext context) {
    }
}
