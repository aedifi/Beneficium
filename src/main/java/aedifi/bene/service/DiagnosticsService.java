package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.ModuleStatus;
import aedifi.bene.api.service.Diagnostics;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DiagnosticsService implements Diagnostics {
    private Supplier<Map<ModuleId, ModuleStatus.Snapshot>> lifecycleSnapshotProvider;

    public DiagnosticsService() {
        this.lifecycleSnapshotProvider = Map::of;
    }

    public void bindLifecycleSnapshots(final Supplier<Map<ModuleId, ModuleStatus.Snapshot>> lifecycleSnapshotProvider) {
        this.lifecycleSnapshotProvider = Objects.requireNonNull(lifecycleSnapshotProvider, "lifecycleSnapshotProvider");
    }

    @Override
    public Map<ModuleId, ModuleStatus.Snapshot> moduleSnapshots() {
        return lifecycleSnapshotProvider.get();
    }
}
