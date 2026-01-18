package cdphandler;

import com.google.gson.JsonObject;

import java.util.Objects;

public class CdpDimension {
    private final int width;
    private final int height;

    public CdpDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public CdpDimension(JsonObject jsonObject) {
        this.width = jsonObject.get("width").getAsInt();
        this.height = jsonObject.get("height").getAsInt();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object object) {
        if(!(object instanceof CdpDimension))
            return false;
        return getWidth() == ((CdpDimension) object).getWidth() && getHeight() == ((CdpDimension) object).getHeight();
    }

    @Override
    public String toString(){
        return String.format("Dimension{width=%s,height=%s}", getWidth(), getHeight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWidth(), getHeight());
    }
}