package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import tools.Log;
import tools.Logger;
import tools.Utilities;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CdpDriver implements ICdpDriver {
    private static final Logger log = Log.getLogger(CdpDriver.class);
    private CdpTraceCollector traceCollector;

    @Override
    public void startTracing(File zipFile) {
        this.traceCollector = new CdpTraceCollector(this, zipFile);
        this.traceCollector.start();
    }

    @Override
    public void stopTracing() {
        if (this.traceCollector != null) {
            this.traceCollector.stop();
            this.traceCollector = null;
        }
    }

    public CdpTraceCollector getTraceCollector() {
        return traceCollector;
    }

    public <T> T record(String name, String type, String target, CdpRect elementRect, Object[] args, Supplier<T> action) {
        if (traceCollector != null) {
            return traceCollector.record(name, type, target, elementRect, args, action);
        }
        return action.get();
    }

    public void record(String name, String type, String target, CdpRect elementRect, Object[] args, Runnable action) {
        if (traceCollector != null) {
            traceCollector.record(name, type, target, elementRect, args, action);
        } else {
            action.run();
        }
    }

    /**
     * Returns true if tracing is currently active. Use this to guard lambda
     * creation at call sites so no closure objects are allocated when not tracing.
     */
    private boolean isTracing() {
        return traceCollector != null;
    }
    private Duration POLLING_INTERVAL = Duration.ofMillis(50);
    private Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
    private Duration PAGE_LOAD_TIMEOUT = Duration.ofMinutes(1);
    private final CdpUtility cdpUtility;
    private BrowserLauncher.LaunchedBrowser launchedBrowser;

    private int currentModifierValue = 0;
    private static final List<CdpKey> modifierKeys = List.of(CdpKey.Alt, CdpKey.Control, CdpKey.Meta, CdpKey.Shift);

    public CdpDriver(String websocketDebuggerAddress) {
        this.cdpUtility = new CdpUtility(websocketDebuggerAddress);
        cdpUtility.runtimeEvaluate(CdpScripts.CDP_ELEMENTS_CLEANUP_SCRIPT, false);
    }

    // -----------------------------------------------------------------------
    // Static launch factory methods
    // -----------------------------------------------------------------------

    /**
     * Launches Chrome in headed mode (with UI) on a free OS-assigned port with
     * remote debugging enabled, and returns a ready-to-use {@code CdpDriver}
     * connected to the first page target.
     * <p>
     * The browser process is automatically killed when the driver is
     * {@linkplain #close() closed} or when the JVM shuts down.
     *
     * <pre>
     * CdpDriver driver = CdpDriver.launch();
     * driver.get("https://example.com");
     * // … interact …
     * driver.close(); // also terminates the Chrome process
     * </pre>
     */
    public static CdpDriver launch() {
        return launch(false);
    }

    /**
     * Launches Chrome on a free OS-assigned port with remote debugging enabled
     * and returns a ready-to-use {@code CdpDriver} connected to the first page.
     *
     * @param headless {@code true} to launch in headless mode,
     *                 {@code false} for a visible browser window.
     */
    public static CdpDriver launch(boolean headless) {
        return launch(0, headless);
    }

    /**
     * Launches Chrome on the given debugging port with remote debugging enabled
     * and returns a ready-to-use {@code CdpDriver} connected to the first page.
     *
     * @param port     The debugging port to use (0 = auto-assign a free port).
     * @param headless {@code true} for headless, {@code false} for headed.
     */
    public static CdpDriver launch(int port, boolean headless) {
        BrowserLauncher.LaunchedBrowser browser = headless
                ? BrowserLauncher.launch(port)
                : BrowserLauncher.launchHeaded(port);

        String pageWsUrl = BrowserLauncher.getFirstPageWsUrl(browser.port());
        log.info("Connected to page target: " + pageWsUrl);

        CdpDriver driver = new CdpDriver(pageWsUrl);
        driver.launchedBrowser = browser;

        // Ensure the browser process is killed even if close() is never called
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
        }));

        return driver;
    }

    @Override
    public void back() {
        if (isTracing()) {
            record("back", "driver", null, null, null, () -> {
                checkBrowsingContextOpen();
                cdpUtility.runtimeEvaluate(CdpScripts.BACK_SCRIPT, false);
                sleep(getPollingInterval());
                waitUntilDocumentReady();
                log.info("Navigating back to: " + getCurrentUrl());
            });
        } else {
            checkBrowsingContextOpen();
            cdpUtility.runtimeEvaluate(CdpScripts.BACK_SCRIPT, false);
            sleep(getPollingInterval());
            waitUntilDocumentReady();
            log.info("Navigating back to: " + getCurrentUrl());
        }
    }

    @Override
    public String captureScreenshot() {
        return cdpUtility.pageCaptureScreenshot("png").get("data").asText();
    }

    private void checkBrowsingContextOpen() {
        return; // TODO: Check why getWindowHandle() is getting stuck
    }

    @Override
    public void close() {
        if (isTracing()) {
            record("close", "driver", null, null, null, () -> {
                cdpUtility.close();
                if (launchedBrowser != null) {
                    launchedBrowser.close();
                    launchedBrowser = null;
                    log.info("Closed websocket connection and terminated browser process.");
                } else {
                    log.info("Closed websocket connection.");
                }
            });
        } else {
            cdpUtility.close();
            if (launchedBrowser != null) {
                launchedBrowser.close();
                launchedBrowser = null;
                log.info("Closed websocket connection and terminated browser process.");
            } else {
                log.info("Closed websocket connection.");
            }
        }
    }

    @Override
    public void closeBrowser() {
        record("closeBrowser", "driver", null, null, null, () -> {
            cdpUtility.browserClose();
            log.info("Browser closed");
        });
    }

    @Override
    public void closeTab() {
        record("closeTab", "driver", null, null, null, () -> {
            cdpUtility.pageClose();
            log.info("Page/Tab closed");
        });
    }

    @Override
    public void closeWindow() {
        record("closeWindow", "driver", null, null, null, () -> {
            String currentHandle = getWindowHandle();
            if (currentHandle == null) {
                throw new RuntimeException("no such window: target window is already closed");
            }
            cdpUtility.targetCloseTarget(currentHandle);

            List<String> handles = getWindowHandles();
            if (handles.isEmpty()) {
                closeBrowser();
            }
        });
    }

    @Override
    public ICdpElement findElement(CdpBy by) {
        return findElement(by, DEFAULT_TIMEOUT);
    }

    @Override
    public ICdpElement findElement(CdpBy by, Duration duration) {
        if (isTracing()) {
            return record("findElement", "driver", by.toString(), null, new Object[] { by, duration }, () -> {
                List<ICdpElement> elements = findElements(by, duration);
                if (elements.isEmpty()) log.fail(String.format("Failed to find element: %s", by));
                return elements.getFirst();
            });
        }
        List<ICdpElement> elements = findElements(by, duration);
        if (elements.isEmpty()) log.fail(String.format("Failed to find element: %s", by));
        return elements.getFirst();
    }

    @Override
    public List<ICdpElement> findElements(CdpBy by) {
        return findElements(by, DEFAULT_TIMEOUT);
    }

    @Override
    public List<ICdpElement> findElements(CdpBy by, Duration duration) {
        if (isTracing()) {
            return record("findElements", "driver", by.toString(), null, new Object[] { by, duration }, () -> doFindElements(by, duration));
        }
        return doFindElements(by, duration);
    }

    private List<ICdpElement> doFindElements(CdpBy by, Duration duration) {
        checkBrowsingContextOpen();
        String locatorScript = switch (by.type()) {
            case ID -> CdpScripts.ID_LOCATOR_SCRIPT;
            case CSS -> CdpScripts.CSS_LOCATOR_SCRIPT;
            case XPATH -> CdpScripts.XPATH_LOCATOR_SCRIPT;
            case PIERCING_CSS -> CdpScripts.PIERCING_CSS_LOCATOR_SCRIPT;
            default -> throw new IllegalStateException("Unexpected value: " + by.type());
        };
        String script = String.format(CdpScripts.FIND_ELEMENT_SCRIPT.replace("<locatorScript>", locatorScript), "", by.locator());
        AtomicReference<List<ICdpElement>> cdpElements = new AtomicReference<>(new ArrayList<>());
        Utilities.waitUntil(() -> {
            JsonNode result = cdpUtility.runtimeEvaluate(script, true);
            if (result != null && result.has("value")) {
                JsonNode valueNode = result.get("value");
                if (valueNode.isArray() && !valueNode.isEmpty()) {
                    ArrayNode values = (ArrayNode) valueNode;
                    for (int i = 0; i < values.size(); i++) {
                        String name = values.size() == 1 ? by.name() : by.name() + "[" + i + "]";
                        cdpElements.get().add(new CdpElement(this, new CdpBy(name, by.type(), by.locator()), values.get(i).asText()));
                    }
                    return !cdpElements.get().isEmpty();
                }
            }
            sleep(getPollingInterval());
            return false;
        }, duration);
        return cdpElements.get();
    }

    @Override
    public void forward() {
        record("forward", "driver", null, null, null, () -> {
            checkBrowsingContextOpen();
            cdpUtility.runtimeEvaluate(CdpScripts.FORWARD_SCRIPT, false);
            sleep(getPollingInterval());
            waitUntilDocumentReady();
            log.info("Navigating forward to: " + getCurrentUrl());
        });
    }

    @Override
    public void fullScreenWindow() {
        try {
            cdpUtility.browserSetWindowBounds(getWindowId(), -1, -1, -1, -1, "fullscreen");
        } catch (RuntimeException e) {
            log.warn("Failed to fullscreen window: " + e.getMessage());
        }
    }

    @Override
    public void get(String url) {
        if (isTracing()) {
            record("get", "driver", null, null, new Object[] { url }, () -> {
                checkBrowsingContextOpen();
                validateUrl(url);
                cdpUtility.pageNavigate(url);
                sleep(getPollingInterval());
                waitUntilDocumentReady();
                log.info("Navigating to: " + getCurrentUrl());
            });
        } else {
            checkBrowsingContextOpen();
            validateUrl(url);
            cdpUtility.pageNavigate(url);
            sleep(getPollingInterval());
            waitUntilDocumentReady();
            log.info("Navigating to: " + getCurrentUrl());
        }
    }

    @Override
    public CdpUtility getCdpUtility() {
        return cdpUtility;
    }

    @Override
    public int getCurrentModifierValue() {
        return currentModifierValue;
    }

    @Override
    public String getCurrentUrl() {
        checkBrowsingContextOpen();
        return cdpUtility.runtimeEvaluate(CdpScripts.GET_CURRENT_URL_SCRIPT, true).get("value").asText();
    }

    @Override
    public Duration getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public Duration getPageLoadTimeout() {
        return PAGE_LOAD_TIMEOUT;
    }

    @Override
    public String getPageSource() {
        checkBrowsingContextOpen();
        return cdpUtility.runtimeEvaluate(CdpScripts.GET_PAGE_SOURCE, true).get("value").asText();
    }

    @Override
    public Duration getPollingInterval() {
        return POLLING_INTERVAL;
    }

    @Override
    public String getTitle() {
        checkBrowsingContextOpen();
        return cdpUtility.runtimeEvaluate(CdpScripts.GET_TITLE_SCRIPT, true).get("value").asText();
    }

    @Override
    public String getWindowHandle() {
        try {
            JsonNode targets = cdpUtility.targetGetTargets();
            if (targets.has("targetInfos")) {
                targets = targets.get("targetInfos");
            }
            if (targets.isArray()) {
                for (JsonNode target : targets) {
                    if (target.has("type") && "page".equals(target.get("type").asText()) && target.has("attached")
                            && target.get("attached").asBoolean()) {
                        return target.get("targetId").asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Error getting window handle: " + e.getMessage());
            return null;
        }
    }

    private int getWindowId() {
        String targetId = null;
        try {
            targetId = getWindowHandle();
        } catch (Exception e) {
            log.warn("Error getting window handle: " + e.getMessage());
        }

        if (targetId == null) {
            throw new RuntimeException("No target attached");
        }

        try {
            JsonNode result = cdpUtility.browserGetWindowForTarget(targetId);
            return result.get("windowId").asInt();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get window ID for target " + targetId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getWindowHandles() {
        List<String> handles = new ArrayList<>();
        try {
            JsonNode targets = cdpUtility.targetGetTargets();
            if (targets.has("targetInfos")) {
                targets = targets.get("targetInfos");
            }
            if (targets.isArray()) {
                for (JsonNode target : targets) {
                    if (target.has("type") && "page".equals(target.get("type").asText())) {
                        handles.add(target.get("targetId").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error getting window handles: " + e.getMessage());
        }
        return handles;
    }

    @Override
    public CdpRect getWindowRect() {
        try {
            String targetId = getWindowHandle();
            if (targetId == null) {
                log.warn("No target attached, cannot get window rect");
                return null;
            }
            JsonNode result = cdpUtility.browserGetWindowForTarget(targetId);
            if (result.has("bounds")) {
                JsonNode bounds = result.get("bounds");
                int x = bounds.has("left") ? bounds.get("left").asInt() : 0;
                int y = bounds.has("top") ? bounds.get("top").asInt() : 0;
                int width = bounds.has("width") ? bounds.get("width").asInt() : 0;
                int height = bounds.has("height") ? bounds.get("height").asInt() : 0;
                return new CdpRect(new CdpPoint(x, y), new CdpDimension(width, height));
            }
            return null;
        } catch (Exception e) {
            log.warn("Error getting window rect: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isElementPresent(CdpBy by) {
        return !findElements(by, Duration.ofSeconds(1)).isEmpty();
    }

    @Override
    public void keyDown(CdpKey key) {
        if (isTracing()) {
            record("keyDown", "driver", null, null, new Object[] { key }, () ->
                cdpUtility.inputDispatchKeyEvent("keyDown", getCurrentModifierValue(), key.getText(), "", key.getCode(),
                        key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode()));
        } else {
            if (modifierKeys.contains(key)) currentModifierValue += key.getModifier();
            cdpUtility.inputDispatchKeyEvent("keyDown", getCurrentModifierValue(), key.getText(), "", key.getCode(),
                    key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
        }
    }

    @Override
    public void keyUp(CdpKey key) {
        if (isTracing()) {
            record("keyUp", "driver", null, null, new Object[] { key }, () ->
                cdpUtility.inputDispatchKeyEvent("keyUp", getCurrentModifierValue(), key.getText(), "", key.getCode(),
                        key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode()));
        } else {
            if (modifierKeys.contains(key)) currentModifierValue -= key.getModifier();
            cdpUtility.inputDispatchKeyEvent("keyUp", getCurrentModifierValue(), key.getText(), "", key.getCode(),
                    key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
        }
    }

    @Override
    public void keyPress(CdpKey key) {
        record("keyPress", "driver", null, null, new Object[] { key }, () -> {
            keyDown(key);
            sleep(getPollingInterval());
            keyUp(key);
        });
    }

    @Override
    public void maximizeWindow() {
        record("maximizeWindow", "driver", null, null, null, () -> {
            try {
                cdpUtility.browserSetWindowBounds(getWindowId(), -1, -1, -1, -1, "maximized");
            } catch (RuntimeException e) {
                log.warn("Failed to maximize window: " + e.getMessage() + ". Attempting retry...");
                try {
                    Thread.sleep(500);
                    cdpUtility.browserSetWindowBounds(getWindowId(), -1, -1, -1, -1, "maximized");
                } catch (Exception retryException) {
                    log.warn("Failed to maximize window after retry: " + retryException.getMessage());
                    throw new RuntimeException("Failed to maximize window", retryException);
                }
            }
        });
    }

    @Override
    public void minimizeWindow() {
        record("minimizeWindow", "driver", null, null, null, () -> {
            try {
                cdpUtility.browserSetWindowBounds(getWindowId(), -1, -1, -1, -1, "minimized");
            } catch (RuntimeException e) {
                log.warn("Failed to minimize window: " + e.getMessage() + ". Attempting retry...");
                try {
                    Thread.sleep(500);
                    cdpUtility.browserSetWindowBounds(getWindowId(), -1, -1, -1, -1, "minimized");
                } catch (Exception retryException) {
                    log.warn("Failed to minimize window after retry: " + retryException.getMessage());
                    throw new RuntimeException("Failed to minimize window", retryException);
                }
            }
        });
    }

    @Override
    public void refresh() {
        record("refresh", "driver", null, null, null, () -> {
            checkBrowsingContextOpen();
            cdpUtility.pageReload();
            sleep(getPollingInterval());
            waitUntilDocumentReady();
            log.info("Refreshing page: " + getCurrentUrl());
        });
    }

    @Override
    public void sendKeys(String text) {
        record("sendKeys", "driver", null, null, new Object[] { text }, () -> {
            for (char character : text.toCharArray()) {
                CdpKey key = CdpKey.getCdpKey(character);
                cdpUtility.inputDispatchKeyEvent("keyDown", getCurrentModifierValue(), String.valueOf(character), "",
                        key.getCode(), String.valueOf(character), key.getWindowsVirtualKeyCode(),
                        key.getNativeVirtualKeyCode());
                sleep(getPollingInterval());
                cdpUtility.inputDispatchKeyEvent("keyUp", getCurrentModifierValue(), String.valueOf(character), "",
                        key.getCode(), String.valueOf(character), key.getWindowsVirtualKeyCode(),
                        key.getNativeVirtualKeyCode());
            }
            log.info("Sending keys: " + text);
        });
    }

    @Override
    public void setDefaultTimeout(Duration DEFAULT_TIMEOUT) {
        this.DEFAULT_TIMEOUT = DEFAULT_TIMEOUT;
    }

    @Override
    public void setPageLoadTimeout(Duration PAGE_LOAD_TIMEOUT) {
        this.PAGE_LOAD_TIMEOUT = PAGE_LOAD_TIMEOUT;
    }

    @Override
    public void setPollingInterval(Duration POLLING_INTERVAL) {
        this.POLLING_INTERVAL = POLLING_INTERVAL;
    }

    @Override
    public void setWindowRect(CdpRect windowRect) {
        record("setWindowRect", "driver", null, null, new Object[] { windowRect }, () -> {
            try {
                cdpUtility.browserSetWindowBounds(getWindowId(), windowRect.point().x(), windowRect.point().y(),
                        windowRect.dimension().width(), windowRect.dimension().height(), "normal");
            } catch (RuntimeException e) {
                log.warn("Failed to set window rect: " + e.getMessage() + ". Attempting retry...");
                try {
                    Thread.sleep(500);
                    cdpUtility.browserSetWindowBounds(getWindowId(), windowRect.point().x(), windowRect.point().y(),
                            windowRect.dimension().width(), windowRect.dimension().height(), "normal");
                } catch (Exception retryException) {
                    log.warn("Failed to set window rect after retry: " + retryException.getMessage());
                    throw new RuntimeException("Failed to set window rect", retryException);
                }
            }
        });
    }

    @Override
    public void sleep(Duration duration) {
        Utilities.sleep(duration);
    }

    @Override
    public void switchToWindow(String windowHandle) {
        List<String> handles = getWindowHandles();
        if (!handles.contains(windowHandle)) {
            throw new RuntimeException("no such window: window handle not found");
        }
        cdpUtility.targetActivateTarget(windowHandle);
    }

    private void validateUrl(String url) {
        if (url == null || !url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            throw new IllegalArgumentException("invalid argument: URL must be an absolute URL: " + url);
        }
    }

    private void waitUntilDocumentReady() {
        boolean isReady = Utilities.waitUntil(() -> {
            JsonNode result = cdpUtility.runtimeEvaluate(CdpScripts.WAIT_UNTIL_DOCUMENT_READY, true);
            return result != null && result.has("value") && result.get("value").asBoolean();
        }, PAGE_LOAD_TIMEOUT);
        if (!isReady)
            log.warn("Timeout waiting for page to load");
    }
}