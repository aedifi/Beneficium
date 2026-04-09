package aedifi.bene.module.external;

import aedifi.bene.api.module.Module;
import aedifi.bene.api.module.descriptor.ModuleDescriptor;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

public record LoadedModule(
        ModuleDescriptor descriptor,
        Module module,
        URLClassLoader classLoader,
        Path jarPath) {
    public void close() throws IOException {
        classLoader.close();
    }
}
