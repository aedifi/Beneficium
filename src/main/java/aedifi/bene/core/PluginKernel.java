package aedifi.bene.core;

import aedifi.bene.module.ModuleLifecycle;
import aedifi.bene.module.ModuleRegistry;
import aedifi.bene.module.core.CoreBootstrapModule;
import aedifi.bene.module.core.CoreCommandsModule;
import aedifi.bene.module.external.ExternalModuleLoader;
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
    private final ExternalModuleLoader externalModuleLoader;
    private ModuleLifecycle moduleLifecycle;

    public PluginKernel(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.loggingService = new LoggingService(plugin.getLogger());
        this.configService = new ConfigService(plugin);
        this.schedulerService = new SchedulerService(plugin);
        this.eventService = new EventService(plugin);
        this.permissionService = new PermissionService();
        this.diagnosticsService = new DiagnosticsService();
        this.commandService = new CommandService(plugin, loggingService);
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
        this.externalModuleLoader = new ExternalModuleLoader(plugin, loggingService);
    }

    public void start() {
        final long start = System.nanoTime();
        loggingService.info("kernel", "Kernel is starting up.");
        configService.load();
        registerCoreModules();
        externalModuleLoader.loadAll(moduleRegistry);

        moduleLifecycle = new ModuleLifecycle(
                moduleRegistry,
                context,
                configService.failFast(),
                configService.strictDependencies());
        diagnosticsService.bindLifecycleSnapshots(moduleLifecycle::statusSnapshots);

        moduleLifecycle.enableAll();
        final double elapsedMillis = (System.nanoTime() - start) / 1_000_000.0D;
        loggingService.info("kernel", "Kernel has finished in " + String.format(java.util.Locale.ROOT, "%.3f", elapsedMillis) + "ms.");
    }

    public void shutdown() {
        loggingService.info("kernel", "Kernel is shutting down.");
        if (moduleLifecycle != null) {
            moduleLifecycle.disableAll();
            moduleLifecycle = null;
        }
        externalModuleLoader.closeAll();
        commandService.clearAllDefinitions();
        commandService.shutdown();
        eventService.unregisterAllListeners();
        schedulerService.cancelAllTrackedTasks();
    }

    private void registerCoreModules() {
        moduleRegistry.register(new CoreBootstrapModule());
        moduleRegistry.register(new CoreCommandsModule());
    }

    public DiagnosticsService diagnostics() {
        return diagnosticsService;
    }

    public CommandService commandService() {
        return commandService;
    }
}
