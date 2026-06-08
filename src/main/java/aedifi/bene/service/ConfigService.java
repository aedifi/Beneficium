package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigService {
    private static final String MODULES_SECTION = "modules";
    private static final String HTTP_SECTION = "http";
    private static final String MODULE_STATE_FILE = "module-state.yml";
    private static final int MISSING_VERSION = -1;
    private static final String DEFAULT_HTTP_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_HTTP_PORT = 2780;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final YamlConfiguration moduleStateConfig;
    private final File moduleStateFile;

    private FileConfiguration config;

    public ConfigService(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.moduleStateConfig = new YamlConfiguration();
        this.moduleStateFile = new File(plugin.getDataFolder(), MODULE_STATE_FILE);
    }

    public void load() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        ensureDataFolderExists();
        loadModuleState();
    }

    public boolean failFast() {
        return config.getBoolean(MODULES_SECTION + ".fail-fast", false);
    }

    public boolean strictDependencies() {
        return config.getBoolean(MODULES_SECTION + ".strict-dependencies", true);
    }

    public boolean httpEnabled() {
        return config.getBoolean(HTTP_SECTION + ".enabled", false);
    }

    public String httpAddress() {
        return config.getString(HTTP_SECTION + ".address", DEFAULT_HTTP_ADDRESS);
    }

    public int httpPort() {
        return config.getInt(HTTP_SECTION + ".port", DEFAULT_HTTP_PORT);
    }

    public int storedModuleVersion(final ModuleId moduleId) {
        return moduleStateConfig.getInt(modulePath(moduleId), MISSING_VERSION);
    }

    public void setStoredModuleVersion(final ModuleId moduleId, final int version) {
        moduleStateConfig.set(modulePath(moduleId), version);
    }

    public void flushModuleState() {
        try {
            moduleStateConfig.save(moduleStateFile);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to write " + MODULE_STATE_FILE, ex);
        }
    }

    private String modulePath(final ModuleId moduleId) {
        return MODULES_SECTION + "." + moduleId.value() + ".version";
    }

    private void ensureDataFolderExists() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data folder.");
        }
    }

    private void loadModuleState() {
        if (!moduleStateFile.exists()) {
            return;
        }
        try {
            moduleStateConfig.load(moduleStateFile);
        } catch (final IOException | InvalidConfigurationException ex) {
            logger.warning("Failed to read module-state.yml, continuing with empty module state.");
        }
    }
}
