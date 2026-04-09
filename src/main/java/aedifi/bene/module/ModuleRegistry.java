package aedifi.bene.module;

import aedifi.bene.api.module.Module;
import aedifi.bene.api.module.ModuleId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ModuleRegistry {
    private final Map<ModuleId, Module> modules = new LinkedHashMap<>();

    public void register(final Module module) {
        final ModuleId id = module.id();
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate module ID: " + id);
        }
        modules.put(id, module);
    }

    public Collection<Module> modules() {
        return List.copyOf(modules.values());
    }

    public List<Module> resolveEnableOrder(
            final boolean strictDependencies,
            final Consumer<String> warningSink) {
        final Set<ModuleId> skipped = strictDependencies ? Set.of() : collectMissingDependencyModules(warningSink);
        final Map<ModuleId, Integer> indegree = new HashMap<>(modules.size());
        final Map<ModuleId, Set<ModuleId>> outgoing = new HashMap<>(modules.size());

        for (final Module module : modules.values()) {
            final ModuleId moduleId = module.id();
            if (skipped.contains(moduleId)) {
                continue;
            }

            indegree.put(moduleId, 0);
            outgoing.computeIfAbsent(moduleId, ignored -> new LinkedHashSet<>());
        }

        for (final Module module : modules.values()) {
            final ModuleId targetId = module.id();
            if (skipped.contains(targetId)) {
                continue;
            }

            for (final ModuleId dependencyId : module.dependencies()) {
                final Module dependency = modules.get(dependencyId);
                if (dependency == null) {
                    if (strictDependencies) {
                        throw new IllegalStateException(
                                "Missing dependency: module " + targetId + " depends on " + dependencyId);
                    }
                    continue;
                }
                if (skipped.contains(dependencyId)) {
                    continue;
                }
                outgoing.computeIfAbsent(dependencyId, ignored -> new LinkedHashSet<>()).add(targetId);
                indegree.put(targetId, indegree.get(targetId) + 1);
            }
        }

        final ArrayDeque<ModuleId> queue = new ArrayDeque<>();
        for (final Module module : modules.values()) {
            final ModuleId id = module.id();
            if (indegree.getOrDefault(id, -1) == 0) {
                queue.add(id);
            }
        }

        final List<Module> ordered = new ArrayList<>(indegree.size());
        while (!queue.isEmpty()) {
            final ModuleId current = queue.removeFirst();
            ordered.add(modules.get(current));
            for (final ModuleId dependent : outgoing.getOrDefault(current, Set.of())) {
                final int next = indegree.get(dependent) - 1;
                indegree.put(dependent, next);
                if (next == 0) {
                    queue.addLast(dependent);
                }
            }
        }

        if (ordered.size() != indegree.size()) {
            throw new IllegalStateException("Detected circular dependency in module graph.");
        }

        return ordered;
    }

    private Set<ModuleId> collectMissingDependencyModules(final Consumer<String> warningSink) {
        final Set<ModuleId> skipped = new LinkedHashSet<>();
        for (final Module module : modules.values()) {
            final ModuleId moduleId = module.id();
            for (final ModuleId dependencyId : module.dependencies()) {
                if (!modules.containsKey(dependencyId)) {
                    warningSink.accept(
                            "Skipping module " + moduleId + " because dependency " + dependencyId + " is missing.");
                    skipped.add(moduleId);
                    break;
                }
            }
        }
        return skipped;
    }
}
