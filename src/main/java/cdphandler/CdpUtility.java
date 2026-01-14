package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for interacting with the Chrome DevTools Protocol (CDP).
 * Provides methods to execute various CDP commands across different domains
 * such as Browser, DOM, Input, Log, Network, Overlay, Page, Performance, Runtime, and SystemInfo.
 */
public class CdpUtility {
    private final CdpClient client;
    private final ApiInterceptor apiInterceptor;
    /**
     * Default duration for command execution timeout.
     */
    public Duration defaultDuration = Duration.ofMinutes(2);

    /**
     * Constructs a new CdpUtility instance.
     *
     * @param websocketDebuggerAddress The WebSocket URL for connecting to the browser's DevTools interface.
     */
    public CdpUtility(String websocketDebuggerAddress) {
        this.client = new CdpClient(websocketDebuggerAddress);
        this.apiInterceptor = new ApiInterceptor(client);
        client.addEventListener(evt -> System.out.println("Event: " + evt.toString()));
    }

    /**
     * Closes the underlying CDP client connection.
     */
    public void close() {
        client.close();
    }

    private JsonNode executeCdpCommand(String command, Map<String, Object> map) {
        return executeCdpCommand(command, map, defaultDuration);
    }

    private JsonNode executeCdpCommand(String command, Map<String, Object> map, Duration timeout) {
        try {
            JsonNode result = client.sendCommand(command, map, timeout);
            System.out.println(command + " invoked: \nmap: " + map + "; \nresult: " + result);
            return result.get("result");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns version information.
     *
     * @return The version information.
     */
    public JsonNode browserGetVersion() {
        return executeCdpCommand("Browser.getVersion", Map.of(), defaultDuration);
    }

    /**
     * Closes the browser.
     *
     * @return The command result.
     */
    public JsonNode browserClose() {
        return executeCdpCommand("Browser.close", Map.of(), defaultDuration);
    }

    /**
     * Describes the node given its backend node id.
     *
     * @param backendNodeId The backend node id.
     * @return The node description.
     */
    public JsonNode domDescribeNode(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.describeNode", map, defaultDuration);
    }

    /**
     * Disables DOM agent for the given page.
     *
     * @return The command result.
     */
    public JsonNode domDisable() {
        return executeCdpCommand("DOM.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables DOM agent for the given page.
     *
     * @return The command result.
     */
    public JsonNode domEnable() {
        return executeCdpCommand("DOM.enable", Map.of(), defaultDuration);
    }

    /**
     * Focuses the given element.
     *
     * @param backendNodeId The backend node id of the element to focus.
     * @return The command result.
     */
    public JsonNode domFocus(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.focus", map, defaultDuration);
    }

    /**
     * Returns attributes for the specified node.
     *
     * @param nodeId The node id.
     * @return The attributes of the node.
     */
    public JsonNode domGetAttributes(int nodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        return executeCdpCommand("DOM.getAttributes", map, defaultDuration);
    }

    /**
     * Returns boxes for the given node.
     *
     * @param backendNodeId The backend node id.
     * @return The box model.
     */
    public JsonNode domGetBoxModel(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.getBoxModel", map, defaultDuration);
    }

    /**
     * Returns the root DOM node (and optionally the subtree) to the caller.
     *
     * @return The document root.
     */
    public JsonNode domGetDocument() {
        return executeCdpCommand("DOM.getDocument", Map.of(), defaultDuration);
    }

    /**
     * Returns the node id at a given location.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return The node id at the location.
     */
    public JsonNode domGetNodeForLocation (int x, int y) {
        Map<String, Object> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        return executeCdpCommand("DOM.getNodeForLocation", map, defaultDuration);
    }

    /**
     * Returns node's HTML markup.
     *
     * @param backendNodeId The backend node id.
     * @return The outer HTML.
     */
    public JsonNode domGetOuterHtml(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.getOuterHTML", map, defaultDuration);
    }

    /**
     * Executes querySelector on a given node.
     *
     * @param nodeId The node id to query from.
     * @param selector The selector string.
     * @return The query result.
     */
    public JsonNode domQuerySelector(int nodeId, String selector) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        map.put("selector", selector);
        return executeCdpCommand("DOM.querySelector", map, defaultDuration);
    }

    /**
     * Executes querySelectorAll on a given node.
     *
     * @param nodeId The node id to query from.
     * @param selector The selector string.
     * @return The query result.
     */
    public JsonNode domQuerySelectorAll(int nodeId, String selector) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        map.put("selector", selector);
        return executeCdpCommand("DOM.querySelectorAll", map, defaultDuration);
    }

    /**
     * Dispatches a key event to the page.
     *
     * @param type Type of the key event.
     * @param modifiers Bit field representing pressed modifier keys.
     * @param text Text as generated by processing a virtual key code with a keyboard layout.
     * @param keyIdentifier Unique identifier of the key.
     * @param code Unique DOM defined string value for each physical key (e.g., 'KeyA').
     * @param key Unique DOM defined string value describing the meaning of the key in the context of active modifiers, keyboard layout, etc (e.g., 'Alt', 'F1', 'a').
     * @param windowsVirtualKeyCode Windows virtual key code.
     * @param nativeVirtualKeyCode Native virtual key code.
     * @return The command result.
     */
    public JsonNode inputDispatchKeyEvent(String type, int modifiers, String text, String keyIdentifier, String code, String key, int windowsVirtualKeyCode, int nativeVirtualKeyCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("modifiers", modifiers);
        map.put("text", text);
        map.put("keyIdentifier", keyIdentifier);
        map.put("code", code);
        map.put("key", key);
        map.put("windowsVirtualKeyCode", windowsVirtualKeyCode);
        map.put("nativeVirtualKeyCode", nativeVirtualKeyCode);
        return executeCdpCommand("Input.dispatchKeyEvent", map, defaultDuration);
    }

    /**
     * Dispatches a mouse event to the page.
     *
     * @param type Type of the mouse event.
     * @param x X coordinate of the event relative to the main frame's viewport in CSS pixels.
     * @param y Y coordinate of the event relative to the main frame's viewport in CSS pixels.
     * @param modifiers Bit field representing pressed modifier keys.
     * @param button Mouse button (left, right, middle, back, forward, none).
     * @param clickCount Number of times the mouse button was clicked.
     * @return The command result.
     */
    public JsonNode inputDispatchMouseEvent(String type, int x, int y, int modifiers, String button, int clickCount) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("x", x);
        map.put("y", y);
        map.put("modifiers", modifiers);
        map.put("button", button);
        map.put("clickCount", clickCount);
        return executeCdpCommand("Input.dispatchMouseEvent", map, defaultDuration);
    }

    /**
     * Clears the log.
     *
     * @return The command result.
     */
    public JsonNode logClear() {
        return executeCdpCommand("Log.clear", Map.of(), defaultDuration);
    }

    /**
     * Disables log domain, prevents further log entries from being reported to the client.
     *
     * @return The command result.
     */
    public JsonNode logDisable() {
        return executeCdpCommand("Log.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables log domain, sends the entries collected so far to the client by means of the entryAdded notification.
     *
     * @return The command result.
     */
    public JsonNode logEnable() {
        return executeCdpCommand("Log.enable", Map.of(), defaultDuration);
    }

    /**
     * Clears browser cache.
     *
     * @return The command result.
     */
    public JsonNode networkClearBrowserCache(){
        return executeCdpCommand("Network.clearBrowserCache", Map.of(), defaultDuration);
    }

    /**
     * Clears browser cookies.
     *
     * @return The command result.
     */
    public JsonNode networkClearBrowserCookies(){
        return executeCdpCommand("Network.clearBrowserCookies", Map.of(), defaultDuration);
    }

    /**
     * Disables network tracking, prevents network events from being sent to the client.
     *
     * @return The command result.
     */
    public JsonNode networkDisable(){
        return executeCdpCommand("Network.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables network tracking, network events will now be delivered to the client.
     *
     * @return The command result.
     */
    public JsonNode networkEnable(){
        return executeCdpCommand("Network.enable", Map.of(), defaultDuration);
    }

    /**
     * Disables domain notifications.
     *
     * @return The command result.
     */
    public JsonNode overlayDisable(){
        return executeCdpCommand("Overlay.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables domain notifications.
     *
     * @return The command result.
     */
    public JsonNode overlayEnable(){
        return executeCdpCommand("Overlay.enable", Map.of(), defaultDuration);
    }

    /**
     * Hides any highlight.
     *
     * @return The command result.
     */
    public JsonNode overlayHideHighlight(){
        return executeCdpCommand("Overlay.hideHighlight", Map.of(), defaultDuration);
    }

    /**
     * Highlights a rectangle with the specified coordinates and size.
     *
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param width Rectangle width.
     * @param height Rectangle height.
     * @return The command result.
     */
    public JsonNode overlayHighlightRect(int x, int y, int width, int height) {
        Map<String, Object> outlineColor = new HashMap<>();
        outlineColor.put("r", 255);
        outlineColor.put("g", 0);
        outlineColor.put("b", 0);

        Map<String, Object> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("width", width);
        map.put("height", height);
        map.put("outlineColor", outlineColor);
        return executeCdpCommand("Overlay.highlightRect", map, defaultDuration);
    }

    public JsonNode pageAddScriptToEvaluateOnNewDocument(String script) {
        Map<String, Object> map = new HashMap<>();
        map.put("source", script);
        return executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", map, defaultDuration);
    }

    /**
     * Brings page to front (activates tab).
     *
     * @return The command result.
     */
    public JsonNode pageBringToFront() {
        return executeCdpCommand("Page.bringToFront", Map.of(), defaultDuration);
    }

    /**
     * Captures page screenshot.
     *
     * @param format Image compression format (jpeg or png).
     * @return The screenshot data.
     */
    public JsonNode pageCaptureScreenshot(String format) {
        Map<String, Object> map = new HashMap<>();
        map.put("format", format);
        return executeCdpCommand("Page.captureScreenshot", map, defaultDuration);
    }

    /**
     * Captures page screenshot with clipping.
     *
     * @param format Image compression format (jpeg or png).
     * @param x X coordinate of the clip area.
     * @param y Y coordinate of the clip area.
     * @param width Width of the clip area.
     * @param height Height of the clip area.
     * @param scale Scale of the screenshot.
     * @return The screenshot data.
     */
    public JsonNode pageCaptureScreenshot(String format, int x, int y, int width, int height, int scale) {
        Map<String, Object> clip = new HashMap<>();
        clip.put("x", x);
        clip.put("y", y);
        clip.put("width", width);
        clip.put("height", height);
        clip.put("scale", scale);

        Map<String, Object> map = new HashMap<>();
        map.put("format", format);
        map.put("clip", clip);
        return executeCdpCommand("Page.captureScreenshot", map, defaultDuration);
    }

    /**
     * Closes the current page.
     *
     * @return The command result.
     */
    public JsonNode pageClose() {
        return executeCdpCommand("Page.close", Map.of(), defaultDuration);
    }

    /**
     * Disables page domain notifications.
     *
     * @return The command result.
     */
    public JsonNode pageDisable() {
        return executeCdpCommand("Page.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables page domain notifications.
     *
     * @return The command result.
     */
    public JsonNode pageEnable() {
        return executeCdpCommand("Page.enable", Map.of(), defaultDuration);
    }

    /**
     * Navigates current page to the given URL.
     *
     * @param url URL to navigate the page to.
     * @return The command result.
     */
    public JsonNode pageNavigate(String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        return executeCdpCommand("Page.navigate", map, defaultDuration);
    }

    /**
     * Reloads given page optionally ignoring the cache.
     *
     * @return The command result.
     */
    public JsonNode pageReload() {
        return executeCdpCommand("Page.reload", Map.of(), defaultDuration);
    }

    /**
     * Disables collecting and reporting metrics.
     *
     * @return The command result.
     */
    public JsonNode performanceDisable() {
        return executeCdpCommand("Performance.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables collecting and reporting metrics.
     *
     * @return The command result.
     */
    public JsonNode performanceEnable() {
        return executeCdpCommand("Performance.enable", Map.of(), defaultDuration);
    }

    /**
     * Retrieve current values of run-time metrics.
     *
     * @return The metrics.
     */
    public JsonNode performanceGetMetrics() {
        return executeCdpCommand("Performance.getMetrics", Map.of(), defaultDuration);
    }

    /**
     * Disables reporting of execution contexts creation.
     *
     * @return The command result.
     */
    public JsonNode runtimeDisable(){
        return executeCdpCommand("Runtime.disable", Map.of(), defaultDuration);
    }

    /**
     * Enables reporting of execution contexts creation by means of executionContextCreated event.
     *
     * @return The command result.
     */
    public JsonNode runtimeEnable(){
        return executeCdpCommand("Runtime.enable", Map.of(), defaultDuration);
    }

    /**
     * Evaluates expression on global object.
     *
     * @param expression Expression to evaluate.
     * @param returnByValue Whether the result is expected to be a JSON object that should be sent by value.
     * @return The evaluation result.
     */
    public JsonNode runtimeEvaluate(String expression, boolean returnByValue){
        Map<String, Object> map = new HashMap<>();
        map.put("expression", expression);
        map.put("returnByValue", returnByValue);
        return executeCdpCommand("Runtime.evaluate", map, defaultDuration);
    }

    /**
     * Returns information about the system.
     *
     * @return The system info.
     */
    public JsonNode systemInfoGetInfo(){
        return executeCdpCommand("SystemInfo.getInfo", Map.of(), defaultDuration);
    }
}