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
    private Duration DEFAULT_TIMEOUT = Duration.ofMillis(50);
    private final CdpUtility cdpUtility;

    public CdpDriver(String websocketDebuggerAddress) {
        this.cdpUtility = new CdpUtility(websocketDebuggerAddress);
        init();
    }

    private void init() {
        cdpUtility.runtimeEvaluate(CdpScripts.CDP_ELEMENTS_CLEANUP_SCRIPT, false);
    }

    public void setPollingInterval(Duration POLLING_INTERVAL) {
        this.POLLING_INTERVAL = POLLING_INTERVAL;
    }

    public Duration getPollingInterval() {
        return POLLING_INTERVAL;
    }

    public void setDefaultTimeout(Duration DEFAULT_TIMEOUT) {
        this.DEFAULT_TIMEOUT = DEFAULT_TIMEOUT;
    }

    public Duration getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    protected CdpUtility getCdpUtility() {
        return cdpUtility;
    }

    public void closeTab(){
        cdpUtility.pageClose();
        System.out.println("Page/Tab closed");
    }

    public void closeBrowser(){
        cdpUtility.browserClose();
        System.out.println("Browser closed");
    }

    public boolean isElementPresent(CdpBy by) {
        return !findElements(by, Duration.ofSeconds(1)).isEmpty();
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
            Utilities.sleep(Duration.ofSeconds(1));
            return false;
        }, duration);
        return cdpElements.get();
    }

    @Override
    public void close() {
        cdpUtility.close();
        System.out.println("Closed websocket connection.");
    }
}
