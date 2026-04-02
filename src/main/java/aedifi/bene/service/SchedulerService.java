package aedifi.bene.service;

import aedifi.bene.module.ModuleId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerService {
    private final JavaPlugin plugin;
    private final boolean testMode;
    private final Map<ModuleId, List<BukkitTask>> moduleTasks = new HashMap<>();

    public SchedulerService(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.testMode = false;
    }

    private SchedulerService() {
        this.plugin = null;
        this.testMode = true;
    }

    public static SchedulerService forTests() {
        return new SchedulerService();
    }

    public BukkitTask runTask(final ModuleId owner, final Runnable task) {
        if (testMode) {
            throw new IllegalStateException("Task scheduling is unavailable in test mode.");
        }
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
        trackTask(owner, bukkitTask);
        return bukkitTask;
    }

    public BukkitTask runTaskTimer(final ModuleId owner, final Runnable task, final long delay, final long period) {
        if (testMode) {
            throw new IllegalStateException("Task scheduling is unavailable in test mode.");
        }
        final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        trackTask(owner, bukkitTask);
        return bukkitTask;
    }

    public void cancelOwnerTasks(final ModuleId owner) {
        final List<BukkitTask> tasks = moduleTasks.remove(owner);
        if (tasks == null) {
            return;
        }
        for (final BukkitTask task : tasks) {
            task.cancel();
        }
    }

    public void cancelAllTrackedTasks() {
        for (final List<BukkitTask> tasks : moduleTasks.values()) {
            for (final BukkitTask task : tasks) {
                task.cancel();
            }
        }
        moduleTasks.clear();
    }

    private void trackTask(final ModuleId owner, final BukkitTask task) {
        moduleTasks.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(task);
    }
}
