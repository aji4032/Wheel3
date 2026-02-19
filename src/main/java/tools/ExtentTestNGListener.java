package tools;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that drives the Extent Reports lifecycle.
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

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getTestClass().getRealClass().getSimpleName()
                + " — " + result.getName();
        ExtentManager.createTest(testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null)
            test.log(Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            Throwable cause = result.getThrowable();
            test.log(Status.SKIP, cause != null ? cause.getMessage() : "Skipped");
        }
    }

    // ── ISuiteListener ───────────────────────────────────────────────────────

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
    }
}
