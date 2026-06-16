package w3c.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Ole32;
import logger.Log;
import logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class W3CWebDriverServer {
    private static final Logger log = Log.getLogger(W3CWebDriverServer.class);
    private final int port;
    private HttpServer server;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static class Session {
        public final String id;
        public final Pointer window;
        public final Map<String, Pointer> elements = new ConcurrentHashMap<>();

        public Session(String id, Pointer window) {
            this.id = id;
            this.window = window;
        }
    }

    public W3CWebDriverServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", new RouterHandler());
            server.setExecutor(Executors.newFixedThreadPool(5));
            server.start();
            log.trace("W3C WebDriver Server started on port " + port);
        } catch (IOException e) {
            log.error("Failed to start W3C WebDriver Server on port " + port, e);
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("W3C WebDriver Server stopped");
        }
    }

    private class RouterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            log.trace("Received request: " + method + " " + path);

            // CORS headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            com.sun.jna.platform.win32.WinNT.HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
            try {
                JsonNode body = null;
                if ("POST".equalsIgnoreCase(method)) {
                    try (InputStream is = exchange.getRequestBody()) {
                        if (is.available() > 0 || exchange.getRequestHeaders().getFirst("Content-Length") != null) {
                            body = mapper.readTree(is);
                        }
                    } catch (Exception e) {
                        // ignore body parse exceptions
                    }
                }

                RouteResult result = route(method, path, body);
                sendResponse(exchange, result.status, result.responseBody);
            } catch (Exception e) {
                log.error("Error handling request: " + method + " " + path, e);
                Map<String, Object> errResponse = Map.of(
                    "value", Map.of(
                        "error", "unknown error",
                        "message", e.getMessage() != null ? e.getMessage() : e.toString()
                    )
                );
                sendResponse(exchange, 500, mapper.writeValueAsString(errResponse));
            } finally {
                if (hr.intValue() >= 0) {
                    Ole32.INSTANCE.CoUninitialize();
                }
            }
        }
    }

    private static class RouteResult {
        public final int status;
        public final String responseBody;

        public RouteResult(int status, String responseBody) {
            this.status = status;
            this.responseBody = responseBody;
        }
    }

    private RouteResult route(String method, String path, JsonNode body) throws Exception {
        // 1. POST /session
        if ("POST".equalsIgnoreCase(method) && path.matches("^/session$")) {
            String app = "Calculator";
            if (body != null && body.has("capabilities")) {
                JsonNode caps = body.get("capabilities");
                if (caps.has("alwaysMatch")) {
                    JsonNode match = caps.get("alwaysMatch");
                    if (match.has("app")) {
                        app = match.get("app").asText();
                    }
                }
            }

            log.info("Creating session for application: " + app);
            Pointer window = null;
            Pointer root = UIA.getRootElement();
            if (root != null) {
                Pointer condition = UIA.createPropertyCondition(UIA.UIA_NamePropertyId, app);
                if (condition != null) {
                    window = UIA.findFirst(root, UIA.TreeScope_Children, condition);
                    UIA.release(condition);
                }
                UIA.release(root);
            }

            if (window == null && (app.toLowerCase().endsWith(".exe") || app.contains("/") || app.contains("\\"))) {
                try {
                    log.info("Window not found. Attempting to launch executable: " + app);
                    Runtime.getRuntime().exec(app);
                    
                    // Extract name of executable to search for window title
                    String searchName = app;
                    int lastSlash = Math.max(app.lastIndexOf('/'), app.lastIndexOf('\\'));
                    if (lastSlash >= 0) {
                        searchName = app.substring(lastSlash + 1);
                    }
                    if (searchName.toLowerCase().endsWith(".exe")) {
                        searchName = searchName.substring(0, searchName.length() - 4);
                    }
                    
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(500);
                        root = UIA.getRootElement();
                        if (root != null) {
                            Pointer condition = UIA.createPropertyCondition(UIA.UIA_NamePropertyId, app);
                            if (condition != null) {
                                window = UIA.findFirst(root, UIA.TreeScope_Children, condition);
                                UIA.release(condition);
                            }
                            if (window == null && !searchName.equalsIgnoreCase(app)) {
                                Pointer cond2 = UIA.createPropertyCondition(UIA.UIA_NamePropertyId, searchName);
                                if (cond2 != null) {
                                    window = UIA.findFirst(root, UIA.TreeScope_Children, cond2);
                                    UIA.release(cond2);
                                }
                            }
                            UIA.release(root);
                        }
                        if (window != null) {
                            log.info("Successfully found window after launching: " + app);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to launch executable: " + app, ex);
                }
            }

            if (window == null) {
                return errorResult(404, "no such window", "Could not locate window with title: " + app);
            }

            String sessionId = UUID.randomUUID().toString();
            Session session = new Session(sessionId, window);
            sessions.put(sessionId, session);

            Map<String, Object> resp = Map.of(
                "sessionId", sessionId,
                "capabilities", Map.of(
                    "platformName", "windows",
                    "app", app
                )
            );
            return successResult(resp);
        }

        Pattern sessionPattern = Pattern.compile("^/session/([^/]+)(.*)$");
        Matcher sessionMatcher = sessionPattern.matcher(path);
        if (!sessionMatcher.matches()) {
            return errorResult(404, "unknown command", "Request path not recognized: " + path);
        }

        String sessionId = sessionMatcher.group(1);
        String subPath = sessionMatcher.group(2);
        Session session = sessions.get(sessionId);
        if (session == null) {
            return errorResult(404, "invalid session id", "Session not found: " + sessionId);
        }

        // 2. DELETE /session/:sessionId
        if ("DELETE".equalsIgnoreCase(method) && subPath.isEmpty()) {
            sessions.remove(sessionId);
            try {
                UIA.closeWindow(session.window);
            } catch (Exception e) {
                // ignore
            }
            for (Pointer elem : session.elements.values()) {
                UIA.release(elem);
            }
            UIA.release(session.window);
            return successResult(null);
        }

        // 3. POST /session/:sessionId/window/maximize
        if ("POST".equalsIgnoreCase(method) && subPath.equals("/window/maximize")) {
            UIA.setWindowVisualState(session.window, 1); // Maximize
            return successResult(null);
        }

        // 4. POST /session/:sessionId/window/minimize
        if ("POST".equalsIgnoreCase(method) && subPath.equals("/window/minimize")) {
            UIA.setWindowVisualState(session.window, 2); // Minimize
            return successResult(null);
        }

        // 5. POST /session/:sessionId/window/focus
        if ("POST".equalsIgnoreCase(method) && subPath.equals("/window/focus")) {
            UIA.setFocus(session.window);
            return successResult(null);
        }

        // 6. DELETE /session/:sessionId/window
        if ("DELETE".equalsIgnoreCase(method) && subPath.equals("/window")) {
            UIA.closeWindow(session.window);
            return successResult(null);
        }

        // 7. POST /session/:sessionId/element
        if ("POST".equalsIgnoreCase(method) && subPath.equals("/element")) {
            String using = body.get("using").asText();
            String value = body.get("value").asText();

            Pointer element = findControl(session.window, using, value);
            if (element == null) {
                return errorResult(404, "no such element", "Element not found using: " + using + " = " + value);
            }

            String elementId = UUID.randomUUID().toString();
            session.elements.put(elementId, element);

            Map<String, Object> resp = Map.of(
                "element-6066-11e4-a52e-4f735466cecf", elementId
            );
            return successResult(resp);
        }

        Pattern elementPattern = Pattern.compile("^/element/([^/]+)(.*)$");
        Matcher elementMatcher = elementPattern.matcher(subPath);
        if (!elementMatcher.matches()) {
            return errorResult(404, "unknown command", "Request subpath not recognized: " + subPath);
        }

        String elementId = elementMatcher.group(1);
        String elemSubPath = elementMatcher.group(2);
        Pointer control = session.elements.get(elementId);
        if (control == null) {
            return errorResult(404, "no such element", "Element not found in registry: " + elementId);
        }

        // 8. POST /session/:sessionId/element/:elementId/element
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/element")) {
            String using = body.get("using").asText();
            String value = body.get("value").asText();

            Pointer child = findControl(control, using, value);
            if (child == null) {
                return errorResult(404, "no such element", "Child element not found using: " + using + " = " + value);
            }

            String childId = UUID.randomUUID().toString();
            session.elements.put(childId, child);

            Map<String, Object> resp = Map.of(
                "element-6066-11e4-a52e-4f735466cecf", childId
            );
            return successResult(resp);
        }

        // 9. POST /session/:sessionId/element/:elementId/click
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/click")) {
            clickControl(control);
            return successResult(null);
        }

        // 10. POST /session/:sessionId/element/:elementId/value
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/value")) {
            String text = "";
            if (body.has("text")) {
                text = body.get("text").asText();
            } else if (body.has("value")) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode valNode : body.get("value")) {
                    sb.append(valNode.asText());
                }
                text = sb.toString();
            }
            setValueControl(control, text);
            return successResult(null);
        }

        // 11. GET /session/:sessionId/element/:elementId/text
        if ("GET".equalsIgnoreCase(method) && elemSubPath.equals("/text")) {
            String val = getTextControl(control);
            return successResult(val);
        }

        // 12. GET /session/:sessionId/element/:elementId/selected
        if ("GET".equalsIgnoreCase(method) && elemSubPath.equals("/selected")) {
            boolean selected = isSelectedControl(control);
            return successResult(selected);
        }

        // 13. POST /session/:sessionId/element/:elementId/expand
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/expand")) {
            UIA.expand(control);
            return successResult(null);
        }

        // 14. POST /session/:sessionId/element/:elementId/collapse
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/collapse")) {
            UIA.collapse(control);
            return successResult(null);
        }

        // 15. POST /session/:sessionId/element/:elementId/selectTabPage
        if ("POST".equalsIgnoreCase(method) && elemSubPath.equals("/selectTabPage")) {
            String pageName = body.get("name").asText();
            Pointer condition = UIA.createPropertyCondition(UIA.UIA_NamePropertyId, pageName);
            if (condition != null) {
                Pointer tabPage = UIA.findFirst(control, UIA.TreeScope_Descendants, condition);
                if (tabPage != null) {
                    UIA.selectItem(tabPage);
                    UIA.release(tabPage);
                }
                UIA.release(condition);
            }
            return successResult(null);
        }

        // 16. GET /session/:sessionId/element/:elementId/rowcount
        if ("GET".equalsIgnoreCase(method) && elemSubPath.equals("/rowcount")) {
            int rows = UIA.getGridRowCount(control);
            return successResult(rows);
        }

        // 17. GET /session/:sessionId/element/:elementId/cell/:row/:col
        Pattern cellPattern = Pattern.compile("^/cell/(\\d+)/(\\d+)$");
        Matcher cellMatcher = cellPattern.matcher(elemSubPath);
        if ("GET".equalsIgnoreCase(method) && cellMatcher.matches()) {
            int row = Integer.parseInt(cellMatcher.group(1));
            int col = Integer.parseInt(cellMatcher.group(2));
            String cellVal = "";
            Pointer cell = UIA.getGridItem(control, row, col);
            if (cell != null) {
                cellVal = getTextControl(cell);
                UIA.release(cell);
            }
            return successResult(cellVal);
        }

        return errorResult(404, "unknown command", "Request path not recognized: " + path);
    }

    private Pointer findControl(Pointer parent, String using, String value) {
        int propertyId;
        switch (using) {
            case "accessibility id", "id":
                propertyId = UIA.UIA_AutomationIdPropertyId;
                break;
            case "class name":
                propertyId = UIA.UIA_ClassNamePropertyId;
                break;
            case "name":
                propertyId = UIA.UIA_NamePropertyId;
                break;
            default:
                throw new IllegalArgumentException("Unsupported locator strategy: " + using);
        }

        Pointer condition = UIA.createPropertyCondition(propertyId, value);
        if (condition == null) return null;
        try {
            return UIA.findFirst(parent, UIA.TreeScope_Descendants, condition);
        } finally {
            UIA.release(condition);
        }
    }

    private void clickControl(Pointer control) {
        Pointer selectPattern = UIA.getPattern(control, UIA.UIA_SelectionItemPatternId, UIA.IID_IUIAutomationSelectionItemPattern);
        if (selectPattern != null) {
            UIA.release(selectPattern);
            UIA.selectItem(control);
            return;
        }

        Pointer togglePattern = UIA.getPattern(control, UIA.UIA_TogglePatternId, UIA.IID_IUIAutomationTogglePattern);
        if (togglePattern != null) {
            UIA.release(togglePattern);
            UIA.toggle(control);
            return;
        }

        UIA.invoke(control);
    }

    private void setValueControl(Pointer control, String value) {
        UIA.setValue(control, value);
    }

    private String getTextControl(Pointer control) {
        Pointer valuePattern = UIA.getPattern(control, UIA.UIA_ValuePatternId, UIA.IID_IUIAutomationValuePattern);
        if (valuePattern != null) {
            UIA.release(valuePattern);
            return UIA.getValue(control);
        }
        return UIA.getElementName(control);
    }

    private boolean isSelectedControl(Pointer control) {
        Pointer togglePattern = UIA.getPattern(control, UIA.UIA_TogglePatternId, UIA.IID_IUIAutomationTogglePattern);
        if (togglePattern != null) {
            UIA.release(togglePattern);
            return UIA.getToggleState(control);
        }
        Pointer selectPattern = UIA.getPattern(control, UIA.UIA_SelectionItemPatternId, UIA.IID_IUIAutomationSelectionItemPattern);
        if (selectPattern != null) {
            UIA.release(selectPattern);
            return UIA.isItemSelected(control);
        }
        return false;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private RouteResult successResult(Object value) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", value);
        return new RouteResult(200, mapper.writeValueAsString(map));
    }

    private RouteResult errorResult(int status, String error, String message) throws IOException {
        Map<String, Object> body = Map.of(
            "value", Map.of(
                "error", error,
                "message", message
            )
        );
        return new RouteResult(status, mapper.writeValueAsString(body));
    }
}
