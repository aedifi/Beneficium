package aedifi.bene;

import aedifi.bene.command.aedi.AediCommand;
import aedifi.bene.core.PluginKernel;
import java.util.Objects;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BenePlugin extends JavaPlugin {
    private PluginKernel kernel;

    @Override
    public void onEnable() {
        kernel = new PluginKernel(this);
        kernel.start();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (kernel != null) {
            kernel.shutdown();
            kernel = null;
        }
    }

    private void registerCommands() {
        register("aedi", new AediCommand(this, () -> kernel, restarted -> kernel = restarted));
    }

    private void register(final String commandName, final CommandExecutor executor) {
        Objects.requireNonNull(getCommand(commandName), "Missing command registration: " + commandName)
                .setExecutor(executor);
    }

}
