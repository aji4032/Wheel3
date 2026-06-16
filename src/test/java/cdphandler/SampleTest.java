package cdphandler;

import logger.Log;
import logger.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.DriverContext;
import tools.ScreenRecorder;

import java.io.File;

public class SampleTest {
    private static final Logger log = Log.getLogger(SampleTest.class);

    private static final String WS_URL = "ws://localhost:9999/devtools/page/59FD6274DC1F2E0A903AF3F77E161FC8";

    private ICdpDriver driver;

    @BeforeClass
    public void setUp() {
        // driver = CdpHandler.createDriver(WS_URL);
        driver = CdpDriver.launch();

        File traceZip = new File("target/traces/my_test_trace.zip");
        driver.startTracing(traceZip);

        DriverContext.setCurrentDriver(driver);
        log.info("Connected to existing Chrome at: {}", WS_URL);
    }

    @Test
    public void testNavigateToGoogle() {
        // Start recording
//        ScreenRecorder.startRecording(driver.getCdpUtility(), "testNavigateToGoogle");

        driver.get("https://www.amazon.in");
        driver.maximizeWindow();
        log.info("Page title: {}", driver.getTitle());

        ICdpElement searchBar = driver
                .findElement(CdpBy.ByCssSelector("Search Bar", "[name='field-keywords']"));
        searchBar.sendKeys("MacBook Pro");

        ICdpElement searchBtn = driver
                .findElement(CdpBy.ByCssSelector("Search Button", "#nav-search-submit-button"));
        searchBtn.click();

        // Wait for search results to load instead of using a hardcoded sleep.
        // The below line assumes your framework waits implicitly, or you can use your framework's explicit wait utility.
        driver.findElement(CdpBy.ByCssSelector("Search Results", "[data-component-type='s-search-result']"));

        // Stop recording and keep the video
        java.io.File recording = ScreenRecorder.stopRecording();
        if (recording != null) {
            log.info("Recording saved at: {}", recording.getAbsolutePath());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        driver.stopTracing();
        DriverContext.removeCurrentDriver();
        if (driver != null) {
            driver.close();
        }
    }
}
