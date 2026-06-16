package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import logger.Log;
import logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CdpTraceCollector {
    private static final Logger log = Log.getLogger(CdpTraceCollector.class);

    private final ICdpDriver driver;
    private final File zipFile;
    private final File tempDir;
    private final List<Map<String, Object>> actions = new ArrayList<>();
    private final ConcurrentLinkedQueue<Map<String, Object>> consoleLogs = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Map<String, Object>> networkRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Map<String, Object>> pendingRequests = new ConcurrentHashMap<>();

    private boolean isRecording = false;
    private long startTime;
    private Consumer<JsonNode> cdpEventListener;
    private ExecutorService bodyFetchExecutor;

    public CdpTraceCollector(ICdpDriver driver, File zipFile) {
        this.driver = driver;
        this.zipFile = zipFile;
        this.tempDir = new File("target/traces/temp_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8));
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void start() {
        try {
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw new IOException("Failed to create temp trace directory: " + tempDir.getAbsolutePath());
            }
            new File(tempDir, "screenshots").mkdirs();

            this.startTime = System.currentTimeMillis();
            this.isRecording = true;
            // Single-thread executor for off-listener body fetches
            this.bodyFetchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "cdp-body-fetch");
                t.setDaemon(true);
                return t;
            });

            // Register CDP Event Listener for Network and Console
            CdpUtility cdp = driver.getCdpUtility();
            if (cdp != null) {
                CdpClient client = cdp.getClient();
                if (client != null) {
                    cdpEventListener = event -> {
                        if (!isRecording || !event.has("method"))
                            return;
                        try {
                            String method = event.get("method").asText();
                            if ("Runtime.consoleAPICalled".equals(method)) {
                                handleConsoleMessage(event);
                            } else if ("Network.requestWillBeSent".equals(method)) {
                                handleNetworkRequest(event);
                            } else if ("Network.responseReceived".equals(method)) {
                                handleNetworkResponse(event);
                            } else if ("Network.loadingFinished".equals(method)) {
                                handleNetworkLoadingFinished(event);
                            }
                        } catch (Exception e) {
                            // Suppress exceptions in listener callback
                        }
                    };
                    client.addEventListener(cdpEventListener);

                    // Enable domains
                    try {
                        client.sendCommand("Runtime.enable", Duration.ofSeconds(5));
                        client.sendCommand("Console.enable", Duration.ofSeconds(5));
                        client.sendCommand("Network.enable", Duration.ofSeconds(5));
                    } catch (Exception e) {
                        log.warn("Failed to enable Console or Network domain: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to start tracing: " + e.getMessage());
        }
    }

    public void stop() {
        if (!isRecording)
            return;
        isRecording = false;

        // Clean up listeners
        CdpUtility cdp = driver.getCdpUtility();
        if (cdp != null && cdpEventListener != null) {
            try {
                CdpClient client = cdp.getClient();
                if (client != null) {
                    client.removeEventListener(cdpEventListener);
                    client.sendCommand("Runtime.disable", Duration.ofSeconds(5));
                    client.sendCommand("Console.disable", Duration.ofSeconds(5));
                    client.sendCommand("Network.disable", Duration.ofSeconds(5));
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Shut down body-fetch executor and wait for all in-flight fetches to complete
        if (bodyFetchExecutor != null) {
            bodyFetchExecutor.shutdown();
            try {
                if (!bodyFetchExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                    bodyFetchExecutor.shutdownNow();
                    log.warn("Body fetch executor timed out — some response bodies may be missing");
                }
            } catch (InterruptedException e) {
                bodyFetchExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Finalize pending network requests (those that never fired loadingFinished)
        for (Map<String, Object> req : pendingRequests.values()) {
            if (!req.containsKey("status")) {
                req.put("status", "Pending");
            }
            networkRequests.add(req);
        }
        pendingRequests.clear();

        // Write trace data
        try {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("title", zipFile.getName().replace(".zip", ""));
            rootNode.put("startTime", startTime);
            rootNode.put("endTime", endTime);
            rootNode.put("duration", duration);

            ArrayNode actionsArray = rootNode.putArray("actions");
            for (Map<String, Object> action : actions) {
                actionsArray.add(mapper.valueToTree(action));
            }

            ArrayNode consoleArray = rootNode.putArray("consoleLogs");
            for (Map<String, Object> logEntry : consoleLogs) {
                consoleArray.add(mapper.valueToTree(logEntry));
            }

            ArrayNode networkArray = rootNode.putArray("networkRequests");
            for (Map<String, Object> netEntry : networkRequests) {
                networkArray.add(mapper.valueToTree(netEntry));
            }

            String jsonString = mapper.writeValueAsString(rootNode);
            String jsContent = "window.traceData = " + jsonString + ";";

            Files.writeString(new File(tempDir, "trace-data.js").toPath(), jsContent);
            Files.writeString(new File(tempDir, "index.html").toPath(), TraceViewerTemplate.HTML_TEMPLATE);

            // Zip it
            File parent = zipFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            zipDirectory(tempDir, zipFile);
            log.info("Trace successfully exported to ZIP: " + zipFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to write trace output: " + e.getMessage());
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir);
        }
    }

    private static final Set<String> FRAMEWORK_CLASSES = Set.of(
        "cdphandler.ApiInterceptor",
        "cdphandler.ApiResponse",
        "cdphandler.BrowserContext",
        "cdphandler.BrowserLauncher",
        "cdphandler.CdpBy",
        "cdphandler.CdpClient",
        "cdphandler.CdpDimension",
        "cdphandler.CdpDriver",
        "cdphandler.CdpDriverProxy",
        "cdphandler.CdpElement",
        "cdphandler.CdpElementProxy",
        "cdphandler.CdpHandler",
        "cdphandler.CdpKey",
        "cdphandler.CdpLocatorType",
        "cdphandler.CdpPoint",
        "cdphandler.CdpRect",
        "cdphandler.CdpScripts",
        "cdphandler.CdpTraceCollector",
        "cdphandler.CdpUtility",
        "cdphandler.ICdpDriver",
        "cdphandler.ICdpElement",
        "cdphandler.MouseEvent",
        "cdphandler.OllamaProxy",
        "cdphandler.OllamaUtility",
        "cdphandler.TraceViewerTemplate",
        "cdphandler.WebSocketMessage"
    );

    private Map<String, Object> captureSourceCode() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = null;
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith("cdphandler.")) {
                boolean isFramework = false;
                for (String fw : FRAMEWORK_CLASSES) {
                    if (className.equals(fw) || className.startsWith(fw + "$")) {
                        isFramework = true;
                        break;
                    }
                }
                if (isFramework) continue;
            }
            if (className.startsWith("java.") || className.startsWith("javax.") 
                || className.startsWith("com.sun.") || className.startsWith("sun.")
                || className.startsWith("jdk.internal.") || className.startsWith("org.testng.")
                || className.startsWith("org.junit.") || className.contains("$$FastClassBy")
                || className.contains("Proxy") || className.contains("reflect.Method")
                || className.contains("MethodAccessor")) {
                continue;
            }
            caller = element;
            break;
        }

        if (caller == null) {
            return null;
        }

        String className = caller.getClassName();
        String fileName = caller.getFileName();
        int lineNumber = caller.getLineNumber();
        if (fileName == null || lineNumber <= 0) {
            return null;
        }

        String packagePath = className.contains(".") ? className.substring(0, className.lastIndexOf('.')).replace('.', '/') : "";
        File srcFile = new File("src/test/java", packagePath + "/" + fileName);
        if (!srcFile.exists()) {
            srcFile = new File("src/main/java", packagePath + "/" + fileName);
        }
        if (!srcFile.exists()) {
            srcFile = findSourceFile(new File("src"), fileName);
        }
        if (srcFile == null || !srcFile.exists()) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(srcFile.toPath());
            int start = Math.max(1, lineNumber - 5);
            int end = Math.min(lines.size(), lineNumber + 5);

            List<Map<String, Object>> snippetLines = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                Map<String, Object> lineMap = new LinkedHashMap<>();
                lineMap.put("line", i);
                lineMap.put("content", lines.get(i - 1));
                snippetLines.add(lineMap);
            }

            Map<String, Object> sourceInfo = new LinkedHashMap<>();
            sourceInfo.put("file", srcFile.getPath().replace('\\', '/'));
            sourceInfo.put("line", lineNumber);
            sourceInfo.put("method", caller.getMethodName());
            sourceInfo.put("snippet", snippetLines);
            return sourceInfo;
        } catch (Exception e) {
            log.warn("Failed to read source file for stack trace: " + e.getMessage());
            return null;
        }
    }

    private File findSourceFile(File root, String fileName) {
        if (!root.exists() || !root.isDirectory()) return null;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findSourceFile(f, fileName);
                if (found != null) return found;
            } else if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return null;
    }

    public <T> T record(String name, String type, String target, CdpRect elementRect, Object[] args,
            Supplier<T> action) {
        if (!isRecording) {
            return action.get();
        }

        Map<String, Object> traceAction = new LinkedHashMap<>();
        String actionId = "action_" + (actions.size() + 1);
        traceAction.put("id", actionId);
        traceAction.put("name", name);
        traceAction.put("type", type);
        traceAction.put("target", target);
        traceAction.put("startTime", System.currentTimeMillis());

        Map<String, Object> sourceCode = captureSourceCode();
        if (sourceCode != null) {
            traceAction.put("sourceCode", sourceCode);
        }

        if (elementRect != null) {
            Map<String, Integer> rectMap = new HashMap<>();
            rectMap.put("x", elementRect.point().x());
            rectMap.put("y", elementRect.point().y());
            rectMap.put("width", elementRect.dimension().width());
            rectMap.put("height", elementRect.dimension().height());
            traceAction.put("elementRect", rectMap);
        }

        if (args != null && args.length > 0) {
            List<String> argStrings = new ArrayList<>();
            for (Object arg : args) {
                argStrings.add(arg != null ? arg.toString() : "null");
            }
            traceAction.put("args", argStrings);
        }

        boolean isModifying = isStateModifying(name);
        if (isModifying) {
            traceAction.put("screenshotBefore", captureScreenshot(actionId + "_before"));
            traceAction.put("pageSourceBefore", capturePageSource());
        }

        T result;
        try {
            result = action.get();
            traceAction.put("status", "success");
        } catch (Throwable t) {
            traceAction.put("status", "failed");
            traceAction.put("error", t.getMessage());
            throw t;
        } finally {
            long endTime = System.currentTimeMillis();
            traceAction.put("endTime", endTime);
            traceAction.put("duration", endTime - ((Long) traceAction.get("startTime")));

            if (isModifying || "failed".equals(traceAction.get("status"))) {
                traceAction.put("screenshotAfter", captureScreenshot(actionId + "_after"));
                traceAction.put("pageSourceAfter", capturePageSource());
            }

            actions.add(traceAction);
        }
        return result;
    }

    public void record(String name, String type, String target, CdpRect elementRect, Object[] args, Runnable action) {
        record(name, type, target, elementRect, args, () -> {
            action.run();
            return null;
        });
    }

    private boolean isStateModifying(String actionName) {
        return List.of("get", "back", "forward", "refresh", "click", "doubleClick", "dragDrop", "clear", "sendKeys",
                "keyPress", "keyDown", "keyUp", "scrollBy").contains(actionName);
    }

    private String captureScreenshot(String filename) {
        try {
            String base64 = driver.captureScreenshot();
            if (base64 == null || base64.isEmpty())
                return null;
            byte[] bytes = Base64.getDecoder().decode(base64);
            File file = new File(tempDir, "screenshots/" + filename + ".png");
            Files.write(file.toPath(), bytes);
            return "screenshots/" + filename + ".png";
        } catch (Exception e) {
            return null;
        }
    }

    private String capturePageSource() {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleConsoleMessage(JsonNode event) {
        // Runtime.consoleAPICalled - modern Chrome event
        JsonNode params = event.path("params");
        if (params.isMissingNode())
            return;

        String type = params.path("type").asText("log");
        // Build message from args array
        StringBuilder sb = new StringBuilder();
        JsonNode args = params.path("args");
        if (args.isArray()) {
            for (JsonNode arg : args) {
                if (sb.length() > 0)
                    sb.append(" ");
                String argType = arg.path("type").asText("");
                if ("string".equals(argType)) {
                    sb.append(arg.path("value").asText(""));
                } else if ("number".equals(argType)) {
                    sb.append(arg.path("value").asText(""));
                } else if ("boolean".equals(argType)) {
                    sb.append(arg.path("value").asText(""));
                } else if ("undefined".equals(argType)) {
                    sb.append("undefined");
                } else if ("null".equals(argType)) {
                    sb.append("null");
                } else {
                    // object, array, etc — use description if available
                    String desc = arg.path("description").asText(null);
                    sb.append(desc != null ? desc : arg.path("value").asText("[Object]"));
                }
            }
        }

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("level", type);
        logEntry.put("message", sb.toString());
        logEntry.put("timestamp", System.currentTimeMillis());
        consoleLogs.add(logEntry);
    }

    private void handleNetworkRequest(JsonNode event) {
        JsonNode params = event.path("params");
        String requestId = params.path("requestId").asText(null);
        if (requestId == null)
            return;

        JsonNode request = params.path("request");
        if (request.isMissingNode())
            return;

        Map<String, Object> reqEntry = new LinkedHashMap<>();
        reqEntry.put("requestId", requestId);
        reqEntry.put("url", request.path("url").asText(""));
        reqEntry.put("method", request.path("method").asText("GET"));
        reqEntry.put("type", params.path("type").asText("fetch"));
        reqEntry.put("timestamp", System.currentTimeMillis());

        pendingRequests.put(requestId, reqEntry);
    }

    private void handleNetworkResponse(JsonNode event) {
        JsonNode params = event.path("params");
        String requestId = params.path("requestId").asText(null);
        if (requestId == null) return;

        JsonNode response = params.path("response");
        if (response.isMissingNode()) return;

        Map<String, Object> reqEntry = pendingRequests.get(requestId);
        if (reqEntry != null) {
            reqEntry.put("status", response.path("status").asInt(200));
            reqEntry.put("mimeType", response.path("mimeType").asText(""));
            // Capture response headers as a flat map
            JsonNode headersNode = response.path("headers");
            if (!headersNode.isMissingNode()) {
                Map<String, String> headers = new LinkedHashMap<>();
                headersNode.fields().forEachRemaining(e -> headers.put(e.getKey(), e.getValue().asText()));
                reqEntry.put("responseHeaders", headers);
            }
        }
    }

    /** Fetches the response body for XHR/Fetch requests once loading is complete. */
    private void handleNetworkLoadingFinished(JsonNode event) {
        JsonNode params = event.path("params");
        String requestId = params.path("requestId").asText(null);
        if (requestId == null) return;

        Map<String, Object> reqEntry = pendingRequests.remove(requestId);
        if (reqEntry == null) return;

        // Only fetch body for XHR and Fetch — skip images, scripts, stylesheets, etc.
        String type = String.valueOf(reqEntry.getOrDefault("type", "")).toLowerCase();
        if (("xhr".equals(type) || "fetch".equals(type)) && bodyFetchExecutor != null && !bodyFetchExecutor.isShutdown()) {
            // Must NOT call sendCommand from within the event listener (WebSocket receive thread),
            // as it would deadlock waiting for a reply that the blocked thread cannot read.
            // Submit to the dedicated executor thread instead.
            bodyFetchExecutor.submit(() -> {
                try {
                    CdpClient client = driver.getCdpUtility().getClient();
                    JsonNode bodyResult = client.sendCommand(
                            "Network.getResponseBody",
                            Map.of("requestId", requestId),
                            Duration.ofSeconds(10));
                    if (bodyResult != null && bodyResult.has("body")) {
                        String body = bodyResult.path("body").asText("");
                        boolean base64Encoded = bodyResult.path("base64Encoded").asBoolean(false);
                        if (base64Encoded) {
                            reqEntry.put("responseBody", "[base64 encoded binary]");
                        } else if (body.length() > 262144) {
                            reqEntry.put("responseBody", body.substring(0, 262144) + "\n... [truncated]");
                        } else {
                            reqEntry.put("responseBody", body);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch response body for request " + requestId + ": " + e.getMessage());
                } finally {
                    networkRequests.add(reqEntry);
                }
            });
        } else {
            networkRequests.add(reqEntry);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
            zipDirectoryInternal(sourceDir, sourceDir, zos);
        }
    }

    private void zipDirectoryInternal(File rootDir, File currentDir, java.util.zip.ZipOutputStream zos)
            throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null)
            return;
        byte[] buffer = new byte[4096];
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryInternal(rootDir, file, zos);
            } else {
                String relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(relativePath);
                zos.putNextEntry(zipEntry);
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
