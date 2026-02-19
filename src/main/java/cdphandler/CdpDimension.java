package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;

public record CdpDimension(int width, int height) {
    public CdpDimension(JsonNode json) {
        this(json.get("width").asInt(), json.get("height").asInt());
    }
}
