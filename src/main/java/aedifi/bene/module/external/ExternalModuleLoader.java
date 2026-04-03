package aedifi.bene.module.external;

import aedifi.bene.api.BeneApi;
import aedifi.bene.api.module.Module;
import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.descriptor.ModuleDescriptor;
import aedifi.bene.api.service.Logging;
import aedifi.bene.module.ModuleRegistry;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExternalModuleLoader {
    private final JavaPlugin plugin;
    private final Logging logging;
    private final ModuleDescriptorReader descriptorReader;
    private final Map<ModuleId, LoadedModule> loadedModules = new LinkedHashMap<>();

    public ExternalModuleLoader(final JavaPlugin plugin, final Logging logging) {
        this.plugin = plugin;
        this.logging = logging;
        this.descriptorReader = new ModuleDescriptorReader();
    }

    public List<LoadedModule> loadAll(final ModuleRegistry registry) {
        final Path moduleDir = ensureModuleDirectory();
        final List<Path> jars;
        try {
            jars = Files.list(moduleDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());
        } catch (final IOException ex) {
            logging.error("module-loader", "Failed to scan module directory.", ex);
            return List.of();
        }

        for (final Path jar : jars) {
            try {
                final LoadedModule loaded = loadSingle(jar);
                if (loaded == null) {
                    continue;
                }
                registry.register(loaded.module());
                loadedModules.put(loaded.descriptor().id(), loaded);
                logging.info("module-loader", "Loaded module jar " + jar.getFileName());
            } catch (final Exception ex) {
                logging.error("module-loader", "Failed to load module jar " + jar.getFileName(), ex);
            }
        }

        return List.copyOf(loadedModules.values());
    }

    public Map<ModuleId, LoadedModule> loadedModules() {
        return Map.copyOf(loadedModules);
    }

    public void closeAll() {
        for (final LoadedModule module : loadedModules.values()) {
            try {
                module.close();
            } catch (final IOException ex) {
                logging.warn("module-loader", "Failed to close module " + module.descriptor().id());
            }
        }
        loadedModules.clear();
    }

    private LoadedModule loadSingle(final Path jarPath) throws Exception {
        final ModuleDescriptor descriptor = descriptorReader.read(jarPath);
        if (descriptor.apiVersion() != BeneApi.VERSION) {
            logging.warn(
                    "module-loader",
                    "Skipping " + descriptor.id() + " due to API version mismatch (" + descriptor.apiVersion() + ").");
            return null;
        }
        if (loadedModules.containsKey(descriptor.id())) {
            throw new IllegalStateException("Duplicate module ID: " + descriptor.id());
        }

        final URL jarUrl = jarPath.toUri().toURL();
        final URLClassLoader classLoader = new URLClassLoader(new URL[] {jarUrl}, plugin.getClass().getClassLoader());
        try {
            final Class<?> rawClass = Class.forName(descriptor.mainClass(), true, classLoader);
            if (!Module.class.isAssignableFrom(rawClass)) {
                throw new IllegalArgumentException("Main class does not implement Module: " + descriptor.mainClass());
            }
            final Module module = (Module) rawClass.getDeclaredConstructor().newInstance();
            if (!descriptor.id().equals(module.id())) {
                throw new IllegalStateException("Module ID mismatch for " + descriptor.mainClass());
            }
            return new LoadedModule(descriptor, module, classLoader, jarPath);
        } catch (final Exception ex) {
            try {
                classLoader.close();
            } catch (final IOException closeEx) {
                logging.warn("module-loader", "Failed to close classloader for " + jarPath.getFileName());
            }
            throw ex;
        }
    }

    private Path ensureModuleDirectory() {
        final Path moduleDir = plugin.getDataFolder().toPath().resolve("modules");
        try {
            Files.createDirectories(moduleDir);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to create module directory.", ex);
        }
        return moduleDir;
    }
}
