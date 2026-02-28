package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.Log;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers and launches a headless Chromium-based browser with CDP enabled.
 * <p>
 * Typical usage:
 * 
 * <pre>
 * LaunchedBrowser browser = BrowserLauncher.launch();
 * String pageWs = BrowserLauncher.getFirstPageWsUrl(browser.port());
 * ICdpDriver driver = CdpHandler.createDriver(pageWs);
 * // … use driver …
 * driver.close();
 * browser.close();
 * </pre>
 */
public final class BrowserLauncher {

    private BrowserLauncher() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Launch a headless Chrome on a random OS-assigned port.
     */
    public static LaunchedBrowser launch() {
        return launch(0);
    }

    /**
     * Launch a headless Chrome on the given debugging port (0 = auto-assign).
     */
    public static LaunchedBrowser launch(int port) {
        String chromePath = findChrome();
        Path userDataDir;
        try {
            userDataDir = Files.createTempDirectory("wheel3-chrome-profile-");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp user-data-dir", e);
        }

        List<String> command = new ArrayList<>();
        command.add(chromePath);
        command.add("--headless=new");
        command.add("--remote-debugging-port=" + port);
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-gpu");
        command.add("--disable-extensions");
        command.add("--disable-popup-blocking");
        command.add("--disable-translate");
        command.add("--no-sandbox");
        command.add("--disable-dev-shm-usage");
        command.add("--user-data-dir=" + userDataDir.toAbsolutePath());

        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to launch Chrome: " + chromePath, e);
        }

        // Chrome writes "DevTools listening on
        // ws://127.0.0.1:<port>/devtools/browser/<id>"
        // to stderr. We read it to discover the actual port and browser WS URL.
        String wsUrl = readWsUrlFromStderr(process, Duration.ofSeconds(30));
        int actualPort = parsePort(wsUrl);

        Log.info("Chrome launched (pid=" + process.pid() + ") on port " + actualPort);
        return new LaunchedBrowser(process, wsUrl, actualPort, userDataDir);
    }

    /**
     * Queries {@code http://127.0.0.1:<port>/json} and returns the
     * {@code webSocketDebuggerUrl} of the first page target.
     * If no page target exists yet, creates one via {@code /json/new}.
     */
    public static String getFirstPageWsUrl(int port) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            // Try /json to list existing targets
            HttpRequest listReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/json"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> listResp = client.send(listReq, HttpResponse.BodyHandlers.ofString());
            JsonNode targets = MAPPER.readTree(listResp.body());

            // Find first "page" target
            if (targets.isArray()) {
                for (JsonNode target : targets) {
                    if ("page".equals(target.path("type").asText())) {
                        String ws = target.path("webSocketDebuggerUrl").asText();
                        if (!ws.isEmpty()) {
                            return ws;
                        }
                    }
                }
            }

            // No page target found — create one
            HttpRequest newReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/json/new"))
                    .timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> newResp = client.send(newReq, HttpResponse.BodyHandlers.ofString());
            JsonNode newTarget = MAPPER.readTree(newResp.body());
            String ws = newTarget.path("webSocketDebuggerUrl").asText();
            if (!ws.isEmpty()) {
                return ws;
            }

            throw new RuntimeException("Could not obtain a page WebSocket URL from Chrome on port " + port);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Failed to query Chrome debug endpoints on port " + port, e);
        }
    }

    // -----------------------------------------------------------------------
    // Chrome discovery
    // -----------------------------------------------------------------------

    /**
     * Returns the path to a Chrome/Chromium executable.
     * Checks {@code CHROME_PATH} env variable first, then common install locations.
     */
    public static String findChrome() {
        // 1) Environment variable override
        String envPath = System.getenv("CHROME_PATH");
        if (envPath != null && new File(envPath).canExecute()) {
            return envPath;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates = new ArrayList<>();

        if (os.contains("win")) {
            // Windows
            String localAppData = System.getenv("LOCALAPPDATA");
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (localAppData != null) {
                candidates.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");
                candidates.add(localAppData + "\\Chromium\\Application\\chrome.exe");
            }
            if (programFiles != null) {
                candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
                candidates.add(programFiles + "\\Microsoft\\Edge\\Application\\msedge.exe");
            }
            if (programFilesX86 != null) {
                candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
                candidates.add(programFilesX86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
            }
        } else if (os.contains("mac")) {
            // macOS
            candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            candidates.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
            candidates.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
        } else {
            // Linux
            candidates.add("google-chrome");
            candidates.add("google-chrome-stable");
            candidates.add("chromium-browser");
            candidates.add("chromium");
            candidates.add("microsoft-edge");
        }

        for (String candidate : candidates) {
            if (isExecutable(candidate)) {
                return candidate;
            }
        }

        throw new RuntimeException(
                "Chrome/Chromium not found. Install Chrome or set the CHROME_PATH environment variable. " +
                        "Searched: " + candidates);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static boolean isExecutable(String path) {
        // For Linux, a bare command name (no path separator) needs a "which" check
        if (!path.contains(File.separator) && !path.contains("/")) {
            try {
                Process p = new ProcessBuilder("which", path)
                        .redirectErrorStream(true)
                        .start();
                boolean found = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
                p.destroyForcibly();
                return found;
            } catch (Exception e) {
                return false;
            }
        }
        return new File(path).canExecute();
    }

    private static final Pattern WS_URL_PATTERN = Pattern.compile("DevTools listening on (ws://\\S+)");

    private static String readWsUrlFromStderr(Process process, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    line = reader.readLine();
                    if (line == null)
                        break;
                    Matcher matcher = WS_URL_PATTERN.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } else {
                    // Brief sleep to avoid busy-waiting
                    Thread.sleep(50);
                }
                if (!process.isAlive()) {
                    throw new RuntimeException(
                            "Chrome process exited prematurely (exit code " + process.exitValue() + ")");
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Error reading Chrome stderr", e);
        }

        process.destroyForcibly();
        throw new RuntimeException("Timed out waiting for Chrome DevTools WebSocket URL (timeout=" + timeout + ")");
    }

    private static int parsePort(String wsUrl) {
        // ws://127.0.0.1:PORT/devtools/browser/...
        try {
            URI uri = URI.create(wsUrl);
            return uri.getPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse port from WebSocket URL: " + wsUrl, e);
        }
    }

    // -----------------------------------------------------------------------
    // LaunchedBrowser record
    // -----------------------------------------------------------------------

    /**
     * Represents a launched headless Chrome process.
     */
    public record LaunchedBrowser(Process process, String wsUrl, int port, Path userDataDir)
            implements AutoCloseable {

        /**
         * Kills the Chrome process and cleans up the temporary user-data directory.
         */
        @Override
        public void close() {
            try {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            // Best-effort cleanup of temp profile directory
            try {
                deleteRecursively(userDataDir.toFile());
            } catch (Exception ignored) {
            }
            Log.info("Chrome process (pid=" + process.pid() + ") terminated and temp dir cleaned up");
        }

        private static void deleteRecursively(File file) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            file.delete();
        }
    }
}
