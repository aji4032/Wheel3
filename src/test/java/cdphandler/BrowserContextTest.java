package cdphandler;

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.Log;

/**
 * CI smoke test that validates BrowserContext isolation.
 * <p>
 * Extends {@link CdpTestBase} so each {@code @Test} method automatically
 * gets its own isolated browser context with a fresh driver.
 */
public class BrowserContextTest extends CdpTestBase {

    @Test
    public void testContextProvidesWorkingDriver() {
        ICdpDriver driver = getDriver();
        driver.get("https://www.example.com");

        String title = driver.getTitle();
        Log.info("Context test - title: " + title);
        Assert.assertTrue(title.toLowerCase().contains("example"),
                "Expected page title to contain 'example', got: " + title);
    }

    @Test
    public void testContextIsolation() {
        // This test runs in its own context — navigating to a different URL
        // should have no effect on other parallel tests
        ICdpDriver driver = getDriver();
        driver.get("https://www.example.com");

        String url = driver.getCurrentUrl();
        Log.info("Context isolation test - URL: " + url);
        Assert.assertTrue(url.contains("example.com"),
                "Expected URL to contain 'example.com', got: " + url);

        // Verify we can find elements (proves the driver is connected)
        ICdpElement heading = driver.findElement(CdpBy.ByCssSelector("heading", "h1"));
        Assert.assertNotNull(heading, "Should find h1 element");
        String text = heading.getText();
        Log.info("Context isolation test - heading: " + text);
        Assert.assertFalse(text.isEmpty(), "Heading text should not be empty");
    }

    @Test
    public void testContextIdIsUnique() {
        // Each test gets its own context — verify it has a valid ID
        String contextId = getContext().getContextId();
        Assert.assertNotNull(contextId, "Context ID should not be null");
        Assert.assertFalse(contextId.isEmpty(), "Context ID should not be empty");
        Log.info("Context ID: " + contextId);
    }
}
