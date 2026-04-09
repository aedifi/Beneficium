package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigService {
    private static final String MODULES_SECTION = "modules";
    private static final String MODULE_STATE_FILE = "module-state.yml";
    private static final int MISSING_VERSION = -1;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final YamlConfiguration moduleStateConfig;
    private final File moduleStateFile;

    private FileConfiguration config;
    private final boolean testMode;
    private final Map<String, Integer> testState;
    private final boolean testFailFast;
    private final boolean testStrictDependencies;

    public ConfigService(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.moduleStateConfig = new YamlConfiguration();
        this.moduleStateFile = new File(plugin.getDataFolder(), MODULE_STATE_FILE);
        this.testMode = false;
        this.testState = Map.of();
        this.testFailFast = false;
        this.testStrictDependencies = true;
    }

    private ConfigService(
            final Map<String, Integer> moduleState,
            final boolean failFast,
            final boolean strictDependencies) {
        this.plugin = null;
        this.logger = Logger.getAnonymousLogger();
        this.moduleStateConfig = null;
        this.moduleStateFile = null;
        this.config = null;
        this.testMode = true;
        this.testState = new HashMap<>(moduleState);
        this.testFailFast = failFast;
        this.testStrictDependencies = strictDependencies;
    }

    public static ConfigService forTests(final boolean failFast, final boolean strictDependencies) {
        return new ConfigService(Map.of(), failFast, strictDependencies);
    }

    public void load() {
        if (testMode) {
            return;
        }

        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        ensureDataFolderExists();
        loadModuleState();
    }

    public boolean failFast() {
        if (testMode) {
            return testFailFast;
        }
        return config.getBoolean(MODULES_SECTION + ".fail-fast", false);
    }

    public boolean strictDependencies() {
        if (testMode) {
            return testStrictDependencies;
        }
        return config.getBoolean(MODULES_SECTION + ".strict-dependencies", true);
    }

    public int storedModuleVersion(final ModuleId moduleId) {
        if (testMode) {
            return testState.getOrDefault(moduleId.value(), MISSING_VERSION);
        }
        return moduleStateConfig.getInt(modulePath(moduleId), MISSING_VERSION);
    }

    public void setStoredModuleVersion(final ModuleId moduleId, final int version) {
        if (testMode) {
            testState.put(moduleId.value(), version);
            return;
        }
        moduleStateConfig.set(modulePath(moduleId), version);
    }

    public void flushModuleState() {
        if (testMode) {
            return;
        }
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
