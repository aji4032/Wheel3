package mcp;

import cdphandler.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tools.Log;

import java.util.List;

/**
 * Dispatches MCP tools/call requests to the underlying ICdpDriver.
 * Natural-language element descriptions (click, type, get_text) are
 * converted into NATURAL_LANGUAGE CdpBy objects, which the existing
 * OllamaProxy intercepts and resolves via Ollama automatically.
 */
public class McpToolDispatcher {

    private final ICdpDriver driver;
    private final ObjectMapper mapper = McpResponse.mapper();

    public McpToolDispatcher(ICdpDriver driver) {
        this.driver = driver;
    }

    /**
     * Dispatch a tools/call request and return a result JSON node.
     * Throws RuntimeException on tool-level errors (caller catches and returns
     * JSON-RPC error).
     */
    public JsonNode dispatch(String toolName, JsonNode args) {
        return switch (toolName) {
            case "navigate" -> navigate(args);
            case "click" -> click(args);
            case "type" -> type(args);
            case "get_text" -> getText(args);
            case "get_page_title" -> textResult(driver.getTitle());
            case "get_current_url" -> textResult(driver.getCurrentUrl());
            case "get_page_source" -> textResult(driver.getPageSource());
            case "take_screenshot" -> screenshotResult();
            case "go_back" -> {
                driver.back();
                yield textResult("navigated back");
            }
            case "go_forward" -> {
                driver.forward();
                yield textResult("navigated forward");
            }
            case "refresh" -> {
                driver.refresh();
                yield textResult("page refreshed");
            }
            case "press_key" -> pressKey(args);
            case "execute_action" -> executeAction(args);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    // -----------------------------------------------------------------------
    // Individual tool implementations
    // -----------------------------------------------------------------------

    private JsonNode navigate(JsonNode args) {
        String url = requireString(args, "url");
        driver.get(url);
        return textResult("navigated to " + driver.getCurrentUrl());
    }

    private JsonNode click(JsonNode args) {
        String description = requireString(args, "description");
        ICdpElement element = driver.findElement(
                new CdpBy(description, CdpLocatorType.NATURAL_LANGUAGE, description));
        element.click();
        return textResult("clicked element: " + description);
    }

    private JsonNode type(JsonNode args) {
        String description = requireString(args, "description");
        String text = requireString(args, "text");
        ICdpElement element = driver.findElement(
                new CdpBy(description, CdpLocatorType.NATURAL_LANGUAGE, description));
        element.sendKeys(text);
        return textResult("typed \"" + text + "\" into element: " + description);
    }

    private JsonNode getText(JsonNode args) {
        String description = requireString(args, "description");
        ICdpElement element = driver.findElement(
                new CdpBy(description, CdpLocatorType.NATURAL_LANGUAGE, description));
        return textResult(element.getText());
    }

    private JsonNode pressKey(JsonNode args) {
        String keyName = requireString(args, "key");
        CdpKey key;
        try {
            key = CdpKey.valueOf(keyName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown key '" + keyName
                            + "'. Valid keys: Enter, Tab, Escape, Space, ArrowLeft, ArrowRight, ArrowUp, ArrowDown, Backspace, Delete, PageUp, PageDown, Home, End, F1‑F12, etc.");
        }
        driver.keyPress(key);
        return textResult("pressed key: " + keyName);
    }

    private JsonNode executeAction(JsonNode args) {
        String instruction = requireString(args, "instruction");
        String html = driver.getPageSource();

        Log.info("execute_action: asking Ollama to plan steps for: " + instruction);
        List<String> steps = OllamaUtility.planActions(html, instruction);

        StringBuilder summary = new StringBuilder();
        for (String step : steps) {
            Log.info("execute_action step: " + step);
            summary.append(step).append("\n");
            // Each step is a natural-language description of an element to click.
            // More sophisticated step parsing can be added over time.
            try {
                ICdpElement element = driver.findElement(
                        new CdpBy(step, CdpLocatorType.NATURAL_LANGUAGE, step));
                element.click();
                summary.append("  -> clicked\n");
            } catch (Exception e) {
                summary.append("  -> skipped (").append(e.getMessage()).append(")\n");
            }
        }
        return textResult("Executed action plan:\n" + summary);
    }

    private JsonNode screenshotResult() {
        String base64 = driver.captureScreenshot();
        ObjectNode content = mapper.createObjectNode();
        content.put("type", "image");
        content.put("data", base64);
        content.put("mimeType", "image/png");
        ArrayNode arr = mapper.createArrayNode();
        arr.add(content);
        ObjectNode result = mapper.createObjectNode();
        result.set("content", arr);
        return result;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JsonNode textResult(String text) {
        ObjectNode content = mapper.createObjectNode();
        content.put("type", "text");
        content.put("text", text);
        ArrayNode arr = mapper.createArrayNode();
        arr.add(content);
        ObjectNode result = mapper.createObjectNode();
        result.set("content", arr);
        return result;
    }

    private static String requireString(JsonNode args, String field) {
        if (args == null || !args.has(field) || args.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required argument: " + field);
        }
        return args.get(field).asText();
    }
}
