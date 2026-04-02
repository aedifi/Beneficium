package aedifi.bene.module;

import java.util.Locale;
import java.util.Objects;

public record ModuleId(String value) {
    public ModuleId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Module ID cannot be blank.");
        }
    }

    public static ModuleId of(final String rawValue) {
        return new ModuleId(rawValue.toLowerCase(Locale.ROOT).trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
