package aedifi.bene.core;

import aedifi.bene.service.ConfigService;
import aedifi.bene.service.LoggingService;
import aedifi.bene.service.SchedulerService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginContext {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final SchedulerService schedulerService;
    private final LoggingService loggingService;

    public PluginContext(
            final JavaPlugin plugin,
            final ConfigService configService,
            final SchedulerService schedulerService,
            final LoggingService loggingService) {
        this.plugin = plugin;
        this.configService = configService;
        this.schedulerService = schedulerService;
        this.loggingService = loggingService;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public ConfigService configService() {
        return configService;
    }

    public SchedulerService schedulerService() {
        return schedulerService;
    }

    public LoggingService loggingService() {
        return loggingService;
    }
}
