package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import tools.Utilities;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CdpDriver implements AutoCloseable {
    private Duration POLLING_INTERVAL = Duration.ofMillis(50);
    private Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
    private Duration PAGE_LOAD_TIMEOUT = Duration.ofMinutes(1);
    private final CdpUtility cdpUtility;

    private int currentModifierValue = 0;
    private static final List<CdpKey> modifierKeys = List.of(CdpKey.Alt, CdpKey.Control, CdpKey.Meta, CdpKey.Shift);

    public CdpDriver(String websocketDebuggerAddress) {
        this.cdpUtility = new CdpUtility(websocketDebuggerAddress);
        cdpUtility.runtimeEvaluate(CdpScripts.CDP_ELEMENTS_CLEANUP_SCRIPT, false);
    }

    public void back() {
        checkBrowsingContextOpen();
        cdpUtility.runtimeEvaluate(CdpScripts.BACK_SCRIPT, false);
        sleep(getPollingInterval());
        waitUntilDocumentReady();
        System.out.println("Navigating back to: " + getCurrentUrl());
    }

    public String captureScreenshot() {
        return cdpUtility.pageCaptureScreenshot("png").get("data").asText();
    }

    private void checkBrowsingContextOpen() {
        if(true)    //TODO: Check why getWindowHandle() is getting stuck
            return;
        if (getWindowHandle() == null) {
            throw new RuntimeException("no such window: target window is already closed");
        }
    }

    @Override
    public void close() {
        cdpUtility.close();
        System.out.println("Closed websocket connection.");
    }

    public void closeBrowser(){
        cdpUtility.browserClose();
        System.out.println("Browser closed");
    }

    public void closeTab(){
        cdpUtility.pageClose();
        System.out.println("Page/Tab closed");
    }

    public void closeWindow(){
        String currentHandle = getWindowHandle();
        if (currentHandle == null) {
            throw new RuntimeException("no such window: target window is already closed");
        }
        cdpUtility.targetCloseTarget(currentHandle);

        List<String> handles = getWindowHandles();
        if (handles.isEmpty()) {
            closeBrowser();
        }
    }

    public CdpElement findElement(CdpBy by) {
        return findElement(by, DEFAULT_TIMEOUT);
    }

    public CdpElement findElement(CdpBy by, Duration duration) {
        List<CdpElement> elements = findElements(by, duration);
        if(elements.isEmpty()) {
            System.err.printf("Failed to find element: %s%n", by);
            System.exit(1);
        }
        return elements.getFirst();
    }

    public List<CdpElement> findElements(CdpBy by) {
        return findElements(by, DEFAULT_TIMEOUT);
    }

    public List<CdpElement> findElements(CdpBy by, Duration duration) {
        checkBrowsingContextOpen();
        String locatorScript = switch (by.getType()) {
            case ID    -> CdpScripts.ID_LOCATOR_SCRIPT;
            case CSS   -> CdpScripts.CSS_LOCATOR_SCRIPT;
            case XPATH -> CdpScripts.XPATH_LOCATOR_SCRIPT;
        };

        String script = String.format(CdpScripts.FIND_ELEMENT_SCRIPT.replace("<locatorScript>", locatorScript), "", by.getLocator());
        AtomicReference<List<CdpElement>> cdpElements = new AtomicReference<>(new ArrayList<>());
        Utilities.waitUntil(() -> {
            JsonNode result = cdpUtility.runtimeEvaluate(script, true);
            if (result != null && result.has("value")) {
                JsonNode valueNode = result.get("value");
                if (valueNode.isArray() && !valueNode.isEmpty()) {
                    ArrayNode values = (ArrayNode) valueNode;
                    for (int i = 0; i < values.size(); i++) {
                        String name = values.size() == 1 ? by.getName() : by.getName() + "[" + i + "]";
                        cdpElements.get().add(new CdpElement(this, new CdpBy(name, by.getType(), by.getLocator()), values.get(i).asText()));
                    }
                    return !cdpElements.get().isEmpty();
                }
            }
            sleep(getPollingInterval());
            return false;
        }, duration);
        return cdpElements.get();
    }

    public void forward() {
        checkBrowsingContextOpen();
        cdpUtility.runtimeEvaluate(CdpScripts.FORWARD_SCRIPT, false);
        sleep(getPollingInterval());
        waitUntilDocumentReady();
        System.out.println("Navigating forward to: " + getCurrentUrl());
    }

    public void fullScreenWindow(){
        //TODO: Implement
    }

    public void get(String url) {
        checkBrowsingContextOpen();
        validateUrl(url);
        cdpUtility.pageNavigate(url);
        sleep(getPollingInterval());
        waitUntilDocumentReady();
        System.out.println("Navigating to: " + getCurrentUrl());
    }

    protected CdpUtility getCdpUtility() {
        return cdpUtility;
    }

    protected int getCurrentModifierValue() {
        return currentModifierValue;
    }

    public String getCurrentUrl() {
        checkBrowsingContextOpen();
        return cdpUtility.runtimeEvaluate(CdpScripts.GET_CURRENT_URL_SCRIPT, true).get("value").asText();
    }

    public Duration getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    public Duration getPageLoadTimeout() {
        return PAGE_LOAD_TIMEOUT;
    }

    public Duration getPollingInterval() {
        return POLLING_INTERVAL;
    }

    public String getTitle() {
        checkBrowsingContextOpen();
        return cdpUtility.runtimeEvaluate(CdpScripts.GET_TITLE_SCRIPT, true).get("value").asText();
    }

    public String getWindowHandle() {
        JsonNode targets = cdpUtility.targetGetTargets();
        if (targets.has("targetInfos")) {
            targets = targets.get("targetInfos");
        }
        if (targets.isArray()) {
            for (JsonNode target : targets) {
                if (target.has("type") && "page".equals(target.get("type").asText()) && target.has("attached") && target.get("attached").asBoolean()) {
                    return target.get("targetId").asText();
                }
            }
        }
        return null;
    }

    public List<String> getWindowHandles() {
        List<String> handles = new ArrayList<>();
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
        return handles;
    }

    public CdpRect getWindowRect() {
        String targetId = getWindowHandle();
        if (targetId == null) throw new RuntimeException("No target attached");
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
    }

    public boolean isElementPresent(CdpBy by) {
        return !findElements(by, Duration.ofSeconds(1)).isEmpty();
    }

    public void keyDown(CdpKey key) {
        if(modifierKeys.contains(key))
            currentModifierValue += key.getModifier();
        cdpUtility.inputDispatchKeyEvent("keyDown", getCurrentModifierValue(), key.getText(), "", key.getCode(), key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
    }

    public void keyUp(CdpKey key) {
        if(modifierKeys.contains(key))
            currentModifierValue -= key.getModifier();
        cdpUtility.inputDispatchKeyEvent("keyUp",   getCurrentModifierValue(), key.getText(), "", key.getCode(), key.getKey(), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
    }

    public void keyPress(CdpKey key) {
        keyDown(key);
        sleep(getPollingInterval());
        keyUp(key);
    }

    public void maximizeWindow(){
        //TODO: Implement
    }

    public void minimizeWindow(){
        //TODO: Implement
    }

    public void refresh() {
        checkBrowsingContextOpen();
        cdpUtility.pageReload();
        sleep(getPollingInterval());
        waitUntilDocumentReady();
        System.out.println("Refreshing page: " + getCurrentUrl());
    }

    public void sendKeys(String text) {
        for (char character : text.toCharArray()) {
            CdpKey key = CdpKey.getCdpKey(character);
            cdpUtility.inputDispatchKeyEvent("keyDown", getCurrentModifierValue(), String.valueOf(character), "", key.getCode(), String.valueOf(character), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
            sleep(getPollingInterval());
            cdpUtility.inputDispatchKeyEvent("keyUp",   getCurrentModifierValue(), String.valueOf(character), "", key.getCode(), String.valueOf(character), key.getWindowsVirtualKeyCode(), key.getNativeVirtualKeyCode());
        }
        System.out.println("Sending keys: " + text);
    }

    public void setDefaultTimeout(Duration DEFAULT_TIMEOUT) {
        this.DEFAULT_TIMEOUT = DEFAULT_TIMEOUT;
    }

    public void setPageLoadTimeout(Duration PAGE_LOAD_TIMEOUT) {
        this.PAGE_LOAD_TIMEOUT = PAGE_LOAD_TIMEOUT;
    }

    public void setPollingInterval(Duration POLLING_INTERVAL) {
        this.POLLING_INTERVAL = POLLING_INTERVAL;
    }

    public void setWindowRect(CdpRect windowRect) {
        String targetId = getWindowHandle();
        if (targetId == null) throw new RuntimeException("No target attached");
        JsonNode result = cdpUtility.browserGetWindowForTarget(targetId);
        int windowId = result.get("windowId").asInt();
        cdpUtility.browserSetWindowBounds(windowId, windowRect.point().x(), windowRect.point().y(), windowRect.dimension().width(), windowRect.dimension().height(), "normal");
    }

    public void sleep(Duration duration){
        Utilities.sleep(duration);
    }

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
        if (!isReady) System.err.println("Timeout waiting for page to load");
    }
}