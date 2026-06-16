package w3c;

import logger.Log;
import logger.Logger;
import w3c.server.W3CClient;
import w3c.server.W3CWebDriverServer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public class W3CDriver {
    private static final Logger log = Log.getLogger(W3CDriver.class);
    private static W3CDriver INSTANCE;
    private static W3CWebDriverServer serverInstance;
    private static int serverPort;
    private static W3CClient client;

    private String sessionId;

    private W3CDriver() {
        // Private constructor for singleton
    }

    public static synchronized W3CDriver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new W3CDriver();
            ensureServerStarted();
        }
        return INSTANCE;
    }

    private static synchronized void ensureServerStarted() {
        if (serverInstance == null) {
            int port = findFreePort();
            serverInstance = new W3CWebDriverServer(port);
            serverInstance.start();
            serverPort = port;
            client = new W3CClient("http://localhost:" + serverPort);
            log.info("W3C WebDriver Server started on port " + serverPort);

            // Register JVM shutdown hook to clean up the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (serverInstance != null) {
                        serverInstance.stop();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }));
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            log.warn("Failed to find free port dynamically. Falling back to port 4723.", e);
            return 4723;
        }
    }

    public static W3CClient getClient() {
        ensureServerStarted();
        return client;
    }

    public W3CWindow getWindow(String title) {
        try {
            ensureServerStarted();
            Map<String, Object> capabilities = Map.of(
                "capabilities", Map.of(
                    "alwaysMatch", Map.of(
                        "app", title
                    )
                )
            );
            JsonNode response = client.execute("POST", "/session", capabilities);
            this.sessionId = response.get("value").get("sessionId").asText();
            log.info("Started W3C session: " + this.sessionId + " for window: " + title);
            return new W3CWindow(this, title, client, sessionId);
        } catch (Exception e) {
            log.error("Failed to start W3C session for window: " + title, e);
            return null;
        }
    }

    public void quit() {
        if (sessionId != null) {
            try {
                client.execute("DELETE", "/session/" + sessionId, null);
                log.info("Closed W3C session: " + sessionId);
                sessionId = null;
            } catch (Exception e) {
                log.error("Failed to close W3C session: " + sessionId, e);
            }
        }
    }

    public void close() {
        quit();
    }
}
