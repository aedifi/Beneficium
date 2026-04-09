package aedifi.bene.module.external;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.descriptor.ModuleDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModuleDescriptorReader {
    public ModuleDescriptor read(final Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            final JarEntry entry = jarFile.getJarEntry(ModuleDescriptor.FILE_NAME);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Missing " + ModuleDescriptor.FILE_NAME + " in " + jarPath.getFileName());
            }
            try (InputStream input = jarFile.getInputStream(entry)) {
                final String yamlSource = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                final YamlConfiguration yaml = new YamlConfiguration();
                try {
                    yaml.loadFromString(yamlSource);
                } catch (final InvalidConfigurationException ex) {
                    throw new IllegalArgumentException(
                            "Invalid " + ModuleDescriptor.FILE_NAME + " in " + jarPath.getFileName(),
                            ex);
                }
                final String idRaw = requireNonBlank(
                        yaml.getString(ModuleDescriptor.Keys.ID),
                        ModuleDescriptor.Keys.ID);
                final String main = requireNonBlank(
                        yaml.getString(ModuleDescriptor.Keys.MAIN),
                        ModuleDescriptor.Keys.MAIN);
                final int apiVersion = yaml.getInt(ModuleDescriptor.Keys.API_VERSION, -1);
                if (apiVersion < 0) {
                    throw new IllegalArgumentException(
                            "Missing " + ModuleDescriptor.Keys.API_VERSION + " in " + jarPath.getFileName());
                }
                return new ModuleDescriptor(ModuleId.of(idRaw), main, apiVersion);
            }
        }
    }

    private static String requireNonBlank(final String value, final String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing " + key + " in " + ModuleDescriptor.FILE_NAME);
        }
        return value;
    }
}
