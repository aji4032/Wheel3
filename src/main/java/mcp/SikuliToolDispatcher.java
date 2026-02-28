package mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sikuli.script.*;
import sikuli.SikuliActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

/**
 * Dispatches MCP tools/call requests to the {@link SikuliActions} singleton.
 *
 * Target parameters use the PFRML convention (Pattern, Filename, Text, Region,
 * Location) and are resolved from a JSON object with a {@code "type"} field.
 */
public class SikuliToolDispatcher {

    private final SikuliActions actions;
    private final ObjectMapper mapper = McpResponse.mapper();

    public SikuliToolDispatcher(SikuliActions actions) {
        this.actions = actions;
    }

    /**
     * Dispatch a tools/call request and return a result JSON node.
     */
    public JsonNode dispatch(String toolName, JsonNode args) {
        return switch (toolName) {
            case "click" -> doClick(args);
            case "double_click" -> doDoubleClick(args);
            case "hover" -> doHover(args);
            case "drag_drop" -> doDragDrop(args);
            case "type_text" -> doTypeText(args);
            case "scroll" -> doScroll(args);
            case "exists" -> doExists(args);
            case "wait_vanish" -> doWaitVanish(args);
            case "capture_screenshot" -> doCaptureScreenshot();
            case "set_result_dir" -> doSetResultDir(args);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    // -----------------------------------------------------------------------
    // Tool implementations
    // -----------------------------------------------------------------------

    private JsonNode doClick(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        try {
            actions.click(target);
            return textResult("clicked target: " + describeTarget(args.get("target")));
        } catch (FindFailed e) {
            return textResult("click failed: " + e.getMessage());
        }
    }

    private JsonNode doDoubleClick(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        try {
            actions.doubleClick(target);
            return textResult("double-clicked target: " + describeTarget(args.get("target")));
        } catch (FindFailed e) {
            return textResult("double-click failed: " + e.getMessage());
        }
    }

    private JsonNode doHover(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        try {
            actions.hover(target);
            return textResult("hovering over target: " + describeTarget(args.get("target")));
        } catch (FindFailed e) {
            return textResult("hover failed: " + e.getMessage());
        }
    }

    private JsonNode doDragDrop(JsonNode args) {
        Object source = resolveTarget(args.get("source"));
        Object destination = resolveTarget(args.get("destination"));
        try {
            actions.dragDrop(source, destination);
            return textResult("dragged from " + describeTarget(args.get("source"))
                    + " to " + describeTarget(args.get("destination")));
        } catch (FindFailed e) {
            return textResult("drag-drop failed: " + e.getMessage());
        }
    }

    private JsonNode doTypeText(JsonNode args) {
        String text = requireString(args, "text");
        actions.type(text);
        return textResult("typed text: \"" + text + "\"");
    }

    private JsonNode doScroll(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        String direction = requireString(args, "direction");
        int steps = args.has("steps") ? args.get("steps").asInt(3) : 3;
        int wheelDirection = direction.equalsIgnoreCase("up")
                ? Mouse.WHEEL_UP
                : Mouse.WHEEL_DOWN;
        try {
            actions.wheel(target, wheelDirection, steps);
            return textResult("scrolled " + direction + " " + steps + " steps on target: "
                    + describeTarget(args.get("target")));
        } catch (FindFailed e) {
            return textResult("scroll failed: " + e.getMessage());
        }
    }

    private JsonNode doExists(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        double timeout = args.has("timeout") ? args.get("timeout").asDouble(5.0) : 5.0;
        Match match = actions.exists(target, timeout);
        if (match != null) {
            ObjectNode result = mapper.createObjectNode();
            result.put("found", true);
            result.put("x", match.getX());
            result.put("y", match.getY());
            result.put("width", match.getW());
            result.put("height", match.getH());
            result.put("score", match.getScore());
            return textResult("target found: " + describeTarget(args.get("target"))
                    + " at (" + match.getX() + ", " + match.getY() + ")"
                    + " score=" + String.format("%.2f", match.getScore()));
        } else {
            return textResult("target not found: " + describeTarget(args.get("target")));
        }
    }

    private JsonNode doWaitVanish(JsonNode args) {
        Object target = resolveTarget(args.get("target"));
        double timeout = args.has("timeout") ? args.get("timeout").asDouble(10.0) : 10.0;
        boolean vanished = actions.waitVanish(target, timeout);
        return textResult(vanished
                ? "target vanished: " + describeTarget(args.get("target"))
                : "target still visible after " + timeout + "s: " + describeTarget(args.get("target")));
    }

    private JsonNode doCaptureScreenshot() {
        try {
            ScreenImage img = actions.capture();
            BufferedImage buffered = img.getImage();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            ObjectNode content = mapper.createObjectNode();
            content.put("type", "image");
            content.put("data", base64);
            content.put("mimeType", "image/png");
            ArrayNode arr = mapper.createArrayNode();
            arr.add(content);
            ObjectNode result = mapper.createObjectNode();
            result.set("content", arr);
            return result;
        } catch (Exception e) {
            return textResult("screenshot failed: " + e.getMessage());
        }
    }

    private JsonNode doSetResultDir(JsonNode args) {
        String path = requireString(args, "path");
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        actions.setResultLocation(dir);
        return textResult("result directory set to: " + dir.getAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // PFRML Target Resolution
    // -----------------------------------------------------------------------

    /**
     * Resolves a JSON target node into a SikuliX-compatible object.
     * Supports types: filename, pattern, text, region, location.
     */
    private Object resolveTarget(JsonNode targetNode) {
        if (targetNode == null || targetNode.isNull()) {
            throw new IllegalArgumentException("Missing required 'target' parameter");
        }

        // Simple string shorthand — treat as filename
        if (targetNode.isTextual()) {
            return targetNode.asText();
        }

        String type = targetNode.has("type") ? targetNode.get("type").asText() : "filename";

        return switch (type) {
            case "filename" -> targetNode.get("value").asText();
            case "pattern" -> {
                String imageName = targetNode.get("imageName").asText();
                double similarity = targetNode.has("similarity")
                        ? targetNode.get("similarity").asDouble(0.85)
                        : 0.85;
                int xOffset = targetNode.has("xOffset") ? targetNode.get("xOffset").asInt(0) : 0;
                int yOffset = targetNode.has("yOffset") ? targetNode.get("yOffset").asInt(0) : 0;
                yield new Pattern(imageName).similar(similarity).targetOffset(xOffset, yOffset);
            }
            case "text" -> targetNode.get("value").asText();
            case "region" -> new Region(
                    targetNode.get("x").asInt(),
                    targetNode.get("y").asInt(),
                    targetNode.get("w").asInt(),
                    targetNode.get("h").asInt());
            case "location" -> new Location(
                    targetNode.get("x").asInt(),
                    targetNode.get("y").asInt());
            default -> throw new IllegalArgumentException("Unknown target type: " + type);
        };
    }

    /**
     * Returns a human-readable description of the target for log messages.
     */
    private String describeTarget(JsonNode targetNode) {
        if (targetNode == null)
            return "null";
        if (targetNode.isTextual())
            return targetNode.asText();
        String type = targetNode.has("type") ? targetNode.get("type").asText() : "unknown";
        return switch (type) {
            case "filename" -> targetNode.get("value").asText();
            case "pattern" -> targetNode.get("imageName").asText()
                    + " (similarity=" + targetNode.path("similarity").asDouble(0.85) + ")";
            case "text" -> "text(\"" + targetNode.get("value").asText() + "\")";
            case "region" -> "region(" + targetNode.get("x") + "," + targetNode.get("y")
                    + "," + targetNode.get("w") + "," + targetNode.get("h") + ")";
            case "location" -> "location(" + targetNode.get("x") + "," + targetNode.get("y") + ")";
            default -> targetNode.toString();
        };
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
