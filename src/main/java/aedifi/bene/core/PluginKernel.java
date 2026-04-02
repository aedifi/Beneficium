package aedifi.bene.core;

import aedifi.bene.module.ModuleLifecycle;
import aedifi.bene.module.ModuleRegistry;
import aedifi.bene.module.core.CoreBootstrapModule;
import aedifi.bene.module.core.CoreCommandsModule;
import aedifi.bene.service.CommandService;
import aedifi.bene.service.ConfigService;
import aedifi.bene.service.DiagnosticsService;
import aedifi.bene.service.EventService;
import aedifi.bene.service.LoggingService;
import aedifi.bene.service.PermissionService;
import aedifi.bene.service.SchedulerService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginKernel {
    private final JavaPlugin plugin;
    private final LoggingService loggingService;
    private final ConfigService configService;
    private final SchedulerService schedulerService;
    private final EventService eventService;
    private final PermissionService permissionService;
    private final DiagnosticsService diagnosticsService;
    private final CommandService commandService;
    private final ModuleRegistry moduleRegistry;
    private final PluginContext context;
    private ModuleLifecycle moduleLifecycle;

    public PluginKernel(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.loggingService = new LoggingService(plugin.getLogger());
        this.configService = new ConfigService(plugin);
        this.schedulerService = new SchedulerService(plugin);
        this.eventService = new EventService(plugin);
        this.permissionService = new PermissionService();
        this.diagnosticsService = new DiagnosticsService();
        this.commandService = new CommandService();
        this.moduleRegistry = new ModuleRegistry();
        this.context = new PluginContext(
                plugin,
                configService,
                schedulerService,
                loggingService,
                eventService,
                permissionService,
                diagnosticsService,
                commandService);
    }

    public void start() {
        loggingService.info("kernel", "Starting Beneficium kernel.");
        configService.load();
        registerCoreModules();

        moduleLifecycle = new ModuleLifecycle(
                moduleRegistry,
                context,
                configService.failFast(),
                configService.strictDependencies());
        diagnosticsService.bindLifecycleSnapshots(moduleLifecycle::statusSnapshots);

        moduleLifecycle.enableAll();
        loggingService.info("kernel", "Beneficium kernel started.");
    }

    public void shutdown() {
        loggingService.info("kernel", "Stopping Beneficium kernel.");
        if (moduleLifecycle != null) {
            moduleLifecycle.disableAll();
            moduleLifecycle = null;
        }
        commandService.clearAllDefinitions();
        eventService.unregisterAllListeners();
        schedulerService.cancelAllTrackedTasks();
        loggingService.info("kernel", "Beneficium kernel stopped.");
    }

    private void registerCoreModules() {
        moduleRegistry.register(new CoreBootstrapModule());
        moduleRegistry.register(new CoreCommandsModule());
    }
}
