package aedifi.bene.module.external;

import aedifi.bene.api.module.ModuleId;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModuleDescriptorReader {
    public static final String DESCRIPTOR_NAME = "module.yml";

    public ModuleDescriptor read(final Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            final JarEntry entry = jarFile.getJarEntry(DESCRIPTOR_NAME);
            if (entry == null) {
                throw new IllegalArgumentException("Missing " + DESCRIPTOR_NAME + " in " + jarPath.getFileName());
            }
            try (InputStream input = jarFile.getInputStream(entry)) {
                final String yamlSource = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                final YamlConfiguration yaml = new YamlConfiguration();
                try {
                    yaml.loadFromString(yamlSource);
                } catch (final InvalidConfigurationException ex) {
                    throw new IllegalArgumentException("Invalid " + DESCRIPTOR_NAME + " in " + jarPath.getFileName(), ex);
                }
                final String idRaw = requireNonBlank(yaml.getString("id"), "id");
                final String main = requireNonBlank(yaml.getString("main"), "main");
                final int apiVersion = yaml.getInt("api-version", -1);
                if (apiVersion < 0) {
                    throw new IllegalArgumentException("Missing api-version in " + jarPath.getFileName());
                }
                final int schemaVersion = yaml.getInt("schema-version", 1);
                final List<String> dependencyIds = yaml.getStringList("dependencies");
                final Set<ModuleId> dependencies = new LinkedHashSet<>();
                for (final String dependencyId : dependencyIds) {
                    if (dependencyId == null || dependencyId.isBlank()) {
                        throw new IllegalArgumentException("Blank dependency in " + jarPath.getFileName());
                    }
                    dependencies.add(ModuleId.of(dependencyId));
                }
                return new ModuleDescriptor(ModuleId.of(idRaw), main, apiVersion, Set.copyOf(dependencies), schemaVersion);
            }
        }
    }

    private static String requireNonBlank(final String value, final String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + key + " in " + DESCRIPTOR_NAME);
        }
        return value;
    }
}
