package cdphandler;

public class CdpBy {
    private String name;
    private CdpLocatorType type;
    private String locator;

    private CdpBy() {}

    public CdpBy(String name, CdpLocatorType type, String locator) {
        this.name = name;
        this.type = type;
        this.locator = locator;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setType(CdpLocatorType type) {
        this.type = type;
    }
    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getName() {
        return name;
    }
    public CdpLocatorType getType() {
        return type;
    }
    public String getLocator() {
        return locator;
    }

    public static CdpBy ById(String name, String id) {
        return new CdpBy(name, CdpLocatorType.ID, id);
    }
    public static CdpBy ByCssSelector(String name, String cssSelector) {
        return new CdpBy(name, CdpLocatorType.CSS, cssSelector);
    }
    public static CdpBy ByXPath(String name, String xpath) {
        return new CdpBy(name, CdpLocatorType.XPATH, xpath);
    }

    @Override
    public String toString() {
        return String.format("{'name': '%s', 'type': '%s', 'locator': '%s'}", name, type, locator);
    }
}
