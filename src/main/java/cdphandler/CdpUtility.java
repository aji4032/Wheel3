package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CdpUtility {
    private final CdpClient client;
    private final ApiInterceptor apiInterceptor;
    public Duration defaultDuration = Duration.ofSeconds(120);

    public CdpUtility(String websocketDebuggerAddress) {
        this.client = new CdpClient(websocketDebuggerAddress);
        this.apiInterceptor = new ApiInterceptor(client);
        client.addEventListener(evt -> System.out.println("Event: " + evt.toString()));
    }

    private JsonNode executeCdpCommand(String command, Map<String, Object> map) {
        return executeCdpCommand(command, map, defaultDuration);
    }

    private JsonNode executeCdpCommand(String command, Map<String, Object> map, Duration timeout) {
        try {
            JsonNode result = client.sendCommand(command, map, timeout);
            System.out.println(command + " invoked: " + map + "; result: " + result);
            return result.get("result");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode browserGetVersion() {
        return executeCdpCommand("Browser.getVersion", Map.of(), defaultDuration);
    }

    public JsonNode browserClose() {
        return executeCdpCommand("Browser.close", Map.of(), defaultDuration);
    }

    public JsonNode domDescribeNode(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.describeNode", map, defaultDuration);
    }

    public JsonNode domDisable() {
        return executeCdpCommand("DOM.disable", Map.of(), defaultDuration);
    }

    public JsonNode domEnable() {
        return executeCdpCommand("DOM.enable", Map.of(), defaultDuration);
    }

    public JsonNode domFocus(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.focus", map, defaultDuration);
    }

    public JsonNode domGetAttributes(int nodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        return executeCdpCommand("DOM.getAttributes", map, defaultDuration);
    }

    public JsonNode domGetBoxModel(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.getBoxModel", map, defaultDuration);
    }

    public JsonNode domGetDocument() {
        return executeCdpCommand("DOM.getDocument", Map.of(), defaultDuration);
    }

    public JsonNode domGetNodeForLocation (int x, int y) {
        Map<String, Object> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        return executeCdpCommand("DOM.getNodeForLocation", map, defaultDuration);
    }

    public JsonNode domGetOuterHtml(int backendNodeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("backendNodeId", backendNodeId);
        return executeCdpCommand("DOM.getOuterHTML", map, defaultDuration);
    }

    public JsonNode domQuerySelector(int nodeId, String selector) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        map.put("selector", selector);
        return executeCdpCommand("DOM.querySelector", map, defaultDuration);
    }

    public JsonNode domQuerySelectorAll(int nodeId, String selector) {
        Map<String, Object> map = new HashMap<>();
        map.put("nodeId", nodeId);
        map.put("selector", selector);
        return executeCdpCommand("DOM.querySelectorAll", map, defaultDuration);
    }

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

    public JsonNode logClear() {
        return executeCdpCommand("Log.clear", Map.of(), defaultDuration);
    }

    public JsonNode logDisable() {
        return executeCdpCommand("Log.disable", Map.of(), defaultDuration);
    }

    public JsonNode logEnable() {
        return executeCdpCommand("Log.enable", Map.of(), defaultDuration);
    }

    public JsonNode networkClearBrowserCache(){
        return executeCdpCommand("Network.clearBrowserCache", Map.of(), defaultDuration);
    }

    public JsonNode networkClearBrowserCookies(){
        return executeCdpCommand("Network.clearBrowserCookies", Map.of(), defaultDuration);
    }

    public JsonNode networkDisable(){
        return executeCdpCommand("Network.disable", Map.of(), defaultDuration);
    }

    public JsonNode networkEnable(){
        return executeCdpCommand("Network.enable", Map.of(), defaultDuration);
    }

    public JsonNode overlayDisable(){
        return executeCdpCommand("Overlay.disable", Map.of(), defaultDuration);
    }

    public JsonNode overlayEnable(){
        return executeCdpCommand("Overlay.enable", Map.of(), defaultDuration);
    }

    public JsonNode overlayHideHighlight(){
        return executeCdpCommand("Overlay.hideHighlight", Map.of(), defaultDuration);
    }

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

    public JsonNode pageBringToFront() {
        return executeCdpCommand("Page.bringToFront", Map.of(), defaultDuration);
    }

    public JsonNode pageCaptureScreenshot(String format) {
        Map<String, Object> map = new HashMap<>();
        map.put("format", format);
        return executeCdpCommand("Page.captureScreenshot", map, defaultDuration);
    }

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

    public JsonNode pageClose() {
        return executeCdpCommand("Page.close", Map.of(), defaultDuration);
    }

    public JsonNode pageDisable() {
        return executeCdpCommand("Page.disable", Map.of(), defaultDuration);
    }

    public JsonNode pageEnable() {
        return executeCdpCommand("Page.enable", Map.of(), defaultDuration);
    }

    public JsonNode pageNavigate(String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("url", url);
        return executeCdpCommand("Page.navigate", map, defaultDuration);
    }

    public JsonNode pageReload() {
        return executeCdpCommand("Page.reload", Map.of(), defaultDuration);
    }

    public JsonNode performanceDisable() {
        return executeCdpCommand("Performance.disable", Map.of(), defaultDuration);
    }

    public JsonNode performanceEnable() {
        return executeCdpCommand("Performance.enable", Map.of(), defaultDuration);
    }

    public JsonNode performanceGetMetrics() {
        return executeCdpCommand("Performance.getMetrics", Map.of(), defaultDuration);
    }

    public JsonNode runtimeDisable(){
        return executeCdpCommand("Runtime.disable", Map.of(), defaultDuration);
    }

    public JsonNode runtimeEnable(){
        return executeCdpCommand("Runtime.enable", Map.of(), defaultDuration);
    }

    public JsonNode runtimeEvaluate(String expression, boolean returnByValue){
        Map<String, Object> map = new HashMap<>();
        map.put("expression", expression);
        map.put("returnByValue", returnByValue);
        return executeCdpCommand("Runtime.evaluate", map, defaultDuration);
    }

    public JsonNode systemInfoGetInfo(){
        return executeCdpCommand("SystemInfo.getInfo", Map.of(), defaultDuration);
    }
}