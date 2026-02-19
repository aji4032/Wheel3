package tools;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * Singleton that owns the single {@link ExtentReports} instance.
 * <p>
 * A per-thread {@link ExtentTest} is stored via {@link ThreadLocal} so that
 * parallel test runs never share the same test node.
 */
public class ExtentManager {

    private static final String REPORT_PATH = "target/extent-reports/AutomationReport.html";

    private static final ExtentReports extent = createInstance();
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    private static ExtentReports createInstance() {
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
        spark.config().setDocumentTitle("Automation Test Report");
        spark.config().setReportName("Desktop UI Automation");
        spark.config().setTheme(Theme.DARK);
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        return reports;
    }

    /** Returns the singleton {@link ExtentReports} instance. */
    public static ExtentReports getInstance() {
        return extent;
    }

    /**
     * Creates a new {@link ExtentTest} for the current thread.
     * Call this at the start of each test (from the TestNG listener).
     */
    public static ExtentTest createTest(String name) {
        ExtentTest test = extent.createTest(name);
        testThread.set(test);
        return test;
    }

    /**
     * Returns the {@link ExtentTest} for the current thread, or {@code null}
     * if called outside a TestNG test context.
     */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /** Flushes the report to disk. Call once at the end of the suite. */
    public static void flush() {
        extent.flush();
    }

    private ExtentManager() {
    }
}
