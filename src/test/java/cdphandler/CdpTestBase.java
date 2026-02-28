package cdphandler;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import tools.Log;

/**
 * Abstract TestNG base class providing per-test browser context isolation.
 * <p>
 * A single headless Chrome process is shared across the entire test suite
 * (launched once in {@code @BeforeSuite}). Each {@code @Test} method gets
 * its own isolated {@link BrowserContext} with a fresh {@link ICdpDriver},
 * stored in a {@link ThreadLocal} for thread-safety during parallel execution.
 *
 * <pre>
 * public class MyTest extends CdpTestBase {
 *     &#64;Test
 *     public void testIsolated() {
 *         getDriver().get("https://example.com");
 *         // This runs in its own browser context — no shared state
 *     }
 * }
 * </pre>
 *
 * @see BrowserContext
 * @see BrowserLauncher
 */
public abstract class CdpTestBase {

    private static BrowserLauncher.LaunchedBrowser browser;
    private final ThreadLocal<BrowserContext> threadContext = new ThreadLocal<>();
    private final ThreadLocal<ICdpDriver> threadDriver = new ThreadLocal<>();

    /**
     * Launches a single headless Chrome process for the entire suite.
     */
    @BeforeSuite(alwaysRun = true)
    public void launchBrowser() {
        if (browser == null) {
            browser = BrowserLauncher.launch();
            Log.info("Suite browser launched on port " + browser.port());
        }
    }

    /**
     * Creates an isolated BrowserContext and a CdpDriver for the current test
     * method.
     */
    @BeforeMethod(alwaysRun = true)
    public void createContext() {
        BrowserContext context = new BrowserContext(browser.wsUrl(), browser.port());
        ICdpDriver driver = context.newDriver();
        threadContext.set(context);
        threadDriver.set(driver);
        Log.info("Test context created: " + context.getContextId());
    }

    /**
     * Closes the CdpDriver and disposes the BrowserContext after each test method.
     */
    @AfterMethod(alwaysRun = true)
    public void destroyContext() {
        ICdpDriver driver = threadDriver.get();
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                Log.warn("Failed to close driver: " + e.getMessage());
            }
            threadDriver.remove();
        }

        BrowserContext context = threadContext.get();
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                Log.warn("Failed to close context: " + e.getMessage());
            }
            threadContext.remove();
        }
    }

    /**
     * Kills the shared Chrome process after all tests in the suite have run.
     */
    @AfterSuite(alwaysRun = true)
    public void killBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
            Log.info("Suite browser terminated");
        }
    }

    /**
     * Returns the {@link ICdpDriver} for the current test thread.
     * Each test method gets its own isolated driver backed by a unique
     * BrowserContext.
     *
     * @return The thread-local ICdpDriver instance.
     */
    protected ICdpDriver getDriver() {
        ICdpDriver driver = threadDriver.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No driver available. Ensure the test class extends CdpTestBase " +
                            "and is run via TestNG (not directly).");
        }
        return driver;
    }

    /**
     * Returns the {@link BrowserContext} for the current test thread.
     */
    protected BrowserContext getContext() {
        return threadContext.get();
    }

    /**
     * Returns the shared {@link BrowserLauncher.LaunchedBrowser} instance.
     */
    protected static BrowserLauncher.LaunchedBrowser getBrowser() {
        return browser;
    }
}
