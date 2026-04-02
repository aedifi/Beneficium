package aedifi.bene.module;

import java.util.Optional;

/**
 * Lifecycle phase and read-only status snapshots for diagnostics.
 */
public final class ModuleStatus {
    private ModuleStatus() {}

    public enum State {
        ENABLING,
        ENABLED,
        POST_ENABLED,
        DISABLING,
        DISABLED,
        FAILED
    }

    public record Snapshot(
            ModuleId moduleId,
            State state,
            Optional<Long> enableTimeMicros,
            Optional<String> failureMessage) {}
}
