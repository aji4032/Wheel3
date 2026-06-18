package w3c;

import logger.Log;
import logger.Logger;
import w3c.server.W3CClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class W3CWindow {
    private static final Logger log = Log.getLogger(W3CWindow.class);
    private final W3CDriver driver;
    private final String title;
    private final W3CClient client;
    private final String sessionId;

    protected W3CWindow(W3CDriver driver, String title, W3CClient client, String sessionId) {
        this.driver = driver;
        this.title = title;
        this.client = client;
        this.sessionId = sessionId;
    }

    public String title() {
        return title;
    }

    public String sessionId() {
        return sessionId;
    }

    public W3CElement findElement(W3CBy by) {
        try {
            String using = mapLocatorType(by.type());
            Map<String, String> body = Map.of(
                "using", using,
                "value", by.locator()
            );
            JsonNode response = client.execute("POST", "/session/" + sessionId + "/element", body);
            String elementId = response.get("value").get("element-6066-11e4-a52e-4f735466cecf").asText();
            return new W3CElement(driver, this, client, sessionId, elementId, by);
        } catch (Exception e) {
            log.error("Failed to find element: " + by.name() + " using: " + by, e);
            return null;
        }
    }

    String mapLocatorType(W3CLocatorType type) {
        return switch (type) {
            case AUTOMATION_ID -> "accessibility id";
            case CLASS_NAME -> "class name";
            case NAME -> "name";
            case XPATH -> "xpath";
        };
    }

    public void closeWindow() {
        try {
            client.execute("DELETE", "/session/" + sessionId + "/window", null);
            log.info("Close window: {}", title);
        } catch (Exception e) {
            log.error("Failed to close window: {}", title, e);
        }
    }

    public void focusWindow() {
        try {
            client.execute("POST", "/session/" + sessionId + "/window/focus", null);
        } catch (Exception e) {
            log.error("Failed to focus window: {}", title, e);
        }
    }

    public void maximizeWindow() {
        try {
            client.execute("POST", "/session/" + sessionId + "/window/maximize", null);
            log.info("Maximize window: {}", title);
        } catch (Exception e) {
            log.error("Failed to maximize window: {}", title, e);
        }
    }

    public void minimizeWindow() {
        try {
            client.execute("POST", "/session/" + sessionId + "/window/minimize", null);
            log.info("Minimize window: {}", title);
        } catch (Exception e) {
            log.error("Failed to minimize window: {}", title, e);
        }
    }
}
