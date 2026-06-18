package w3c;

import org.jspecify.annotations.NonNull;

public record W3CBy(String name, W3CLocatorType type, String locator) {

    public static W3CBy ByAutomationId(String name, String locator) {
        return new W3CBy(name, W3CLocatorType.AUTOMATION_ID, locator);
    }

    public static W3CBy ByClassName(String name, String locator) {
        return new W3CBy(name, W3CLocatorType.CLASS_NAME, locator);
    }

    public static W3CBy ByName(String name, String locator) {
        return new W3CBy(name, W3CLocatorType.NAME, locator);
    }

    public static W3CBy ByXPath(String name, String locator) {
        return new W3CBy(name, W3CLocatorType.XPATH, locator);
    }

    @Override
    public @NonNull String toString() {
        return String.format("{'name': '%s', 'type': '%s', 'locator': '%s'}", name, type, locator);
    }
}
