package aedifi.bene.core;

import aedifi.bene.BenePlugin;
import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Commands;
import aedifi.bene.command.aedi.AediCommand;
import aedifi.bene.module.ModuleLifecycle;
import aedifi.bene.module.ModuleRegistry;
import aedifi.bene.module.external.ExternalModuleLoader;
import aedifi.bene.service.CommandService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import aedifi.bene.service.ConfigService;
import aedifi.bene.service.DiagnosticsService;
import aedifi.bene.service.EventService;
import aedifi.bene.service.LoggingService;
import aedifi.bene.service.PermissionService;
import aedifi.bene.service.SchedulerService;

public final class PluginKernel {
    private static final ModuleId KERNEL_COMMANDS = ModuleId.of("kernel");

    private final BenePlugin plugin;
    private final LoggingService loggingService;
    private final ConfigService configService;
    private final SchedulerService schedulerService;
    private final EventService eventService;
    private final PermissionService permissionService;
    private final DiagnosticsService diagnosticsService;
    private final CommandService commandService;
    private final ModuleRegistry moduleRegistry;
    private final KernelContext context;
    private final ExternalModuleLoader externalModuleLoader;
    private ModuleLifecycle moduleLifecycle;
    private Map<ModuleId, String> moduleDisplayNames = Map.of();

    public PluginKernel(final BenePlugin plugin) {
        this.plugin = plugin;
        this.loggingService = new LoggingService(plugin.getLogger());
        this.configService = new ConfigService(plugin);
        this.schedulerService = new SchedulerService(plugin);
        this.eventService = new EventService(plugin);
        this.permissionService = new PermissionService();
        this.diagnosticsService = new DiagnosticsService();
        this.commandService = new CommandService(plugin, loggingService);
        this.moduleRegistry = new ModuleRegistry();
        this.context = new KernelContext(
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
        registerAdministrativeCommands();
        externalModuleLoader.loadAll(moduleRegistry);
        final LinkedHashMap<ModuleId, String> names = new LinkedHashMap<>();
        externalModuleLoader.loadedModules().forEach((id, loaded) -> names.put(id, loaded.descriptor().displayName()));
        moduleDisplayNames = Collections.unmodifiableMap(names);

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
        moduleDisplayNames = Map.of();
        externalModuleLoader.closeAll();
        commandService.shutdown();
        eventService.unregisterAllListeners();
        schedulerService.cancelAllTrackedTasks();
    }

    private void registerAdministrativeCommands() {
        final AediCommand aediCommand = new AediCommand(plugin);
        commandService.register(
                KERNEL_COMMANDS,
                new Commands.Registration("aedi"),
                aediCommand,
                aediCommand);
    }

    public DiagnosticsService diagnostics() {
        return diagnosticsService;
    }

    public Map<ModuleId, String> moduleDisplayNames() {
        return moduleDisplayNames;
    }
}
