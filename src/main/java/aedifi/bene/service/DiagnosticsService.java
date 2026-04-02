package aedifi.bene.service;

import aedifi.bene.module.ModuleId;
import aedifi.bene.module.ModuleStatus;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DiagnosticsService {
    private Supplier<Map<ModuleId, ModuleStatus.Snapshot>> lifecycleSnapshotProvider;

    public DiagnosticsService() {
        this.lifecycleSnapshotProvider = Map::of;
    }

    public void bindLifecycleSnapshots(final Supplier<Map<ModuleId, ModuleStatus.Snapshot>> lifecycleSnapshotProvider) {
        this.lifecycleSnapshotProvider = Objects.requireNonNull(lifecycleSnapshotProvider, "lifecycleSnapshotProvider");
    }

    public Map<ModuleId, ModuleStatus.Snapshot> moduleSnapshots() {
        return lifecycleSnapshotProvider.get();
    }
}
