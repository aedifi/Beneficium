package aedifi.bene.service;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingService {
    private final Logger logger;

    public LoggingService(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void info(final String component, final String message) {
        logger.info(prefix(component) + message);
    }

    public void warn(final String component, final String message) {
        logger.warning(prefix(component) + message);
    }

    public void error(final String component, final String message, final Throwable throwable) {
        logger.log(Level.SEVERE, prefix(component) + message, throwable);
    }

    private String prefix(final String component) {
        return "[" + component + "] ";
    }
}
