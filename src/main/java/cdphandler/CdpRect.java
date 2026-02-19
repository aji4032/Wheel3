package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;

public record CdpRect(CdpPoint point, CdpDimension dimension) {
    public CdpRect(JsonNode json) {
        this(new CdpPoint(json.get("x").asInt(), json.get("y").asInt()),
                new CdpDimension(json.get("width").asInt(), json.get("height").asInt()));
    }
}
