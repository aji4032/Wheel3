package cdphandler;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tools.DriverContext;
import tools.Log;
import tools.ScreenRecorder;

import java.time.Duration;

public class SampleTest {

    private static final String WS_URL = "ws://localhost:9999/devtools/page/59FD6274DC1F2E0A903AF3F77E161FC8";

    private ICdpDriver driver;

    @BeforeClass
    public void setUp() {
        driver = CdpHandler.createDriver(WS_URL);
        DriverContext.setCurrentDriver(driver);
        Log.info("Connected to existing Chrome at: " + WS_URL);
    }

    @Test
    public void testNavigateToGoogle() {
        // Start recording
        ScreenRecorder.startRecording(driver.getCdpUtility(), "testNavigateToGoogle");

        driver.get("https://www.google.com");
        driver.fullScreenWindow();
        Log.info("Page title: " + driver.getTitle());
        // driver.findElement(CdpBy.ByXPath("Search Bar",
        // "//input[@name='q']")).sendKeys("Hello World");
        driver.findElement(CdpBy.ByCssSelector("Search Button", ".FPdoLc.lJ9FBc [value=\"I'm Feeling Lucky\"]"))
                .click();

        // Wait 10 seconds
        driver.sleep(Duration.ofSeconds(10));

        // Stop recording and keep the video
        java.io.File recording = ScreenRecorder.stopRecording();
        if (recording != null) {
            Log.info("Recording saved at: " + recording.getAbsolutePath());
        }
    }

    @AfterClass
    public void tearDown() {
        DriverContext.removeCurrentDriver();
        if (driver != null) {
            driver.close();
        }
    }
}
