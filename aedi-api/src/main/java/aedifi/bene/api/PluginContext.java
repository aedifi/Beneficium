package aedifi.bene.api;

import aedifi.bene.api.service.Commands;
import aedifi.bene.api.service.Diagnostics;
import aedifi.bene.api.service.Events;
import aedifi.bene.api.service.Logging;
import aedifi.bene.api.service.Permissions;
import aedifi.bene.api.service.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;

public interface PluginContext {
    JavaPlugin plugin();

    Logging logging();

    Scheduler scheduler();

    Events events();

    Permissions permissions();

    Diagnostics diagnostics();

    Commands commands();
}
