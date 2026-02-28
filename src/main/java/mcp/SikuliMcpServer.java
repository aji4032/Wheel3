package mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sikuli.script.ImagePath;
import sikuli.SikuliActions;
import tools.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * MCP (Model Context Protocol) stdio server that exposes the Sikuli
 * desktop-automation package as AI tool calls over JSON-RPC 2.0.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 *   java -cp ... mcp.SikuliMcpServer [--imagePath &lt;dir&gt;] [--resultDir &lt;dir&gt;]
 * </pre>
 *
 * <p>
 * The server reads newline-delimited JSON-RPC 2.0 requests from stdin
 * and writes responses to stdout. Log messages go to stderr so they don't
 * corrupt the protocol stream.
 */
public class SikuliMcpServer {

    private static final String SERVER_NAME = "sikuli-desktop-server";
    private static final String SERVER_VERSION = "1.0.0";
    private static final ObjectMapper MAPPER = McpResponse.mapper();

    public static void main(String[] args) throws Exception {
        // Parse optional CLI arguments
        String imagePath = null;
        String resultDir = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--imagePath" -> {
                    if (i + 1 < args.length)
                        imagePath = args[++i];
                }
                case "--resultDir" -> {
                    if (i + 1 < args.length)
                        resultDir = args[++i];
                }
            }
        }

        // Set up Sikuli
        SikuliActions sikuliActions = SikuliActions.getSikuliActions();

        if (imagePath != null) {
            ImagePath.add(imagePath);
            Log.info("Sikuli ImagePath set to: " + imagePath);
        }

        if (resultDir != null) {
            File dir = new File(resultDir);
            dir.mkdirs();
            sikuliActions.setResultLocation(dir);
            Log.info("Result directory set to: " + resultDir);
        }

        SikuliToolDispatcher dispatcher = new SikuliToolDispatcher(sikuliActions);

        Log.info("MCP Sikuli Server started. Listening on stdin...");

        // Use stdout for protocol, stderr for logs
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            ObjectNode response;
            Object id = null;
            try {
                JsonNode request = MAPPER.readTree(line);
                id = extractId(request);
                String method = request.has("method") ? request.get("method").asText() : "";
                JsonNode params = request.has("params") ? request.get("params") : MAPPER.createObjectNode();

                response = switch (method) {
                    case "initialize" -> handleInitialize(id, params);
                    case "notifications/initialized" -> null; // no response needed
                    case "tools/list" -> handleToolsList(id);
                    case "tools/call" -> handleToolsCall(id, params, dispatcher);
                    case "ping" -> McpResponse.success(id, "pong");
                    default -> McpResponse.error(id, -32601, "Method not found: " + method);
                };
            } catch (Exception e) {
                Log.warn("Error processing request: " + e.getMessage());
                response = McpResponse.error(id, -32603, "Internal error: " + e.getMessage());
            }

            if (response != null) {
                out.println(MAPPER.writeValueAsString(response));
            }
        }

        Log.info("stdin closed — shutting down.");
    }

    // -----------------------------------------------------------------------
    // MCP lifecycle handlers
    // -----------------------------------------------------------------------

    private static ObjectNode handleInitialize(Object id, JsonNode params) {
        ObjectNode capabilities = MAPPER.createObjectNode();
        capabilities.set("tools", MAPPER.createObjectNode());

        ObjectNode serverInfo = MAPPER.createObjectNode();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);

        ObjectNode result = MAPPER.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        result.set("capabilities", capabilities);
        result.set("serverInfo", serverInfo);

        ObjectNode resp = MAPPER.createObjectNode();
        resp.put("jsonrpc", "2.0");
        putId(resp, id);
        resp.set("result", result);
        return resp;
    }

    private static ObjectNode handleToolsList(Object id) {
        ArrayNode tools = MAPPER.createArrayNode();

        // --- PFRML target schema (reused across tools) ---
        ObjectNode targetParam = targetParamDef("target",
                "Target to act on. Can be a simple string (image filename) or an object with 'type' field: "
                        + "filename, pattern, text, region, or location",
                true);

        // click
        addTool(tools, "click",
                "Click on a screen element matching the target (PFRML)",
                targetParam);

        // double_click
        addTool(tools, "double_click",
                "Double-click on a screen element matching the target (PFRML)",
                targetParamDef("target", "Target to double-click (PFRML)", true));

        // hover
        addTool(tools, "hover",
                "Hover the mouse over a screen element matching the target (PFRML)",
                targetParamDef("target", "Target to hover over (PFRML)", true));

        // drag_drop
        addTool(tools, "drag_drop",
                "Drag from source to destination (both PFRML targets)",
                targetParamDef("source", "Source target to drag from (PFRML)", true),
                targetParamDef("destination", "Destination target to drop onto (PFRML)", true));

        // type_text
        addTool(tools, "type_text",
                "Type text at the current cursor position",
                param("text", "string", "The text to type", true));

        // scroll
        addTool(tools, "scroll",
                "Scroll on a screen element matching the target",
                targetParamDef("target", "Target to scroll on (PFRML)", true),
                param("direction", "string", "Scroll direction: 'up' or 'down'", true),
                param("steps", "integer", "Number of scroll steps (default 3)", false));

        // exists
        addTool(tools, "exists",
                "Check if a target exists on screen within a timeout",
                targetParamDef("target", "Target to search for (PFRML)", true),
                param("timeout", "number", "Timeout in seconds (default 5)", false));

        // wait_vanish
        addTool(tools, "wait_vanish",
                "Wait for a target to disappear from the screen",
                targetParamDef("target", "Target to wait for disappearance (PFRML)", true),
                param("timeout", "number", "Timeout in seconds (default 10)", false));

        // capture_screenshot
        addTool(tools, "capture_screenshot",
                "Capture the current screen as a base64 PNG image",
                new ObjectNode[0]);

        // set_result_dir
        addTool(tools, "set_result_dir",
                "Set the directory for saving screenshots and result artifacts",
                param("path", "string", "Absolute path to the results directory", true));

        ObjectNode result = MAPPER.createObjectNode();
        result.set("tools", tools);

        ObjectNode resp = MAPPER.createObjectNode();
        resp.put("jsonrpc", "2.0");
        putId(resp, id);
        resp.set("result", result);
        return resp;
    }

    private static ObjectNode handleToolsCall(Object id, JsonNode params, SikuliToolDispatcher dispatcher) {
        String name = params.has("name") ? params.get("name").asText() : "";
        JsonNode args = params.has("arguments") ? params.get("arguments") : MAPPER.createObjectNode();

        try {
            JsonNode result = dispatcher.dispatch(name, args);
            ObjectNode resp = MAPPER.createObjectNode();
            resp.put("jsonrpc", "2.0");
            putId(resp, id);
            resp.set("result", result);
            return resp;
        } catch (IllegalArgumentException e) {
            return McpResponse.error(id, -32602, e.getMessage());
        } catch (Exception e) {
            Log.warn("Tool error [" + name + "]: " + e.getMessage());
            return McpResponse.error(id, -32603, "Tool execution failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a tool parameter definition for a PFRML target.
     * The schema accepts either a simple string or a structured object.
     */
    private static ObjectNode targetParamDef(String name, String description, boolean required) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("_name", name);
        p.put("_required", required);
        p.put("description", description);
        // Use an open schema that accepts string or object
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode().put("type", "string"));
        oneOf.add(MAPPER.createObjectNode().put("type", "object"));
        p.set("oneOf", oneOf);
        return p;
    }

    private static void addTool(ArrayNode tools, String name, String description, ObjectNode... paramDefs) {
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);

        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = MAPPER.createObjectNode();
        ArrayNode required = MAPPER.createArrayNode();

        for (ObjectNode p : paramDefs) {
            String pName = p.get("_name").asText();
            boolean pRequired = p.get("_required").asBoolean();
            ObjectNode pCopy = p.deepCopy();
            pCopy.remove("_name");
            pCopy.remove("_required");
            properties.set(pName, pCopy);
            if (pRequired)
                required.add(pName);
        }

        schema.set("properties", properties);
        if (required.size() > 0)
            schema.set("required", required);

        tool.set("inputSchema", schema);
        tools.add(tool);
    }

    private static ObjectNode param(String name, String type, String description, boolean required) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("_name", name);
        p.put("_required", required);
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    private static Object extractId(JsonNode request) {
        if (!request.has("id") || request.get("id").isNull())
            return null;
        JsonNode idNode = request.get("id");
        if (idNode.isInt() || idNode.isLong())
            return idNode.longValue();
        return idNode.asText();
    }

    private static void putId(ObjectNode resp, Object id) {
        if (id == null)
            resp.putNull("id");
        else if (id instanceof Long)
            resp.put("id", (Long) id);
        else if (id instanceof Integer)
            resp.put("id", (Integer) id);
        else
            resp.put("id", id.toString());
    }
}
