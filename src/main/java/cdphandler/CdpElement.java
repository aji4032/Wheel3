package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import tools.Utilities;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CdpElement {
    private final CdpDriver cdpDriver;
    private final CdpBy by;
    private final String referenceId;
    private final CdpElement parentElement;

    public CdpDriver getCdpDriver() {
        return cdpDriver;
    }

    private CdpElement(){
        this.cdpDriver = null;
        this.by = null;
        this.referenceId = null;
        this.parentElement = null;
    }

    public CdpElement(CdpDriver cdpDriver, CdpBy by, String referenceId) {
        this.cdpDriver = cdpDriver;
        this.by = by;
        this.referenceId = referenceId;
        this.parentElement = null;
    }

    private CdpElement(CdpElement parentElement, CdpBy by, String referenceId) {
        this.parentElement = parentElement;
        this.cdpDriver = parentElement.getCdpDriver();
        this.by = by;
        this.referenceId = referenceId;
    }

    @Override
    public String toString(){
        return (parentElement == null ? "" : parentElement + " --> ") + by.toString();
    }

    public boolean isElementPresent(CdpBy by) {
        return !findElements(by, Duration.ofSeconds(1)).isEmpty();
    }

    public CdpElement findElement(CdpBy by) {
        return findElement(by, this.cdpDriver.getDefaultTimeout());
    }

    public CdpElement findElement(CdpBy by, Duration duration) {
        List<CdpElement> elements = findElements(by, duration);
        if(elements.isEmpty()) {
            System.err.printf("Failed to find element: %s%n", this + " --> " + by);
            System.exit(1);
        }
        return elements.getFirst();
    }

    public List<CdpElement> findElements(CdpBy by) {
        return findElements(by, this.cdpDriver.getDefaultTimeout());
    }

    public List<CdpElement> findElements(CdpBy by, Duration duration) {
        String locatorScript = "";
        switch (by.getType()) {
            case ID:
                locatorScript = CdpScripts.ID_LOCATOR_SCRIPT;
                break;
            case CSS:
                locatorScript = CdpScripts.CSS_LOCATOR_SCRIPT;
                break;
            case XPATH:
                locatorScript = CdpScripts.XPATH_LOCATOR_SCRIPT;
                break;
        }

        String script = String.format(CdpScripts.FIND_ELEMENT_SCRIPT.replace("<locatorScript>", locatorScript), this.referenceId, by.getLocator());
        AtomicReference<List<CdpElement>> cdpElements = new AtomicReference<>(new ArrayList<>());
        Utilities.waitUntil( () -> {
            JsonNode result = this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true);
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
}
