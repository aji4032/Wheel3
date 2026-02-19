package apachehttpclient;

import tools.JSONParser;

public record ResponseObject(String apiName, String status, String response) {
    public String getValue(String key) {
        return JSONParser.getAttributeValue(this.response, key);
    }

    public String getValueWithoutQuotes(String key) {
        return getValue(key).replace("\"", "");
    }
}
