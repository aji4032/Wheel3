package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import tools.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an isolated browser context (similar to an incognito window).
 * <p>
 * Each {@code BrowserContext} has its own cookies, localStorage,
 * sessionStorage,
 * and cache — completely isolated from other contexts in the same browser
 * process.
 * This makes it safe for parallel test execution: each test thread creates its
 * own context, and there is zero shared state between them.
 *
 * <pre>
 * BrowserContext ctx = new BrowserContext(browser.wsUrl(), browser.port());
 * ICdpDriver driver = ctx.newDriver();
 * driver.get("https://example.com");
 * // ... test ...
 * driver.close();
 * ctx.close();
 * </pre>
 *
 * @see BrowserLauncher
 * @see CdpTestBase
 */
public class BrowserContext implements AutoCloseable {

    private final CdpUtility browserUtility;
    private final String browserContextId;
    private final int port;
    private final List<String> targetIds = new ArrayList<>();

    /**
     * Creates a new isolated browser context.
     *
     * @param browserWsUrl The browser-level WebSocket URL
     *                     (e.g.,
     *                     {@code ws://127.0.0.1:PORT/devtools/browser/UUID}).
     * @param port         The Chrome debugging port (for {@code /json} HTTP
     *                     queries).
     */
    public BrowserContext(String browserWsUrl, int port) {
        this.port = port;
        this.browserUtility = new CdpUtility(browserWsUrl);
        this.browserContextId = browserUtility.targetCreateBrowserContext();
        Log.info("Created BrowserContext: " + browserContextId);
    }

    /**
     * Opens a new page (tab) within this isolated context and returns
     * a ready-to-use {@link ICdpDriver} connected to it.
     */
    public ICdpDriver newDriver() {
        String pageWsUrl = newPage();
        return CdpHandler.createDriver(pageWsUrl);
    }

    /**
     * Opens a new page (tab) within this isolated context.
     *
     * @return The WebSocket debugger URL for the new page target.
     */
    public String newPage() {
        String targetId = browserUtility.targetCreateTarget("about:blank", browserContextId);
        targetIds.add(targetId);
        Log.info("Created page target: " + targetId + " in context: " + browserContextId);

        // Query /json to get the WebSocket URL for this target
        return getWsUrlForTarget(targetId);
    }

    /**
     * Returns the browser context ID (CDP {@code browserContextId}).
     */
    public String getContextId() {
        return browserContextId;
    }

    /**
     * Disposes of this context, closing all pages that belong to it.
     */
    @Override
    public void close() {
        // Close all targets we created
        for (String targetId : targetIds) {
            try {
                browserUtility.targetCloseTarget(targetId);
            } catch (Exception e) {
                Log.warn("Failed to close target " + targetId + ": " + e.getMessage());
            }
        }
        targetIds.clear();

        // Dispose the browser context itself
        try {
            browserUtility.targetDisposeBrowserContext(browserContextId);
            Log.info("Disposed BrowserContext: " + browserContextId);
        } catch (Exception e) {
            Log.warn("Failed to dispose BrowserContext " + browserContextId + ": " + e.getMessage());
        }

        // Close the browser-level utility connection
        try {
            browserUtility.close();
        } catch (Exception ignored) {
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Queries the Chrome HTTP debug endpoint to find the WebSocket URL for
     * a specific target by its targetId.
     */
    private String getWsUrlForTarget(String targetId) {
        HttpClient client = HttpClient.newHttpClient();
        // Retry a few times — the target may take a moment to appear in /json
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/json"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode targets = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(response.body());

                if (targets.isArray()) {
                    for (JsonNode target : targets) {
                        if (targetId.equals(target.path("id").asText())) {
                            String ws = target.path("webSocketDebuggerUrl").asText();
                            if (!ws.isEmpty()) {
                                return ws;
                            }
                        }
                    }
                }

                // Target not yet visible — wait a bit
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // retry
            }
        }
        throw new RuntimeException("Could not find WebSocket URL for target " + targetId
                + " on port " + port);
    }
}
