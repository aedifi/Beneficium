package aedifi.bene.api.service;

public interface Logging {
    void info(String component, String message);

    void warn(String component, String message);

    void error(String component, String message, Throwable throwable);
}
