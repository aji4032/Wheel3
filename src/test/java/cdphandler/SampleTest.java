package cdphandler;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.Log;

public class SampleTest {

    private ICdpDriver driver;

    @BeforeClass
    public void setUp() {
        String ws = "ws://localhost:59251/devtools/page/05986F850454E700508B07A0F73915AD";
        try {
            driver = CdpHandler.createDriver(ws);
            Log.info("WebSocket driver initialized: " + ws);
        } catch (Exception e) {
            Log.fail("Failed to initialize driver", e);
        }
    }

    @Test
    public void testClickImFeelingLucky() {
        try {
            driver.get("https://www.facebook.com");
            Log.info("Navigated to Google");

            // ICdpElement searchBox = driver.findElement(
            // new CdpBy("im feeling lucky submit", CdpLocatorType.NATURAL_LANGUAGE, "I'm
            // Feeling Lucky"));
            // searchBox.click();
            Log.info("Clicked 'I'm Feeling Lucky' button");

            driver.keyPress(CdpKey.Enter);
            Log.info("Pressed Enter");
        } catch (AssertionError e) {
            throw e; // re-throw TestNG assertion failures as-is
        } catch (Exception e) {
            Log.fail("Test failed with unexpected exception", e);
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.close();
            Log.info("Driver closed");
        }
    }
}
