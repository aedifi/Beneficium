package aedifi.bene.script;

import aedifi.bene.api.service.Logging;
import aedifi.bene.command.CommandInvocation;
import aedifi.bene.util.MessageUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;

public final class LuaCommandRuntime {
    private static final long DEFAULT_TIMEOUT_MS = 100;

    private final JavaPlugin plugin;
    private final Logging logging;
    private final Path scriptRoot;
    private final ExecutorService executor;

    public LuaCommandRuntime(final JavaPlugin plugin, final Logging logging) {
        this.plugin = plugin;
        this.logging = logging;
        this.scriptRoot = plugin.getDataFolder().toPath().resolve("scripts");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "bene-lua");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Files.createDirectories(scriptRoot);
        } catch (final IOException ex) {
            logging.error("lua", "Failed to create scripts directory.", ex);
        }
    }

    public boolean execute(final String scriptRef, final CommandInvocation invocation) {
        final Path scriptPath;
        try {
            scriptPath = resolveScriptPath(scriptRef);
        } catch (final IllegalArgumentException ex) {
            logging.warn("lua", ex.getMessage());
            return false;
        }
        final String source;
        try {
            source = Files.readString(scriptPath, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            logging.error("lua", "Failed to read script " + scriptPath.getFileName(), ex);
            return false;
        }

        final Callable<Void> task = () -> {
            final Globals globals = createGlobals(invocation);
            final LuaValue chunk = globals.load(source, scriptPath.toString());
            chunk.call();
            return null;
        };

        final Future<Void> future = executor.submit(task);
        try {
            future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return true;
        } catch (final TimeoutException ex) {
            future.cancel(true);
            logging.warn("lua", "Script timed out after " + DEFAULT_TIMEOUT_MS + "ms: " + scriptRef);
            return false;
        } catch (final ExecutionException ex) {
            logging.error("lua", "Script failed: " + scriptRef, ex.getCause());
            return false;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            logging.warn("lua", "Script interrupted: " + scriptRef);
            return false;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private Path resolveScriptPath(final String scriptRef) {
        final Path root = scriptRoot.toAbsolutePath().normalize();
        final Path resolved = root.resolve(scriptRef).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Script path escapes script root: " + scriptRef);
        }
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("Script not found: " + scriptRef);
        }
        return resolved;
    }

    private Globals createGlobals(final CommandInvocation invocation) {
        final Globals globals = new Globals();
        globals.load(new BaseLib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new MathLib());
        globals.load(new CoroutineLib());

        final LuaTable argsTable = new LuaTable();
        for (final Map.Entry<String, Object> entry : invocation.arguments().entrySet()) {
            argsTable.set(entry.getKey(), toLuaValue(entry.getValue()));
        }

        final CommandSender sender = invocation.sender();
        final LuaTable senderTable = new LuaTable();
        senderTable.set("name", LuaValue.valueOf(sender.getName()));
        senderTable.set("is_player", LuaValue.valueOf(sender instanceof Player));
        if (sender instanceof Player player) {
            senderTable.set("uuid", LuaValue.valueOf(player.getUniqueId().toString()));
        }

        final LuaTable ctxTable = new LuaTable();
        ctxTable.set("sender", senderTable);
        ctxTable.set("args", argsTable);
        ctxTable.set("reply", new OneArgFunction() {
            @Override
            public LuaValue call(final LuaValue message) {
                final String text = message.checkjstring();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MessageUtil.component(text)));
                return LuaValue.NIL;
            }
        });
        ctxTable.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(final LuaValue message) {
                logging.info("lua", message.checkjstring());
                return LuaValue.NIL;
            }
        });

        globals.set("ctx", ctxTable);
        globals.set("args", argsTable);
        globals.set("sender", senderTable);
        return globals;
    }

    private static LuaValue toLuaValue(final Object value) {
        if (value instanceof Integer integer) {
            return LuaValue.valueOf(integer);
        }
        if (value instanceof Boolean bool) {
            return LuaValue.valueOf(bool);
        }
        if (value == null) {
            return LuaValue.NIL;
        }
        return LuaValue.valueOf(String.valueOf(value));
    }
}
