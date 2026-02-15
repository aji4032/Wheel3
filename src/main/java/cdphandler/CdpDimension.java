package cdphandler;

import com.google.gson.JsonObject;

public record CdpDimension(int width, int height) {
    public CdpDimension(JsonObject jsonObject) {
        this(jsonObject.get("width").getAsInt(), jsonObject.get("height").getAsInt());
    }
}
