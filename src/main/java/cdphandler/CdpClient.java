package cdphandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lightweight Chrome DevTools Protocol client using java.net.http.WebSocket.
 *
 * Example usage:
 *   ChromeDevToolsClient client = new ChromeDevToolsClient("ws://127.0.0.1:9222/devtools/page/...");
 *   JsonNode result = client.sendCommand("Page.navigate", Map.of("url", "https://example.com"), Duration.ofSeconds(10));
 *   client.addEventListener(event -> System.out.println("Event: " + event.toString()));
 *   client.close();
 */
public final class CdpClient implements WebSocket.Listener, AutoCloseable {
    private final URI websocketUri;
    private final HttpClient httpClient;
    private volatile WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<JsonNode>> eventListeners = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();
    private final ExecutorService listenerExecutor;

    /**
     * Construct and connect synchronously to the supplied websocket debugger address.
     *
     * @param websocketDebuggerAddress ws:// or wss:// address of the Chrome DevTools websocket.
     * @throws RuntimeException on connection failure
     */
    public CdpClient(String websocketDebuggerAddress) {
        Objects.requireNonNull(websocketDebuggerAddress, "websocketDebuggerAddress required");
        this.websocketUri = URI.create(websocketDebuggerAddress);
        this.httpClient = HttpClient.newHttpClient();
        this.listenerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cdp-ws-listener");
            t.setDaemon(true);
            return t;
        });

        // Connect synchronously; throws RuntimeException on failure.
        try {
            this.webSocket = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(this.websocketUri, this)
                    .join();
            // Wait for onOpen -> connectFuture completion (with timeout).
            connectFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to CDP websocket: " + websocketDebuggerAddress, e);
        }
    }

    /**
     * Send a CDP command with a parameter map and wait for the result.
     *
     * @param method   CDP method name (e.g. "Page.navigate", "Runtime.evaluate")
     * @param params   Parameters map (nullable)
     * @param timeout  Timeout to wait for response
     * @return Result JsonNode (the "result" object returned by CDP)
     * @throws TimeoutException if response not received in time
     * @throws ExecutionException if underlying error occurred
     * @throws InterruptedException if interrupted while waiting
     */
    public JsonNode sendCommand(String method, Map<String, Object> params, Duration timeout)
            throws TimeoutException, ExecutionException, InterruptedException {
        int id = idCounter.getAndIncrement();
        ObjectNodeBuilder wrapper = new ObjectNodeBuilder(objectMapper);
        wrapper.put("id", id);
        wrapper.put("method", method);
        if (params != null) wrapper.putObject("params", params);

        String payload;
        try {
            payload = wrapper.buildString();
        } catch (JsonProcessingException e) {
            throw new ExecutionException("Failed to serialize command payload", e);
        }

        CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
        pendingRequests.put(id, responseFuture);

        // Send text message
        CompletableFuture<WebSocket> sendFuture = webSocket.sendText(payload, true);

        try {
            // Ensure send succeeded
            sendFuture.join();
        } catch (CompletionException ce) {
            pendingRequests.remove(id);
            throw new ExecutionException("Failed to send CDP command", ce);
        }

        // Wait for response
        try {
            JsonNode message = responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            // CDP responses will contain either "result" or "error"
            if (message.has("error")) {
                throw new ExecutionException(new RuntimeException("CDP error: " + message.get("error").toString()));
            }
            return message.get("result") != null ? message.get("result") : message;
        } catch (TimeoutException te) {
            pendingRequests.remove(id);
            throw te;
        }
    }

    /**
     * Convenience overload for no-params commands.
     */
    public JsonNode sendCommand(String method, Duration timeout)
            throws TimeoutException, ExecutionException, InterruptedException {
        return sendCommand(method, null, timeout);
    }

    /**
     * Register an event listener. CDP events (messages without "id") will be posted to listeners.
     *
     * @param listener consumer receiving event JsonNode
     */
    public void addEventListener(Consumer<JsonNode> listener) {
        eventListeners.add(listener);
    }

    /**
     * Remove a previously registered event listener.
     */
    public void removeEventListener(Consumer<JsonNode> listener) {
        eventListeners.remove(listener);
    }

    /**
     * Close the websocket and release resources.
     */
    @Override
    public void close() {
        try {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "client closing").join();
            }
        } catch (Exception ignored) {
        } finally {
            listenerExecutor.shutdownNow();
            pendingRequests.forEach((id, f) -> f.completeExceptionally(new RuntimeException("Client closed")));
            pendingRequests.clear();
        }
    }

    // WebSocket.Listener implementations

    @Override
    public void onOpen(WebSocket webSocket) {
        // mark as connected
        connectFuture.complete(null);
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        final String payload = data.toString();
        // Process message asynchronously on listenerExecutor to avoid blocking web socket IO thread
        listenerExecutor.submit(() -> handleIncomingMessage(payload));
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        // CDP uses text frames; binary unexpected — ignore or log as needed.
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        // respond with pong (default behavior may handle, but ensure explicit Pong)
        webSocket.request(1);
        return webSocket.sendPong(message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        // onClose called when connection closed
        pendingRequests.forEach((id, cf) -> cf.completeExceptionally(new RuntimeException("WebSocket closed: " + reason)));
        pendingRequests.clear();
        connectFuture.completeExceptionally(new RuntimeException("WebSocket closed: " + reason));
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        pendingRequests.forEach((id, cf) -> cf.completeExceptionally(error));
        pendingRequests.clear();
        connectFuture.completeExceptionally(error);
    }

    // Internal message handling
    private void handleIncomingMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("id")) {
                int id = node.get("id").asInt();
                CompletableFuture<JsonNode> cf = pendingRequests.remove(id);
                if (cf != null) {
                    cf.complete(node);
                    return;
                } else {
                    // No pending request — ignore or log
                }
            }
            // Treat as event (no "id")
            for (Consumer<JsonNode> listener : eventListeners) {
                try {
                    listener.accept(node);
                } catch (Exception ex) {
                    // swallow per-listener exception to avoid impacting other listeners
                }
            }
        } catch (Exception e) {
            // log parse error if necessary
        }
    }

    // Helper builder to produce compact JSON without exposing ObjectNode to consumers
    private static final class ObjectNodeBuilder {
        private final ObjectMapper mapper;
        private final com.fasterxml.jackson.databind.node.ObjectNode root;

        ObjectNodeBuilder(ObjectMapper mapper) {
            this.mapper = mapper;
            this.root = mapper.createObjectNode();
        }

        void put(String key, int value) {
            root.put(key, value);
        }

        void put(String key, String value) {
            root.put(key, value);
        }

        void putObject(String key, Map<String, Object> map) {
            if (map == null) return;
            com.fasterxml.jackson.databind.node.ObjectNode p = mapper.createObjectNode();
            map.forEach((k, v) -> p.set(k, mapper.valueToTree(v)));
            root.set(key, p);
        }

        String buildString() throws JsonProcessingException {
            return mapper.writeValueAsString(root);
        }
    }
}

