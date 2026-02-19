package mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper to build JSON-RPC 2.0 success and error response nodes.
 */
public class McpResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Build a successful JSON-RPC 2.0 response. */
    public static ObjectNode success(Object id, Object result) {
        ObjectNode resp = MAPPER.createObjectNode();
        resp.put("jsonrpc", "2.0");
        if (id instanceof Integer)
            resp.put("id", (Integer) id);
        else if (id instanceof Long)
            resp.put("id", (Long) id);
        else if (id instanceof String)
            resp.put("id", (String) id);
        else
            resp.putNull("id");

        if (result instanceof String) {
            ObjectNode content = MAPPER.createObjectNode();
            content.put("type", "text");
            content.put("text", (String) result);
            resp.set("result", MAPPER.createObjectNode()
                    .set("content", MAPPER.createArrayNode().add(content)));
        } else if (result instanceof com.fasterxml.jackson.databind.JsonNode) {
            resp.set("result", (com.fasterxml.jackson.databind.JsonNode) result);
        } else {
            ObjectNode content = MAPPER.createObjectNode();
            content.put("type", "text");
            content.put("text", String.valueOf(result));
            resp.set("result", MAPPER.createObjectNode()
                    .set("content", MAPPER.createArrayNode().add(content)));
        }
        return resp;
    }

    /** Build a JSON-RPC 2.0 error response. */
    public static ObjectNode error(Object id, int code, String message) {
        ObjectNode resp = MAPPER.createObjectNode();
        resp.put("jsonrpc", "2.0");
        if (id instanceof Integer)
            resp.put("id", (Integer) id);
        else if (id instanceof Long)
            resp.put("id", (Long) id);
        else if (id instanceof String)
            resp.put("id", (String) id);
        else
            resp.putNull("id");

        ObjectNode err = MAPPER.createObjectNode();
        err.put("code", code);
        err.put("message", message);
        resp.set("error", err);
        return resp;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
