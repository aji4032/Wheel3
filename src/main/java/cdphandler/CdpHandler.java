package cdphandler;

import tools.Log;

import java.lang.reflect.Proxy;

public class CdpHandler {

    public static ICdpDriver createDriver(String websocketDebuggerAddress) {
        CdpDriver cdpDriver = new CdpDriver(websocketDebuggerAddress);
        return (ICdpDriver) Proxy.newProxyInstance(
                ICdpDriver.class.getClassLoader(),
                new Class<?>[] { ICdpDriver.class },
                new OllamaProxy(cdpDriver));
    }

    /**
     * Launches a headless Chrome on a random port, connects to the first page,
     * and returns a ready-to-use driver.
     * <p>
     * The Chrome process is automatically killed when the JVM exits.
     * For explicit control, use {@link BrowserLauncher#launch()} directly.
     */
    public static ICdpDriver launchAndConnect() {
        return launchAndConnect(0);
    }

    /**
     * Launches a headless Chrome on the given port (0 = auto-assign),
     * connects to the first page, and returns a ready-to-use driver.
     */
    public static ICdpDriver launchAndConnect(int port) {
        BrowserLauncher.LaunchedBrowser browser = BrowserLauncher.launch(port);
        String pageWsUrl = BrowserLauncher.getFirstPageWsUrl(browser.port());
        Log.info("Connected to page target: " + pageWsUrl);

        // Register a shutdown hook so the browser gets killed even if close() is never
        // called
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
        }));

        return createDriver(pageWsUrl);
    }

    public static ICdpElement createElement(ICdpElement cdpElement) {
        return (ICdpElement) Proxy.newProxyInstance(
                ICdpElement.class.getClassLoader(),
                new Class<?>[] { ICdpElement.class },
                new CdpElementProxy(cdpElement));
    }
}
