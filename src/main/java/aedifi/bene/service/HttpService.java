package aedifi.bene.service;

import aedifi.bene.api.module.ModuleId;
import aedifi.bene.api.service.Http;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class HttpService implements Http {
    private static final String COMPONENT = "http";
    private static final int STATUS_NO_CONTENT = 204;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_INTERNAL_ERROR = 500;
    private static final int THREAD_POOL_SIZE = 8;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int STOP_DELAY_SECONDS = 2;

    private final LoggingService logging;
    private final boolean enabled;
    private final String address;
    private final int port;
    private final List<RegisteredRoute> routes = new ArrayList<>();
    private final Map<ModuleId, List<RegisteredRoute>> routesByOwner = new ConcurrentHashMap<>();

    private HttpServer server;
    private ExecutorService executor;

    public HttpService(final LoggingService logging, final boolean enabled, final String address, final int port) {
        this.logging = logging;
        this.enabled = enabled;
        this.address = address;
        this.port = port;
    }

    public void start() {
        if (!enabled) {
            logging.info(COMPONENT, "HTTP listener is disabled in config; skipping bind.");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(address, port), 0);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to bind HTTP listener on " + address + ":" + port, ex);
        }
        executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new NamedThreadFactory());
        server.setExecutor(executor);
        server.createContext("/", new RootHandler());
        server.start();
        logging.info(COMPONENT, "HTTP listener bound to " + address + ":" + port + ".");
    }

    public void stop() {
        if (!enabled) {
            return;
        }
        if (server != null) {
            server.stop(STOP_DELAY_SECONDS);
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (final InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        synchronized (routes) {
            routes.clear();
        }
        routesByOwner.clear();
        logging.info(COMPONENT, "HTTP listener stopped.");
    }

    @Override
    public void register(final ModuleId owner, final Route route, final Handler handler) {
        final RegisteredRoute registered = new RegisteredRoute(owner, route, handler, PathTemplate.compile(route.path()));
        synchronized (routes) {
            for (final RegisteredRoute existing : routes) {
                if (existing.route.method() == route.method() && existing.template.pattern.equals(registered.template.pattern)) {
                    throw new IllegalStateException(
                            "Route already registered: " + route.method() + " " + route.path()
                                    + " (owner=" + existing.owner.value() + ")");
                }
            }
            routes.add(registered);
        }
        routesByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>(2)).add(registered);
    }

    @Override
    public void unregisterOwnerRoutes(final ModuleId owner) {
        final List<RegisteredRoute> owned = routesByOwner.remove(owner);
        if (owned == null) {
            return;
        }
        synchronized (routes) {
            routes.removeAll(owned);
        }
    }

    private final class RootHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            try {
                final String rawMethod = exchange.getRequestMethod();
                if ("OPTIONS".equalsIgnoreCase(rawMethod)) {
                    handlePreflight(exchange);
                    return;
                }

                final Method method;
                try {
                    method = Method.valueOf(rawMethod.toUpperCase(Locale.ROOT));
                } catch (final IllegalArgumentException ex) {
                    respondEmpty(exchange, STATUS_METHOD_NOT_ALLOWED);
                    return;
                }

                final String path = exchange.getRequestURI().getPath();
                MatchedRoute matched = null;
                boolean pathMatchedDifferentMethod = false;
                final List<RegisteredRoute> snapshot;
                synchronized (routes) {
                    snapshot = new ArrayList<>(routes);
                }
                for (final RegisteredRoute candidate : snapshot) {
                    final Map<String, String> params = candidate.template.match(path);
                    if (params == null) {
                        continue;
                    }
                    if (candidate.route.method() != method) {
                        pathMatchedDifferentMethod = true;
                        continue;
                    }
                    matched = new MatchedRoute(candidate, params);
                    break;
                }

                if (matched == null) {
                    respondEmpty(exchange, pathMatchedDifferentMethod ? STATUS_METHOD_NOT_ALLOWED : STATUS_NOT_FOUND);
                    return;
                }

                final ExchangeRequest request = new ExchangeRequest(exchange, method, path, matched.params);
                final ExchangeResponse response = new ExchangeResponse(exchange);
                try {
                    matched.route.handler.handle(request, response);
                    response.finishIfNeeded();
                } catch (final Exception ex) {
                    logging.error(COMPONENT, "Handler threw for " + method + " " + path, ex);
                    if (!response.headersSent()) {
                        respondEmpty(exchange, STATUS_INTERNAL_ERROR);
                    }
                }
            } finally {
                exchange.close();
            }
        }

        private void handlePreflight(final HttpExchange exchange) throws IOException {
            applyCorsHeaders(exchange);
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, HEAD");
            final List<String> requestedHeaders = exchange.getRequestHeaders().get("Access-Control-Request-Headers");
            if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", String.join(", ", requestedHeaders));
            }
            exchange.getResponseHeaders().add("Access-Control-Max-Age", "600");
            respondEmpty(exchange, STATUS_NO_CONTENT);
        }

        private void respondEmpty(final HttpExchange exchange, final int status) throws IOException {
            applyCorsHeaders(exchange);
            exchange.sendResponseHeaders(status, -1);
        }
    }

    private static void applyCorsHeaders(final HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Vary", "Origin");
    }

    private record RegisteredRoute(ModuleId owner, Route route, Handler handler, PathTemplate template) {}

    private record MatchedRoute(RegisteredRoute route, Map<String, String> params) {}

    private static final class PathTemplate {
        private final String pattern;
        private final String[] segments;
        private final boolean[] isParam;

        private PathTemplate(final String pattern, final String[] segments, final boolean[] isParam) {
            this.pattern = pattern;
            this.segments = segments;
            this.isParam = isParam;
        }

        static PathTemplate compile(final String pattern) {
            final String[] raw = splitPath(pattern);
            final String[] segments = new String[raw.length];
            final boolean[] isParam = new boolean[raw.length];
            for (int i = 0; i < raw.length; i++) {
                final String segment = raw[i];
                if (segment.startsWith("{") && segment.endsWith("}") && segment.length() > 2) {
                    segments[i] = segment.substring(1, segment.length() - 1);
                    isParam[i] = true;
                } else {
                    segments[i] = segment;
                    isParam[i] = false;
                }
            }
            return new PathTemplate(pattern, segments, isParam);
        }

        Map<String, String> match(final String path) {
            final String[] parts = splitPath(path);
            if (parts.length != segments.length) {
                return null;
            }
            Map<String, String> params = null;
            for (int i = 0; i < segments.length; i++) {
                if (isParam[i]) {
                    if (params == null) {
                        params = new LinkedHashMap<>(2);
                    }
                    params.put(segments[i], parts[i]);
                } else if (!segments[i].equals(parts[i])) {
                    return null;
                }
            }
            return params == null ? Map.of() : Collections.unmodifiableMap(params);
        }

        private static String[] splitPath(final String path) {
            final String trimmed = path.startsWith("/") ? path.substring(1) : path;
            if (trimmed.isEmpty()) {
                return new String[0];
            }
            return trimmed.split("/", -1);
        }
    }

    private static final class ExchangeRequest implements Request {
        private final HttpExchange exchange;
        private final Method method;
        private final String path;
        private final Map<String, String> pathParameters;
        private Map<String, List<String>> cachedQuery;
        private Map<String, List<String>> cachedHeaders;

        ExchangeRequest(
                final HttpExchange exchange,
                final Method method,
                final String path,
                final Map<String, String> pathParameters) {
            this.exchange = exchange;
            this.method = method;
            this.path = path;
            this.pathParameters = pathParameters;
        }

        @Override
        public Method method() {
            return method;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public Map<String, String> pathParameters() {
            return pathParameters;
        }

        @Override
        public Map<String, List<String>> queryParameters() {
            if (cachedQuery == null) {
                cachedQuery = parseQuery(exchange.getRequestURI().getRawQuery());
            }
            return cachedQuery;
        }

        @Override
        public Map<String, List<String>> headers() {
            if (cachedHeaders == null) {
                final Map<String, List<String>> copy = new LinkedHashMap<>();
                for (final Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                    copy.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
                cachedHeaders = Collections.unmodifiableMap(copy);
            }
            return cachedHeaders;
        }

        @Override
        public InputStream body() {
            return exchange.getRequestBody();
        }

        private static Map<String, List<String>> parseQuery(final String rawQuery) {
            if (rawQuery == null || rawQuery.isEmpty()) {
                return Map.of();
            }
            final Map<String, List<String>> parsed = new LinkedHashMap<>();
            for (final String pair : rawQuery.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                final int eq = pair.indexOf('=');
                final String key = decode(eq < 0 ? pair : pair.substring(0, eq));
                final String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
                parsed.computeIfAbsent(key, ignored -> new ArrayList<>(1)).add(value);
            }
            return Collections.unmodifiableMap(parsed);
        }

        private static String decode(final String value) {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }

    private static final class ExchangeResponse implements Response {
        private final HttpExchange exchange;
        private int status = 200;
        private boolean headersSent;
        private boolean finished;

        ExchangeResponse(final HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public Response status(final int code) {
            ensureNotSent();
            this.status = code;
            return this;
        }

        @Override
        public Response header(final String name, final String value) {
            ensureNotSent();
            exchange.getResponseHeaders().add(name, value);
            return this;
        }

        @Override
        public void body(final byte[] bytes) throws IOException {
            ensureNotSent();
            applyCorsHeaders(exchange);
            headersSent = true;
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            finished = true;
        }

        @Override
        public void body(final String text) throws IOException {
            body(text.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void bodyStream(final StreamWriter writer) throws IOException {
            ensureNotSent();
            applyCorsHeaders(exchange);
            headersSent = true;
            exchange.sendResponseHeaders(status, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                writer.write(out);
            }
            finished = true;
        }

        void finishIfNeeded() throws IOException {
            if (finished) {
                return;
            }
            applyCorsHeaders(exchange);
            headersSent = true;
            exchange.sendResponseHeaders(status, -1);
            finished = true;
        }

        boolean headersSent() {
            return headersSent;
        }

        private void ensureNotSent() {
            if (headersSent) {
                throw new IllegalStateException("Response already committed.");
            }
        }
    }

    private static final class NamedThreadFactory implements java.util.concurrent.ThreadFactory {
        private static final Map<String, AtomicInteger> COUNTERS = new HashMap<>();
        private final AtomicInteger counter;

        NamedThreadFactory() {
            synchronized (COUNTERS) {
                counter = COUNTERS.computeIfAbsent("bene-http", ignored -> new AtomicInteger());
            }
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, "bene-http-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
