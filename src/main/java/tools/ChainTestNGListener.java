package tools;

import cdphandler.CdpUtility;
import cdphandler.ICdpDriver;
import com.aventstack.chaintest.plugins.ChainTestListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * TestNG listener that drives CDP-based video recording and
 * attaches media to the ChainTest report on failure.
 * <p>
 * Wire up via the SPI file
 * {@code META-INF/services/org.testng.ITestNGListener},
 * or declare it in {@code testng.xml}:
 *
 * <pre>
 * &lt;listeners&gt;
 *   &lt;listener class-name="tools.ChainTestNGListener"/&gt;
 * &lt;/listeners&gt;
 * </pre>
 */
public class ChainTestNGListener implements ITestListener {

    private static final Logger log = Log.getLogger(ChainTestNGListener.class);

    /** ThreadLocal holding the last recording file for the current test. */
    private static final ThreadLocal<File> lastRecording = new ThreadLocal<>();

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        // Start video recording if a CdpDriver is registered for this thread
        ICdpDriver driver = DriverContext.getCurrentDriver();
        if (driver != null && ScreenRecorder.isEnabled()) {
            try {
                String testName = result.getTestClass().getRealClass().getSimpleName()
                        + " — " + result.getName();
                CdpUtility cdp = driver.getCdpUtility();
                ScreenRecorder.startRecording(cdp, testName);
            } catch (Exception e) {
                log.warn("Could not start recording: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Stop and discard the recording on success
        File recording = ScreenRecorder.stopRecording();
        ScreenRecorder.deleteLastRecording(recording);
        lastRecording.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Stop recording and attach to the ChainTest report
        File recording = ScreenRecorder.stopRecording();
        if (recording != null && recording.exists() && recording.length() > 0) {
            try {
                byte[] videoBytes = Files.readAllBytes(recording.toPath());
                ChainTestListener.embed(videoBytes, "video/avi");
                lastRecording.set(recording);
            } catch (IOException e) {
                log.warn("Failed to attach recording to report: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // Stop recording but keep the file
        File recording = ScreenRecorder.stopRecording();
        if (recording != null) {
            lastRecording.set(recording);
        }
    }
}
