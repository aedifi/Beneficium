package aedifi.bene.api.module.descriptor;

import aedifi.bene.api.module.ModuleId;

public record ModuleDescriptor(
        ModuleId id,
        String mainClass,
        int apiVersion) {
    public static final String FILE_NAME = "module.yml";

    public static final class Keys {
        public static final String ID = "id";
        public static final String MAIN = "main";
        public static final String API_VERSION = "api-version";

        private Keys() {}
    }
}
