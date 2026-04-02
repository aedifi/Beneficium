package aedifi.bene.module;

import aedifi.bene.core.PluginContext;
import java.util.Set;

public interface Module {
    ModuleId id();

    default Set<ModuleId> dependencies() {
        return Set.of();
    }

    default int schemaVersion() {
        return 1;
    }

    default void firstLoad(final PluginContext context) throws Exception {
    }

    default void migrate(final PluginContext context, final int previousVersion, final int newVersion)
            throws Exception {
    }

    default boolean onEnable(final PluginContext context) throws Exception {
        return true;
    }

    default void postEnable(final PluginContext context) throws Exception {
    }

    default void onDisable(final PluginContext context) throws Exception {
    }
}
