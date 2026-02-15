package cdphandler;

import com.google.gson.JsonObject;

public record CdpRect(CdpPoint point, CdpDimension dimension) {
    public CdpRect(JsonObject jsonObject) {
        this(new CdpPoint(jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt()),
             new CdpDimension(jsonObject.get("width").getAsInt(), jsonObject.get("height").getAsInt()));
    }
}
