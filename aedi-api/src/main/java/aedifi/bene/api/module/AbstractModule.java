package aedifi.bene.api.module;

import aedifi.bene.api.PluginContext;

public abstract class AbstractModule implements Module {
    protected final void info(final PluginContext context, final String message) {
        context.logging().info(id().value(), message);
    }

    protected final void warn(final PluginContext context, final String message) {
        context.logging().warn(id().value(), message);
    }

    protected final void error(final PluginContext context, final String message, final Throwable throwable) {
        context.logging().error(id().value(), message, throwable);
    }
}
