package cdphandler;

import com.google.gson.JsonObject;

import java.util.Objects;

public class CdpPoint {
    private final int x;
    private final int y;

    public CdpPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public CdpPoint(JsonObject jsonObject) {
        this.x = jsonObject.get("x").getAsInt();
        this.y = jsonObject.get("y").getAsInt();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object object) {
        if(!(object instanceof CdpPoint))
            return false;
        return getX() == ((CdpPoint) object).getX() && getY() == ((CdpPoint) object).getY();
    }

    @Override
    public String toString(){
        return String.format("Point{x=%s,y=%s}", getX(), getY());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }
}
