package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class JSONParser {
    private JSONParser() {
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String getAttributeValue(String json, String attributeKey) {
        try {
            if (attributeKey.contains(".")) {
                String[] splitAttribute = attributeKey.split("[.]", 2);
                return getAttributeValue(getAttributeValue(json, splitAttribute[0]), splitAttribute[1]);
            } else {
                JsonNode root = mapper.readTree(json);
                JsonNode value = root.get(attributeKey);
                return value != null ? value.toString() : "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static Set<String> getKeySet(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            Set<String> keys = new LinkedHashSet<>();
            Iterator<String> it = root.fieldNames();
            while (it.hasNext()) {
                keys.add(it.next());
            }
            return keys;
        } catch (Exception e) {
            return Set.of();
        }
    }

    public static ObjectNode getJSONObject(String json, String attributeKey) {
        try {
            if (attributeKey.contains(".")) {
                String[] splitAttribute = attributeKey.split("[.]", 2);
                return getJSONObject(getJSONObject(json, splitAttribute[0]).toString(), splitAttribute[1]);
            } else {
                JsonNode root = mapper.readTree(json);
                return (ObjectNode) root.get(attributeKey);
            }
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    public static String getJSONObject(File file, String attributeKey) {
        return getAttributeValue(getJSONData(file), attributeKey);
    }

    public static ArrayNode getJsonArray(String json) {
        try {
            return (ArrayNode) mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createArrayNode();
        }
    }

    public static ArrayNode getJsonArray(ObjectNode json, String attributeKey) {
        JsonNode node = json.get(attributeKey);
        if (node != null && node.isArray()) {
            return (ArrayNode) node;
        }
        return mapper.createArrayNode();
    }

    public static String getJSONData(File file) {
        StringBuilder objStringBuilder = new StringBuilder();
        try (FileReader objFileReader = new FileReader(file.getAbsolutePath())) {
            int i;
            while ((i = objFileReader.read()) != -1)
                objStringBuilder.append((char) i);
        } catch (Exception e) {
            Log.fail("Failed to get JSON data! - " + e.getMessage());
        }
        return objStringBuilder.toString();
    }
}