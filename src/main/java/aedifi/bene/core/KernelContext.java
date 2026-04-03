package aedifi.bene.core;

import aedifi.bene.api.service.Commands;
import aedifi.bene.api.service.Diagnostics;
import aedifi.bene.api.service.Events;
import aedifi.bene.api.service.Logging;
import aedifi.bene.api.service.Permissions;
import aedifi.bene.api.service.Scheduler;
import aedifi.bene.service.CommandService;
import aedifi.bene.service.ConfigService;
import aedifi.bene.service.DiagnosticsService;
import aedifi.bene.service.EventService;
import aedifi.bene.service.LoggingService;
import aedifi.bene.service.PermissionService;
import aedifi.bene.service.SchedulerService;
import org.bukkit.plugin.java.JavaPlugin;

public final class KernelContext implements aedifi.bene.api.PluginContext {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final SchedulerService schedulerService;
    private final LoggingService loggingService;
    private final EventService eventService;
    private final PermissionService permissionService;
    private final DiagnosticsService diagnosticsService;
    private final CommandService commandService;

    public KernelContext(
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

    @Override
    public JavaPlugin plugin() {
        return plugin;
    }

    public ConfigService configService() {
        return configService;
    }

    @Override
    public Scheduler scheduler() {
        return schedulerService;
    }

    @Override
    public Logging logging() {
        return loggingService;
    }

    @Override
    public Events events() {
        return eventService;
    }

    @Override
    public Permissions permissions() {
        return permissionService;
    }

    @Override
    public Diagnostics diagnostics() {
        return diagnosticsService;
    }

    @Override
    public Commands commands() {
        return commandService;
    }
}
