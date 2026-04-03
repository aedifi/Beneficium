package aedifi.bene.api.service;

import aedifi.bene.api.module.ModuleId;
import org.bukkit.command.CommandSender;

public interface Permissions {
    String node(ModuleId moduleId, String action);

    boolean hasPermission(CommandSender sender, String permissionNode);

    boolean hasPermission(CommandSender sender, ModuleId moduleId, String action);
}
