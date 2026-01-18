package cdphandler;

import com.google.gson.JsonObject;

public class CdpRect {
    private final CdpPoint point;
    private final CdpDimension dimension;

    public CdpRect(CdpPoint point, CdpDimension dimension) {
        this.point = point;
        this.dimension = dimension;
    }

    public CdpRect(JsonObject jsonObject) {
        point = new CdpPoint(jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt());
        dimension = new CdpDimension(jsonObject.get("width").getAsInt(), jsonObject.get("height").getAsInt());
    }

    public CdpPoint getPoint() {
        return point;
    }

    public CdpDimension getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object object) {
        if(!(object instanceof CdpRect)) return false;
        return getPoint().equals(((CdpRect) object).getPoint()) &&
                getDimension().equals(((CdpRect) object).getDimension());
    }
}
