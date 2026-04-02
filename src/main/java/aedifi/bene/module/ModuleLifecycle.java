package aedifi.bene.module;

import aedifi.bene.core.PluginContext;
import aedifi.bene.service.ConfigService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleLifecycle {
    private final ModuleRegistry registry;
    private final PluginContext context;
    private final ConfigService configService;
    private final boolean failFast;
    private final boolean strictDependencies;

    private final Map<ModuleId, State> states = new HashMap<>();
    private final List<Module> enabledModules = new ArrayList<>();

    public ModuleLifecycle(
            final ModuleRegistry registry,
            final PluginContext context,
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
                warning -> context.loggingService().warn("module-lifecycle", warning));

        for (final Module module : enableOrder) {
            final ModuleId moduleId = module.id();
            final long start = System.nanoTime();
            states.put(moduleId, State.ENABLING);
            try {
                executeMigrationsIfNeeded(module);
                final boolean enabled = module.onEnable(context);
                if (!enabled) {
                    states.put(moduleId, State.FAILED);
                    context.loggingService().warn(moduleId.value(), "Module returned false during enable.");
                    if (failFast) {
                        throw new IllegalStateException("Module " + moduleId + " returned false during enable.");
                    }
                    continue;
                }

                states.put(moduleId, State.ENABLED);
                enabledModules.add(module);
                configService.setStoredModuleVersion(moduleId, module.schemaVersion());
                final long elapsedMicros = (System.nanoTime() - start) / 1_000L;
                context.loggingService().info(moduleId.value(), "Enabled in " + elapsedMicros + "us.");
            } catch (final Exception ex) {
                states.put(moduleId, State.FAILED);
                context.loggingService().error(moduleId.value(), "Module failed to enable.", ex);
                if (failFast) {
                    throw new IllegalStateException("Module startup aborted due to failure in " + moduleId, ex);
                }
            }
        }

        for (final Module module : enabledModules) {
            final ModuleId moduleId = module.id();
            try {
                module.postEnable(context);
                states.put(moduleId, State.POST_ENABLED);
            } catch (final Exception ex) {
                states.put(moduleId, State.FAILED);
                context.loggingService().error(moduleId.value(), "Module post-enable failed.", ex);
                if (failFast) {
                    throw new IllegalStateException("Post-enable aborted due to failure in " + moduleId, ex);
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
            states.put(moduleId, State.DISABLING);
            try {
                context.schedulerService().cancelOwnerTasks(moduleId);
                module.onDisable(context);
                states.put(moduleId, State.DISABLED);
            } catch (final Exception ex) {
                states.put(moduleId, State.FAILED);
                context.loggingService().error(moduleId.value(), "Module failed to disable cleanly.", ex);
            }
        }
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

    private enum State {
        ENABLING,
        ENABLED,
        POST_ENABLED,
        DISABLING,
        DISABLED,
        FAILED
    }
}
