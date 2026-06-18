package w3c;

import logger.Log;
import logger.Logger;
import w3c.server.W3CClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class W3CElement {
    private static final Logger log = Log.getLogger(W3CElement.class);
    private final W3CDriver driver;
    private final W3CWindow window;
    private final W3CClient client;
    private final String sessionId;
    private final String elementId;
    private final W3CBy by;

    protected W3CElement(W3CDriver driver, W3CWindow window, W3CClient client, String sessionId, String elementId, W3CBy by) {
        this.driver = driver;
        this.window = window;
        this.client = client;
        this.sessionId = sessionId;
        this.elementId = elementId;
        this.by = by;
    }

    public String elementId() {
        return elementId;
    }

    public String sessionId() {
        return sessionId;
    }

    public W3CBy by() {
        return by;
    }

    public W3CElement findElement(W3CBy childBy) {
        try {
            String using = mapLocatorType(childBy.type());
            Map<String, String> body = Map.of(
                "using", using,
                "value", childBy.locator()
            );
            JsonNode response = client.execute("POST", "/session/" + sessionId + "/element/" + elementId + "/element", body);
            String childElementId = response.get("value").get("element-6066-11e4-a52e-4f735466cecf").asText();
            return new W3CElement(driver, window, client, sessionId, childElementId, childBy);
        } catch (Exception e) {
            log.error("Failed to find child element: " + childBy.name() + " using: " + childBy, e);
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

    public void clickButton() {
        try {
            client.execute("POST", "/session/" + sessionId + "/element/" + elementId + "/click", null);
            log.info("Clicked button: '{}'", by.name());
        } catch (Exception e) {
            log.error("Failed to click button: '{}'", by.name(), e);
        }
    }

    public void setEditBoxValue(String value) {
        try {
            Map<String, Object> body = Map.of("text", value);
            client.execute("POST", "/session/" + sessionId + "/element/" + elementId + "/value", body);
            log.info("Setting '{}' edit box value to: '{}'", by.name(), value);
        } catch (Exception e) {
            log.error("Failed to set '{}' edit box value to: '{}'", by.name(), value, e);
        }
    }

    public String getEditBoxValue() {
        try {
            JsonNode response = client.execute("GET", "/session/" + sessionId + "/element/" + elementId + "/text", null);
            return response.get("value").asText();
        } catch (Exception e) {
            log.error("Failed to get '{}' edit box value!", by.name(), e);
            return null;
        }
    }

    public String getText() {
        try {
            JsonNode response = client.execute("GET", "/session/" + sessionId + "/element/" + elementId + "/text", null);
            return response.get("value").asText();
        } catch (Exception e) {
            log.error("Failed to get text for element: '{}'", by.name(), e);
            return null;
        }
    }
}
