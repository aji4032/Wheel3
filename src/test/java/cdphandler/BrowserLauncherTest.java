package cdphandler;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.Log;
import tools.Logger;

/**
 * CI smoke test that launches a headless Chrome via {@link BrowserLauncher},
 * navigates to a public page, and asserts basic driver functionality.
 *
 * <p>
 * This test requires Chrome/Chromium to be installed (e.g. via
 * {@code browser-actions/setup-chrome} in CI) but does NOT require a
 * manually-started browser or a hardcoded WebSocket URL.
 */
public class BrowserLauncherTest {
    private static final Logger log = Log.getLogger(BrowserLauncherTest.class);

    private ICdpDriver driver;
    private BrowserLauncher.LaunchedBrowser browser;

    @BeforeClass
    public void setUp() {
        try {
            browser = BrowserLauncher.launch();
            String pageWsUrl = BrowserLauncher.getFirstPageWsUrl(browser.port());
            driver = CdpHandler.createDriver(pageWsUrl);
            log.info("Headless Chrome launched on port {}", browser.port());
        } catch (Exception e) {
            log.fail("Failed to launch headless Chrome", e);
        }
    }

    @Test
    public void testNavigateAndGetTitle() {
        driver.get("https://www.example.com");

        String title = driver.getTitle();
        log.info("Page title: {}", title);
        Assert.assertTrue(
                title.toLowerCase().contains("example"),
                "Expected page title to contain 'example', got: " + title);

        String url = driver.getCurrentUrl();
        log.info("Current URL: {}", url);
        Assert.assertTrue(
                url.contains("example.com"),
                "Expected URL to contain 'example.com', got: " + url);
    }

    @Test(dependsOnMethods = "testNavigateAndGetTitle")
    public void testFindElement() {
        // example.com has an <h1> element
        ICdpElement heading = driver.findElement(CdpBy.ByCssSelector("heading", "h1"));
        String text = heading.getText();
        log.info("Heading text: {}", text);
        Assert.assertTrue(
                text.toLowerCase().contains("example"),
                "Expected heading to contain 'example', got: " + text);
    }

    @Test(dependsOnMethods = "testNavigateAndGetTitle")
    public void testScreenshot() {
//        String base64 = driver.captureScreenshot();
//        Assert.assertNotNull(base64, "Screenshot should not be null");
//        Assert.assertFalse(base64.isEmpty(), "Screenshot should not be empty");
//        log.info("Screenshot captured (" + base64.length() + " chars base64)");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            try {
                driver.close();
                log.info("Driver closed");
            } catch (Exception ignored) {
            }
        }
        if (browser != null) {
            browser.close();
            log.info("Browser process terminated");
        }
    }
}
