package aedifi.bene.api.service;

import aedifi.bene.api.module.ModuleId;
import org.bukkit.event.Listener;

public interface Events {
    void registerListener(ModuleId owner, Listener listener);

    void unregisterOwnerListeners(ModuleId owner);
}
