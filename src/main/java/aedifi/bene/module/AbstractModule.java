package aedifi.bene.module;

import aedifi.bene.core.PluginContext;

public abstract class AbstractModule implements Module {
    protected final void info(final PluginContext context, final String message) {
        context.loggingService().info(id().value(), message);
    }

    protected final void warn(final PluginContext context, final String message) {
        context.loggingService().warn(id().value(), message);
    }

    protected final void error(final PluginContext context, final String message, final Throwable throwable) {
        context.loggingService().error(id().value(), message, throwable);
    }
}
