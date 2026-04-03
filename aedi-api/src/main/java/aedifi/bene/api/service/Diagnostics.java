package aedifi.bene.api.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.ModuleStatus;
import java.util.Map;

public interface Diagnostics {
    Map<ModuleId, ModuleStatus.Snapshot> moduleSnapshots();
}
