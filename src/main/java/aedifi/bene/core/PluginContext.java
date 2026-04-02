package aedifi.bene.core;

import aedifi.bene.service.ConfigService;
import aedifi.bene.service.DiagnosticsService;
import aedifi.bene.service.EventService;
import aedifi.bene.service.LoggingService;
import aedifi.bene.service.PermissionService;
import aedifi.bene.service.SchedulerService;
import aedifi.bene.service.CommandService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginContext {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final SchedulerService schedulerService;
    private final LoggingService loggingService;
    private final EventService eventService;
    private final PermissionService permissionService;
    private final DiagnosticsService diagnosticsService;
    private final CommandService commandService;

    public PluginContext(
            final JavaPlugin plugin,
            final ConfigService configService,
            final SchedulerService schedulerService,
            final LoggingService loggingService,
            final EventService eventService,
            final PermissionService permissionService,
            final DiagnosticsService diagnosticsService,
            final CommandService commandService) {
        this.plugin = plugin;
        this.configService = configService;
        this.schedulerService = schedulerService;
        this.loggingService = loggingService;
        this.eventService = eventService;
        this.permissionService = permissionService;
        this.diagnosticsService = diagnosticsService;
        this.commandService = commandService;
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

    public EventService eventService() {
        return eventService;
    }

    public PermissionService permissionService() {
        return permissionService;
    }

    public DiagnosticsService diagnosticsService() {
        return diagnosticsService;
    }

    public CommandService commandService() {
        return commandService;
    }
}
