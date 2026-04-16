package tools;

import cdphandler.CdpUtility;
import cdphandler.ICdpDriver;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;

/**
 * TestNG listener that drives the Extent Reports lifecycle and
 * CDP-based video recording.
 * <p>
 * Wire up via {@code @Listeners(ExtentTestNGListener.class)} on your test
 * class,
 * or declare it in {@code testng.xml}.
 *
 * <pre>
 * &lt;listeners&gt;
 *   &lt;listener class-name="tools.ExtentTestNGListener"/&gt;
 * &lt;/listeners&gt;
 * </pre>
 */
public class ExtentTestNGListener implements ITestListener, ISuiteListener {

    /** ThreadLocal holding the last recording file for the current test. */
    private static final ThreadLocal<File> lastRecording = new ThreadLocal<>();

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getTestClass().getRealClass().getSimpleName()
                + " — " + result.getName();
        ExtentManager.createTest(testName);

        // Start video recording if a CdpDriver is registered for this thread
        ICdpDriver driver = DriverContext.getCurrentDriver();
        if (driver != null && ScreenRecorder.isEnabled()) {
            try {
                CdpUtility cdp = driver.getCdpUtility();
                ScreenRecorder.startRecording(cdp, testName);
            } catch (Exception e) {
                Log.warn("Could not start recording: " + e.getMessage());
            }
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null)
            test.log(Status.PASS, "Test passed");

        // Stop and discard the recording on success
        File recording = ScreenRecorder.stopRecording();
        ScreenRecorder.deleteLastRecording(recording);
        lastRecording.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, result.getThrowable());

            // Stop recording and attach to the report
            File recording = ScreenRecorder.stopRecording();
            if (recording != null && recording.exists() && recording.length() > 0) {
                try {
                    test.info("Video recording: "
                            + "<a href='" + recording.getAbsolutePath() + "'>"
                            + recording.getName() + "</a>");
                    lastRecording.set(recording);
                } catch (Exception e) {
                    Log.warn("Failed to attach recording to report: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            Throwable cause = result.getThrowable();
            test.log(Status.SKIP, cause != null ? cause.getMessage() : "Skipped");
        }

        // Stop recording but keep the file
        File recording = ScreenRecorder.stopRecording();
        if (recording != null) {
            lastRecording.set(recording);
        }
    }

    // ── ISuiteListener ───────────────────────────────────────────────────────

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
    }
}
