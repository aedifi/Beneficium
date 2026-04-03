package aedifi.bene.api.module;

import java.util.Optional;

public final class ModuleStatus {
    private ModuleStatus() {}

    public enum State {
        ENABLING,
        ENABLED,
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
