package cdphandler;

public enum MouseEvent {
    PRESSED("mousePressed"),
    RELEASED("mouseReleased"),
    MOVED("mouseMoved"),
    WHEEL("mouseWheel");

    private String eventType;
    MouseEvent(String eventType) {
        this.eventType = eventType;
    }

    public String toString() {
        return eventType;
    }
}