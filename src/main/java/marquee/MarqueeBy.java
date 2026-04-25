package marquee;

import org.jspecify.annotations.NonNull;

public record MarqueeBy(String name, MarqueeLocatorType type, String locator) {

    public static MarqueeBy ByAutomationId(String name, String locator) {
        return new MarqueeBy(name, MarqueeLocatorType.AUTOMATION_ID, locator);
    }

    public static MarqueeBy ByClassName(String name, String locator) {
        return new MarqueeBy(name, MarqueeLocatorType.CLASS_NAME, locator);
    }

    public static MarqueeBy ByName(String name, String locator) {
        return new MarqueeBy(name, MarqueeLocatorType.NAME, locator);
    }

    @Override
    public @NonNull String toString() {
        return String.format("{'name': '%s', 'type': '%s', 'locator': '%s'}", name, type, locator);
    }
}
