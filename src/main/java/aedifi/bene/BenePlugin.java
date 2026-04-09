package aedifi.bene;

import aedifi.bene.core.PluginKernel;
import org.bukkit.plugin.java.JavaPlugin;

public final class BenePlugin extends JavaPlugin {
    private PluginKernel kernel;

    @Override
    public void onEnable() {
        kernel = new PluginKernel(this);
        kernel.start();
    }

    @Override
    public void onDisable() {
        if (kernel != null) {
            kernel.shutdown();
            kernel = null;
        }
    }

    public PluginKernel kernel() {
        return kernel;
    }

    public void replaceKernel(final PluginKernel replacement) {
        this.kernel = replacement;
    }
}
