package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;

public record CdpPoint(int x, int y) {

    public CdpPoint(JsonNode json) {
        this(json.get("x").asInt(), json.get("y").asInt());
    }
}
