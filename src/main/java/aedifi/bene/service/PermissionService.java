package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Permissions;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.CommandSender;

public final class PermissionService implements Permissions {
    private static final String ROOT = "aedifi";

    private final PermissionChecker permissionChecker;

    public PermissionService() {
        this(CommandSender::hasPermission);
    }

    public PermissionService(final PermissionChecker permissionChecker) {
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker");
    }

    @Override
    public String node(final ModuleId moduleId, final String action) {
        final String module = sanitize(moduleId.value().replace('.', '-'));
        final String operation = sanitize(action);
        return ROOT + "." + module + "." + operation;
    }

    @Override
    public boolean hasPermission(final CommandSender sender, final String permissionNode) {
        return permissionChecker.hasPermission(sender, permissionNode);
    }

    @Override
    public boolean hasPermission(final CommandSender sender, final ModuleId moduleId, final String action) {
        return hasPermission(sender, node(moduleId, action));
    }

    private static String sanitize(final String raw) {
        final String normalized = raw.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Permission segments cannot be blank.");
        }
        final String safe = normalized.replaceAll("[^a-z0-9.-]+", "-");
        return safe.replaceAll("-{2,}", "-");
    }

    @FunctionalInterface
    public interface PermissionChecker {
        boolean hasPermission(CommandSender sender, String permissionNode);
    }
}
