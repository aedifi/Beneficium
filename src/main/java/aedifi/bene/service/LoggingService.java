package aedifi.bene.service;

import aedifi.bene.api.service.Logging;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingService implements Logging {
    private final Logger logger;

    public LoggingService(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public static LoggingService forTests() {
        return new LoggingService(Logger.getAnonymousLogger());
    }

    @Override
    public void info(final String component, final String message) {
        logger.info(prefix(component) + message);
    }

    @Override
    public void warn(final String component, final String message) {
        logger.warning(prefix(component) + message);
    }

    @Override
    public void error(final String component, final String message, final Throwable throwable) {
        logger.log(Level.SEVERE, prefix(component) + message, throwable);
    }

    private String prefix(final String component) {
        return "[" + component + "] ";
    }
}
