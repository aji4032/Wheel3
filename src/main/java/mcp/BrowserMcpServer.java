package mcp;

import cdphandler.CdpHandler;
import cdphandler.ICdpDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tools.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * MCP (Model Context Protocol) stdio server that exposes the cdphandler
 * browser automation package as AI tool calls over JSON-RPC 2.0.
 *
 * Usage:
 * java -jar DesktopUiAutomation-...-jar-with-dependencies.jar
 * <webSocketDebuggerUrl>
 *
 * Example:
 * java -jar target/DesktopUiAutomation-1.0-SNAPSHOT-jar-with-dependencies.jar \
 * ws://localhost:9222/devtools/page/XXXXXXXX
 *
 * The server reads newline-delimited JSON-RPC 2.0 requests from stdin
 * and writes responses to stdout. Log messages go to stderr so they don't
 * corrupt the protocol stream.
 */
public class BrowserMcpServer {

    private static final String SERVER_NAME = "cdp-browser-server";
    private static final String SERVER_VERSION = "1.0.0";
    private static final ObjectMapper MAPPER = McpResponse.mapper();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: BrowserMcpServer <webSocketDebuggerUrl>");
            System.err.println("Example: ws://localhost:9222/devtools/page/XXXXXXXX");
            System.exit(1);
        }

        String wsUrl = args[0];
        Log.info("Connecting to browser at: " + wsUrl);

        ICdpDriver driver = CdpHandler.createDriver(wsUrl);
        McpToolDispatcher dispatcher = new McpToolDispatcher(driver);

        Log.info("MCP Browser Server started. Listening on stdin...");

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
        driver.close();
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

        addTool(tools, "navigate",
                "Navigate the browser to a URL",
                param("url", "string", "The URL to navigate to", true));

        addTool(tools, "click",
                "Click on a page element described in plain English",
                param("description", "string",
                        "Plain-English description of the element to click (e.g. 'search button', 'username field')",
                        true));

        addTool(tools, "type",
                "Type text into a page element described in plain English",
                param("description", "string", "Plain-English description of the element to type into", true),
                param("text", "string", "The text to type", true));

        addTool(tools, "get_text",
                "Get the visible text content of a page element described in plain English",
                param("description", "string", "Plain-English description of the element", true));

        addTool(tools, "get_page_title",
                "Get the current page title", new ObjectNode[0]);

        addTool(tools, "get_current_url",
                "Get the current page URL", new ObjectNode[0]);

        addTool(tools, "get_page_source",
                "Get the full HTML source of the current page", new ObjectNode[0]);

        addTool(tools, "take_screenshot",
                "Capture a screenshot of the current page (returns base64 PNG)", new ObjectNode[0]);

        addTool(tools, "go_back",
                "Navigate back in browser history", new ObjectNode[0]);

        addTool(tools, "go_forward",
                "Navigate forward in browser history", new ObjectNode[0]);

        addTool(tools, "refresh",
                "Refresh the current page", new ObjectNode[0]);

        addTool(tools, "press_key",
                "Press a keyboard key by name",
                param("key", "string", "Key name e.g. Enter, Tab, Escape, Space, ArrowDown, Backspace, Delete, F5",
                        true));

        addTool(tools, "execute_action",
                "Execute a high-level natural-language browser instruction (Ollama plans and executes all required steps)",
                param("instruction", "string",
                        "A plain-English instruction describing what to do, e.g. 'log in with username admin and password 123'",
                        true));

        ObjectNode result = MAPPER.createObjectNode();
        result.set("tools", tools);

        ObjectNode resp = MAPPER.createObjectNode();
        resp.put("jsonrpc", "2.0");
        putId(resp, id);
        resp.set("result", result);
        return resp;
    }

    private static ObjectNode handleToolsCall(Object id, JsonNode params, McpToolDispatcher dispatcher) {
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
            p.remove("_name");
            p.remove("_required");
            properties.set(pName, p);
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
