package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise-grade CDP network interceptor
 */
public final class ApiInterceptor {

    private final CdpClient client;

    // URL filter → future
    private final ConcurrentHashMap<String, CompletableFuture<ApiResponse>> pending = new ConcurrentHashMap<>();

    // requestId → URL
    private final ConcurrentHashMap<String, String> requestMap = new ConcurrentHashMap<>();

    // requestId → response metadata
    private final ConcurrentHashMap<String, JsonNode> responseMap = new ConcurrentHashMap<>();

    public ApiInterceptor(CdpClient client) {
        this.client = client;

        try {
            client.sendCommand("Network.enable", Duration.ofSeconds(5));
        } catch (Exception e) {
            throw new RuntimeException("Failed to enable Network domain", e);
        }

        registerListeners();
    }

    private void registerListeners() {

        client.addEventListener(event -> {

            if (!event.has("method")) return;

            switch (event.get("method").asText()) {

                case "Network.requestWillBeSent":
                    handleRequest(event);
                    break;

                case "Network.responseReceived":
                    handleResponse(event);
                    break;

                case "Network.loadingFinished":
                    handleBody(event);
                    break;
            }
        });
    }

    private void handleRequest(JsonNode event) {
        String url = event.get("params").get("request").get("url").asText();
        String requestId = event.get("params").get("requestId").asText();

        for (String key : pending.keySet()) {
            if (url.contains(key)) {
                requestMap.put(requestId, key);
            }
        }
    }

    private void handleResponse(JsonNode event) {
        System.out.println(event);
        String requestId = event.get("params").get("requestId").asText();

        if (requestMap.containsKey(requestId)) {
            responseMap.put(requestId, event.get("params").get("response"));
        }
    }

    private void handleBody(JsonNode event) {
        String requestId = event.get("params").get("requestId").asText();

        if (!requestMap.containsKey(requestId)) return;

        String urlKey = requestMap.get(requestId);
        CompletableFuture<ApiResponse> future = pending.get(urlKey);

        try {
            JsonNode body = client.sendCommand(
                    "Network.getResponseBody",
                    Map.of("requestId", requestId),
                    Duration.ofSeconds(5)
            );

            JsonNode response = responseMap.get(requestId);

            ApiResponse apiResponse = new ApiResponse(
                    response.get("url").asText(),
                    response.get("status").asInt(),
                    response.get("mimeType").asText(),
                    body.get("body").asText()
            );

            future.complete(apiResponse);

        } catch (Exception e) {
            future.completeExceptionally(e);
        } finally {
            pending.remove(urlKey);
            requestMap.remove(requestId);
            responseMap.remove(requestId);
        }
    }

    /**
     * Blocks until the API matching the urlFragment returns.
     */
    public ApiResponse waitFor(String urlFragment, Duration timeout)
            throws TimeoutException, ExecutionException, InterruptedException {

        CompletableFuture<ApiResponse> future = new CompletableFuture<>();
        pending.put(urlFragment, future);

        return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}

