package aedifi.bene.module.external;

import aedifi.bene.api.module.ModuleId;
import java.util.Set;

public record ModuleDescriptor(
        ModuleId id,
        String mainClass,
        int apiVersion,
        Set<ModuleId> dependencies,
        int schemaVersion) {}
