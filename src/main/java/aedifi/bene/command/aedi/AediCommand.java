package aedifi.bene.command.aedi;

import aedifi.bene.BenePlugin;
import aedifi.bene.api.command.ArgumentDispatcher;
import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.module.ModuleStatus;
import aedifi.bene.api.util.MessageUtil;
import aedifi.bene.core.PluginKernel;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class AediCommand implements CommandExecutor, TabCompleter {
    private static final String PERM_INFO = "bene.aedi.info";
    private static final String PERM_MODULES = "bene.aedi.modules";
    private static final String PERM_RESTART = "bene.aedi.restart";

    private final BenePlugin plugin;
    private final ArgumentDispatcher dispatcher;

    public AediCommand(final BenePlugin plugin) {
        this.plugin = plugin;
        this.dispatcher = new ArgumentDispatcher(
                "aedi",
                PERM_INFO,
                List.of(
                        new ArgumentDispatcher.ArgumentSpec(
                                "version",
                                List.of("info"),
                                PERM_INFO,
                                this::handleInfo),
                        new ArgumentDispatcher.ArgumentSpec(
                                "modules",
                                List.of("list"),
                                PERM_MODULES,
                                this::handleModules),
                        new ArgumentDispatcher.ArgumentSpec(
                                "reload",
                                List.of("restart"),
                                PERM_RESTART,
                                this::handleRestart)));
    }

    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args) {
        return dispatcher.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args) {
        return dispatcher.complete(sender, args);
    }

    private boolean handleInfo(final CommandSender sender, final String[] args) {
        sender.sendMessage(MessageUtil.personal("The server is running version &f" + plugin.getPluginMeta().getVersion() + "&7 of its plugin, " + plugin.getName() + " (&fhttps://github.com/aedifi/Beneficium&7)."));
        return true;
    }

    private boolean handleModules(final CommandSender sender, final String[] args) {
        final PluginKernel kernel = plugin.kernel();
        if (kernel == null) {
            sender.sendMessage(MessageUtil.error("The kernel is unavailable."));
            return true;
        }
        final Map<ModuleId, String> displayNames = kernel.moduleDisplayNames();
        final Map<ModuleId, ModuleStatus.Snapshot> snapshots = kernel.diagnostics().moduleSnapshots();
        if (snapshots.isEmpty()) {
            sender.sendMessage(MessageUtil.error("There are no modules currently present."));
            return true;
        }
        final List<ModuleStatus.Snapshot> ordered = snapshots.values().stream()
                .sorted(Comparator.comparing(snapshot -> snapshot.moduleId().value()))
                .toList();
        final long enabledCount = ordered.stream()
                .filter(s -> s.state() == ModuleStatus.State.ENABLED)
                .count();
        sender.sendMessage(MessageUtil.personal(
                "List of modules (" + enabledCount + "/" + ordered.size() + "):"));
        int ordinal = 1;
        for (final ModuleStatus.Snapshot snapshot : ordered) {
            sender.sendMessage(MessageUtil.component(moduleSnapshotLine(ordinal++, snapshot, displayNames)));
        }
        return true;
    }

    private static String moduleSnapshotLine(
            final int order,
            final ModuleStatus.Snapshot snapshot,
            final Map<ModuleId, String> displayNames) {
        final String pluginLabel =
                displayNames.getOrDefault(snapshot.moduleId(), snapshot.moduleId().value());
        final StringBuilder line = new StringBuilder();
        line.append("&7").append(order).append(". ")
                .append("&7").append(pluginLabel)
                .append("&7 - ")
                .append(colorForModuleState(snapshot.state()))
                .append(snapshot.state());
        snapshot.enableTimeMicros().ifPresent(enableMicros -> {
            final double ms = enableMicros / 1_000.0D;
            line.append("&7 (took ").append(String.format(Locale.ROOT, "%.3f", ms)).append("ms)");
        });
        snapshot.failureMessage().ifPresent(message -> line.append("&7 - &c").append(message));
        return line.toString();
    }

    private static String colorForModuleState(final ModuleStatus.State state) {
        return switch (state) {
            case ENABLED -> "&2";
            case ENABLING, DISABLING -> "&f";
            case DISABLED -> "&7";
            case FAILED -> "&c";
        };
    }

    private boolean handleRestart(final CommandSender sender, final String[] args) {
        sender.sendMessage(MessageUtil.personal("Restarting the server plugin..."));
        try {
            restartKernel();
            plugin.getServer().broadcast(MessageUtil.broadcast("The server plugin has been restarted."));
        } catch (final Exception ex) {
            sender.sendMessage(MessageUtil.error("The server plugin has failed to restart; check your logs!"));
            plugin.getLogger().severe("Log dump: " + ex.getMessage());
        }
        return true;
    }

    private synchronized void restartKernel() {
        final PluginKernel current = plugin.kernel();
        if (current != null) {
            current.shutdown();
        }
        final PluginKernel restarted = new PluginKernel(plugin);
        restarted.start();
        plugin.replaceKernel(restarted);
    }
}
