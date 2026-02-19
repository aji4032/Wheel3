package tools;

import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.Reporter;

/**
 * Unified logging and assertion utility.
 * <p>
 * This is the ONLY class test authors should use for logging and assertions.
 * It internally routes to Log4J for all log levels, writes to the TestNG
 * HTML report via {@link Reporter}, writes to the Extent HTML report via
 * {@link ExtentManager}, and triggers a TestNG assertion failure on
 * {@link #fail}.
 * 
 * <pre>
 * Log.info("Navigated to login page"); // informational
 * Log.warn("Slow response detected"); // non-fatal warning
 * Log.fail("Login button not found"); // logs ERROR + fails the test
 * </pre>
 */
public class Log {

    private Log() {
    }

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /** Returns a logger named after the direct caller's class. */
    private static Logger logger() {
        String callerName = WALKER.walk(frames -> frames.map(StackWalker.StackFrame::getClassName)
                .filter(name -> !name.equals(Log.class.getName()))
                .findFirst()
                .orElse(Log.class.getName()));
        return LogManager.getLogger(callerName);
    }

    /**
     * Logs an informational message.
     * Use for normal test steps: navigation, actions, verifications that pass.
     */
    public static void info(String message) {
        logger().info(message);
        Reporter.log("[INFO] " + message);
        ExtentTest t = ExtentManager.getTest();
        if (t != null)
            t.info(message);
    }

    /**
     * Logs a warning.
     * Use for non-critical observations that don't fail the test.
     */
    public static void warn(String message) {
        logger().warn(message);
        Reporter.log("[WARN] " + message);
        ExtentTest t = ExtentManager.getTest();
        if (t != null)
            t.warning(message);
    }

    /**
     * Logs an error and immediately fails the active TestNG test.
     * Use wherever you would have called {@code System.err.println} or thrown an
     * assertion.
     */
    public static void fail(String message) {
        logger().error(message);
        Reporter.log("[FAIL] " + message);
        ExtentTest t = ExtentManager.getTest();
        if (t != null)
            t.fail(message);
        Assert.fail(message);
    }

    /**
     * Logs an error with a cause and immediately fails the active TestNG test.
     */
    public static void fail(String message, Throwable cause) {
        logger().error(message, cause);
        Reporter.log("[FAIL] " + message + ": " + cause.getMessage());
        ExtentTest t = ExtentManager.getTest();
        if (t != null)
            t.fail(message + ": " + cause.getMessage());
        Assert.fail(message + ": " + cause.getMessage());
    }
}
