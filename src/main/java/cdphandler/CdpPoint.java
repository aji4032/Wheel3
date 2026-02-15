package cdphandler;

import com.google.gson.JsonObject;

 public record CdpPoint(int x, int y) {

    public CdpPoint(JsonObject jsonObject) {
        this(jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt());
    }
}
