package cdphandler;

public record CdpBy(String name, CdpLocatorType type, String locator) {

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
