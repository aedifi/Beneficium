package aedifi.bene.api.service;

import aedifi.bene.api.module.ModuleId;
import org.bukkit.scheduler.BukkitTask;

public interface Scheduler {
    BukkitTask runTask(ModuleId owner, Runnable task);

    BukkitTask runTaskTimer(ModuleId owner, Runnable task, long delay, long period);

    void cancelOwnerTasks(ModuleId owner);
}
