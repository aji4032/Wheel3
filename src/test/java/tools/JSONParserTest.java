package tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * Unit tests for the {@link JSONParser} utility class.
 */
public class JSONParserTest {

    // -----------------------------------------------------------------------
    // getAttributeValue — simple keys
    // -----------------------------------------------------------------------

    @Test
    public void testGetAttributeValueSimple() {
        String json = """
                {"name": "Alice", "age": 30}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "name"), "\"Alice\"");
        Assert.assertEquals(JSONParser.getAttributeValue(json, "age"), "30");
    }

    @Test
    public void testGetAttributeValueMissing() {
        String json = """
                {"name": "Alice"}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "missing"), "");
    }

    @Test
    public void testGetAttributeValueInvalidJson() {
        Assert.assertEquals(JSONParser.getAttributeValue("not json", "key"), "");
    }

    @Test
    public void testGetAttributeValueEmptyJson() {
        Assert.assertEquals(JSONParser.getAttributeValue("{}", "key"), "");
    }

    // -----------------------------------------------------------------------
    // getAttributeValue — nested (dot notation)
    // -----------------------------------------------------------------------

    @Test
    public void testGetAttributeValueNested() {
        String json = """
                {"user": {"name": "Bob", "address": {"city": "NYC"}}}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "user.name"), "\"Bob\"");
    }

    @Test
    public void testGetAttributeValueDeepNested() {
        String json = """
                {"a": {"b": {"c": "deep"}}}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "a.b.c"), "\"deep\"");
    }

    @Test
    public void testGetAttributeValueNestedMissing() {
        String json = """
                {"user": {"name": "Bob"}}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "user.age"), "");
    }

    // -----------------------------------------------------------------------
    // getKeySet
    // -----------------------------------------------------------------------

    @Test
    public void testGetKeySet() {
        String json = """
                {"alpha": 1, "beta": 2, "gamma": 3}""";
        Set<String> keys = JSONParser.getKeySet(json);
        Assert.assertEquals(keys.size(), 3);
        Assert.assertTrue(keys.contains("alpha"));
        Assert.assertTrue(keys.contains("beta"));
        Assert.assertTrue(keys.contains("gamma"));
    }

    @Test
    public void testGetKeySetEmpty() {
        Assert.assertTrue(JSONParser.getKeySet("{}").isEmpty());
    }

    @Test
    public void testGetKeySetInvalidJson() {
        Assert.assertTrue(JSONParser.getKeySet("bad json").isEmpty());
    }

    @Test
    public void testGetKeySetPreservesOrder() {
        String json = """
                {"z": 1, "a": 2, "m": 3}""";
        Set<String> keys = JSONParser.getKeySet(json);
        // LinkedHashSet should preserve insertion order
        String[] arr = keys.toArray(new String[0]);
        Assert.assertEquals(arr[0], "z");
        Assert.assertEquals(arr[1], "a");
        Assert.assertEquals(arr[2], "m");
    }

    // -----------------------------------------------------------------------
    // getJSONObject (String, String)
    // -----------------------------------------------------------------------

    @Test
    public void testGetJSONObject() {
        String json = """
                {"config": {"timeout": 30, "retries": 3}}""";
        ObjectNode obj = JSONParser.getJSONObject(json, "config");
        Assert.assertEquals(obj.get("timeout").asInt(), 30);
        Assert.assertEquals(obj.get("retries").asInt(), 3);
    }

    @Test
    public void testGetJSONObjectNested() {
        String json = """
                {"data": {"inner": {"value": "found"}}}""";
        ObjectNode obj = JSONParser.getJSONObject(json, "data.inner");
        Assert.assertEquals(obj.get("value").asText(), "found");
    }

    @Test
    public void testGetJSONObjectMissingReturnsEmpty() {
        String json = """
                {"data": "string"}""";
        ObjectNode obj = JSONParser.getJSONObject(json, "missing");
        Assert.assertNotNull(obj, "Should return empty ObjectNode, not null");
        Assert.assertEquals(obj.size(), 0);
    }

    // -----------------------------------------------------------------------
    // getJsonArray (String)
    // -----------------------------------------------------------------------

    @Test
    public void testGetJsonArrayFromString() {
        String json = """
                [1, 2, 3]""";
        ArrayNode arr = JSONParser.getJsonArray(json);
        Assert.assertEquals(arr.size(), 3);
        Assert.assertEquals(arr.get(0).asInt(), 1);
        Assert.assertEquals(arr.get(2).asInt(), 3);
    }

    @Test
    public void testGetJsonArrayInvalidReturnsEmpty() {
        ArrayNode arr = JSONParser.getJsonArray("not an array");
        Assert.assertNotNull(arr);
        Assert.assertEquals(arr.size(), 0);
    }

    @Test
    public void testGetJsonArrayOfObjects() {
        String json = """
                [{"id": 1}, {"id": 2}]""";
        ArrayNode arr = JSONParser.getJsonArray(json);
        Assert.assertEquals(arr.size(), 2);
        Assert.assertEquals(arr.get(0).get("id").asInt(), 1);
    }

    // -----------------------------------------------------------------------
    // getJsonArray (ObjectNode, String)
    // -----------------------------------------------------------------------

    @Test
    public void testGetJsonArrayFromObjectNode() {
        String json = "{\"items\": [10, 20], \"name\": \"test\"}";
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            ArrayNode arr = JSONParser.getJsonArray(node, "items");
            Assert.assertEquals(arr.size(), 2);
            Assert.assertEquals(arr.get(0).asInt(), 10);
        } catch (Exception e) {
            Assert.fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    public void testGetJsonArrayFromObjectNodeMissing() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree("""
                    {"name": "test"}""");
            ArrayNode arr = JSONParser.getJsonArray(node, "items");
            Assert.assertNotNull(arr);
            Assert.assertEquals(arr.size(), 0);
        } catch (Exception e) {
            Assert.fail("Should not throw: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // getJSONData and getJSONObject (File, String)
    // -----------------------------------------------------------------------

    @Test
    public void testGetJSONDataFromFile() throws IOException {
        File tempFile = File.createTempFile("jsonparser-test-", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("""
                    {"greeting": "hello", "count": 5}""");
        }

        String data = JSONParser.getJSONData(tempFile);
        Assert.assertTrue(data.contains("greeting"));
        Assert.assertTrue(data.contains("hello"));
    }

    @Test
    public void testGetJSONObjectFromFile() throws IOException {
        File tempFile = File.createTempFile("jsonparser-test-", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("""
                    {"settings": {"theme": "dark"}}""");
        }

        String result = JSONParser.getJSONObject(tempFile, "settings");
        Assert.assertTrue(result.contains("dark"));
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testGetAttributeValueWithArray() {
        String json = """
                {"tags": ["a", "b"]}""";
        String result = JSONParser.getAttributeValue(json, "tags");
        Assert.assertTrue(result.contains("a"));
        Assert.assertTrue(result.contains("b"));
    }

    @Test
    public void testGetAttributeValueBoolean() {
        String json = """
                {"active": true}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "active"), "true");
    }

    @Test
    public void testGetAttributeValueNull() {
        String json = """
                {"value": null}""";
        Assert.assertEquals(JSONParser.getAttributeValue(json, "value"), "null");
    }
}
