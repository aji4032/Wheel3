package tools;

import cdphandler.ICdpDriver;

/**
 * Thread-local holder for the current test's {@link ICdpDriver}.
 * <p>
 * This allows infrastructure classes like {@link ExtentTestNGListener} and
 * {@link ScreenRecorder} to access the driver without coupling them to
 * the test base class hierarchy.
 * <p>
 * Call {@link #setCurrentDriver(ICdpDriver)} in your
 * {@code @BeforeMethod}, and {@link #removeCurrentDriver()} in your
 * {@code @AfterMethod}.
 */
public class DriverContext {

    private static final ThreadLocal<ICdpDriver> currentDriver = new ThreadLocal<>();

    private DriverContext() {
    }

    /**
     * Registers the driver for the current thread.
     */
    public static void setCurrentDriver(ICdpDriver driver) {
        currentDriver.set(driver);
    }

    /**
     * Returns the driver for the current thread, or {@code null}.
     */
    public static ICdpDriver getCurrentDriver() {
        return currentDriver.get();
    }

    /**
     * Clears the driver for the current thread.
     */
    public static void removeCurrentDriver() {
        currentDriver.remove();
    }
}
