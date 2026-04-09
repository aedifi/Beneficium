package aedifi.bene.module;

import aedifi.bene.api.module.Module;
import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.ModuleStatus;
import aedifi.bene.core.KernelContext;
import aedifi.bene.service.ConfigService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleLifecycle {
    private final ModuleRegistry registry;
    private final KernelContext context;
    private final ConfigService configService;
    private final boolean failFast;
    private final boolean strictDependencies;

    private final Map<ModuleId, ModuleStatus.State> states = new HashMap<>();
    private final Map<ModuleId, Long> enableTimesMicros = new HashMap<>();
    private final Map<ModuleId, Throwable> failures = new HashMap<>();
    private final List<Module> enabledModules = new ArrayList<>();

    public ModuleLifecycle(
            final ModuleRegistry registry,
            final KernelContext context,
            final boolean failFast,
            final boolean strictDependencies) {
        this.registry = registry;
        this.context = context;
        this.configService = context.configService();
        this.failFast = failFast;
        this.strictDependencies = strictDependencies;
    }

    public void enableAll() {
        final List<Module> enableOrder = registry.resolveEnableOrder(
                strictDependencies,
                warning -> context.logging().warn("module-lifecycle", warning));

        for (final Module module : enableOrder) {
            final ModuleId moduleId = module.id();
            final long start = System.nanoTime();
            states.put(moduleId, ModuleStatus.State.ENABLING);
            context.logging().info(moduleId.value(), "Module is starting up.");
            try {
                executeMigrationsIfNeeded(module);
                final boolean enabled = module.onEnable(context);
                if (!enabled) {
                    states.put(moduleId, ModuleStatus.State.FAILED);
                    failures.put(moduleId, new IllegalStateException("Module returned false during enable."));
                    context.logging().warn(moduleId.value(), "Module returned false during enable.");
                    runModuleCleanup(moduleId);
                    if (failFast) {
                        throw new IllegalStateException("Module " + moduleId + " returned false during enable.");
                    }
                    continue;
                }

                states.put(moduleId, ModuleStatus.State.ENABLED);
                enabledModules.add(module);
                configService.setStoredModuleVersion(moduleId, module.schemaVersion());
                final long elapsedMicros = (System.nanoTime() - start) / 1_000L;
                enableTimesMicros.put(moduleId, elapsedMicros);
                final double elapsedMillis = elapsedMicros / 1_000.0D;
                context.logging().info(
                        moduleId.value(),
                        "Module has finished in " + formatMillis(elapsedMillis) + "ms.");
            } catch (final Exception ex) {
                states.put(moduleId, ModuleStatus.State.FAILED);
                failures.put(moduleId, ex);
                runModuleCleanup(moduleId);
                context.logging().error(moduleId.value(), "Module failed to enable.", ex);
                if (failFast) {
                    throw new IllegalStateException("Module startup aborted due to failure in " + moduleId, ex);
                }
            }
        }

        configService.flushModuleState();
    }

    public void disableAll() {
        final List<Module> reverseOrder = new ArrayList<>(enabledModules);
        Collections.reverse(reverseOrder);

        for (final Module module : reverseOrder) {
            final ModuleId moduleId = module.id();
            states.put(moduleId, ModuleStatus.State.DISABLING);
            context.logging().info(moduleId.value(), "Module is shutting down.");
            try {
                module.onDisable(context);
                states.put(moduleId, ModuleStatus.State.DISABLED);
            } catch (final Exception ex) {
                states.put(moduleId, ModuleStatus.State.FAILED);
                failures.put(moduleId, ex);
                context.logging().error(moduleId.value(), "Module failed to disable cleanly.", ex);
            } finally {
                runModuleCleanup(moduleId);
            }
        }
        enabledModules.clear();
    }

    private void executeMigrationsIfNeeded(final Module module) throws Exception {
        final ModuleId moduleId = module.id();
        final int storedVersion = configService.storedModuleVersion(moduleId);
        final int currentVersion = module.schemaVersion();

        if (storedVersion < 0) {
            module.firstLoad(context);
            return;
        }

        if (storedVersion != currentVersion) {
            module.migrate(context, storedVersion, currentVersion);
        }
    }

    public Map<ModuleId, ModuleStatus.Snapshot> statusSnapshots() {
        final Map<ModuleId, ModuleStatus.Snapshot> snapshots = new HashMap<>(states.size());
        for (final Map.Entry<ModuleId, ModuleStatus.State> entry : states.entrySet()) {
            final ModuleId moduleId = entry.getKey();
            snapshots.put(
                    moduleId,
                    new ModuleStatus.Snapshot(
                            moduleId,
                            entry.getValue(),
                            Optional.ofNullable(enableTimesMicros.get(moduleId)),
                            Optional.ofNullable(failures.get(moduleId)).map(Throwable::getMessage)));
        }
        return Map.copyOf(snapshots);
    }

    private void runModuleCleanup(final ModuleId moduleId) {
        context.scheduler().cancelOwnerTasks(moduleId);
        context.events().unregisterOwnerListeners(moduleId);
        context.commands().unregisterOwnerCommands(moduleId);
    }

    private static String formatMillis(final double millis) {
        return String.format(Locale.ROOT, "%.3f", millis);
    }
}
