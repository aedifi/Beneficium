package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Events;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventService implements Events {
    private final JavaPlugin plugin;
    private final Map<ModuleId, List<Listener>> moduleListeners = new HashMap<>();

    public EventService(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerListener(final ModuleId owner, final Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        moduleListeners.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(listener);
    }

    @Override
    public void unregisterOwnerListeners(final ModuleId owner) {
        final List<Listener> listeners = moduleListeners.remove(owner);
        if (listeners == null) {
            return;
        }
        for (final Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
    }

    public void unregisterAllListeners() {
        for (final List<Listener> listeners : moduleListeners.values()) {
            for (final Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }
        moduleListeners.clear();
    }
}
