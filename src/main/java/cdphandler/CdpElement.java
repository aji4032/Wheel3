package cdphandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import tools.Utilities;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CdpElement {
    private final CdpDriver cdpDriver;
    private final CdpBy by;
    private final String referenceId;
    private final CdpElement parentElement;

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

    public String captureScreenshot(){
        scrollIntoView();
        CdpRect objRect = getRect();
        CdpPoint objPoint = objRect.point();
        CdpDimension objDimension = objRect.dimension();
        if(objDimension.width() > 0 && objDimension.height() > 0)
            return this.cdpDriver.getCdpUtility().pageCaptureScreenshot("png", objPoint.x(), objPoint.y(), objDimension.width(), objDimension.height(), 1).get("data").asText();
        return "";
    }

    public void clear(){
        String script = String.format(CdpScripts.CLEAR_ELEMENT_SCRIPT, this.referenceId);
        this.cdpDriver.getCdpUtility().runtimeEvaluate(script, false);
    }

    public void click() {
        scrollIntoView();
        if(!isElementActionable())
            System.err.println(this + " element is not actionable");

        CdpPoint objCdpPoint = getCenterLocation();
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.MOVED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "none", 0);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.PRESSED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 1);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.RELEASED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 1);
        System.out.println("Clicked: " + this);
    }

    public void doubleClick() {
        scrollIntoView();
        if(!isElementActionable())
            System.err.println(this + " element is not actionable");

        CdpPoint objCdpPoint = getCenterLocation();
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.MOVED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "none", 0);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.PRESSED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 1);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.RELEASED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 1);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.PRESSED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 2);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.RELEASED, objCdpPoint.x(), objCdpPoint.y(), cdpDriver.getCurrentModifierValue(), "left", 2);
        System.out.println("Double clicked: " + this);
    }

    public void dragDrop(int xOffset, int yOffset){
        scrollIntoView();
        CdpPoint sourceLocation = getCenterLocation();
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.MOVED, sourceLocation.x(), sourceLocation.y(), cdpDriver.getCurrentModifierValue(), "none", 0);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.PRESSED, sourceLocation.x(), sourceLocation.y(), cdpDriver.getCurrentModifierValue(), "left", 1);

        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.MOVED, sourceLocation.x() + xOffset, sourceLocation.y() + yOffset, cdpDriver.getCurrentModifierValue(), "none", 0);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.RELEASED, sourceLocation.x() + xOffset, sourceLocation.y() + yOffset, cdpDriver.getCurrentModifierValue(), "left", 1);
        System.out.println("Dragged: " + this + " to " + (sourceLocation.x() + xOffset) + ", " + (sourceLocation.y() + yOffset));
    }

    public boolean equals(Object object) {
        return object instanceof CdpElement && ((CdpElement) object).referenceId.equals(this.referenceId);
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
        String locatorScript = switch (by.getType()) {
            case ID    -> CdpScripts.ID_LOCATOR_SCRIPT;
            case CSS   -> CdpScripts.CSS_LOCATOR_SCRIPT;
            case XPATH -> CdpScripts.XPATH_LOCATOR_SCRIPT;
        };

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

    public String getAttribute(String attributeName) {
        String script = String.format(CdpScripts.GET_ATTRIBUTE_SCRIPT, this.referenceId, attributeName);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asText();
    }

    public CdpDriver getCdpDriver() {
        return cdpDriver;
    }

    public CdpPoint getCenterLocation(){
        String script = String.format(CdpScripts.IN_VIEW_CENTER_POINT, this.referenceId);
        JsonNode result = this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(result.get("value").asText());
            return new CdpPoint(node.get("x").asInt(), node.get("y").asInt());
        } catch(Exception e) {
            System.err.println("Failed to get point of element");
            System.exit(1);
            return null;
        }
    }

    public String getCssValue(String propertyName){
        String script = String.format(CdpScripts.GET_CSS_VALUE_SCRIPT, this.referenceId, propertyName);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asText();
    }

    public CdpPoint getLocation() {
        return getRect().point();
    }

    public CdpRect getRect(){
        String script = String.format(CdpScripts.GET_RECT_SCRIPT, this.referenceId);
        JsonNode result = this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(result.get("value").asText());
            return new CdpRect(
                    new CdpPoint(node.get("x").asInt(), node.get("y").asInt()),
                    new CdpDimension(node.get("width").asInt(), node.get("height").asInt())
            );
        } catch(Exception e) {
            System.err.println("Failed to get rect of element");
            System.exit(1);
            return null;
        }
    }

    public String getReferenceId(){
        return this.referenceId;
    }

    public int getScrollHeight(){
        String script = String.format(CdpScripts.GET_SCROLL_HEIGHT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asInt();
    }

    public int getScrollLeft(){
        String script = String.format(CdpScripts.GET_SCROLL_LEFT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asInt();
    }

    public int getScrollTop(){
        String script = String.format(CdpScripts.GET_SCROLL_TOP, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asInt();
    }

    public CdpDimension getSize() {
        return getRect().dimension();
    }

    public String getText() {
        String script = String.format(CdpScripts.GET_INNER_TEXT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asText();
    }

    @Override
    public int hashCode() {
        return Objects.hash(by, referenceId);
    }

    public boolean isDisplayed() {
        String script = String.format(CdpScripts.IS_DISPLAYED_SCRIPT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asBoolean();
    }

    public boolean isElementActionable() {
        return isElementActionable(cdpDriver.getDefaultTimeout());
    }

    public boolean isElementActionable(Duration timeout) {
        return Utilities.waitUntil(() -> isDisplayed() && isEnabled() && !isElementObscured(), timeout);
    }

    public boolean isElementObscured(){
        CdpPoint inViewCenterPoint = getCenterLocation();
        String script = String.format(CdpScripts.IS_ELEMENT_OBSCURED_SCRIPT, this.referenceId, inViewCenterPoint.x(), inViewCenterPoint.y());
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asBoolean();
    }

    public boolean isElementPresent(CdpBy by) {
        return !findElements(by, Duration.ofSeconds(1)).isEmpty();
    }

    public boolean isEnabled() {
        String script = String.format(CdpScripts.IS_ENABLED_SCRIPT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asBoolean();
    }

    public boolean isSelected(){
        String script = String.format(CdpScripts.IS_SELECTED_SCRIPT, this.referenceId);
        return this.cdpDriver.getCdpUtility().runtimeEvaluate(script, true).get("value").asBoolean();
    }

    public void mouseMove() {
        mouseMove(0, 0);
    }

    public void mouseMove(int xOffset, int yOffset) {
        scrollIntoView();
        if(!isElementActionable())
            System.err.println("Element is not actionable");

        CdpPoint objCdpPoint = getCenterLocation();
        objCdpPoint = new CdpPoint(objCdpPoint.x() + xOffset, objCdpPoint.y() + yOffset);
        cdpDriver.getCdpUtility().inputDispatchMouseEvent(MouseEvent.MOVED, objCdpPoint.x(), objCdpPoint.y(), 0, "none", 0);
    }

    public void scrollBy(int x, int y) {
        String script = String.format(CdpScripts.SCROLL_BY_SCRIPT, this.referenceId, x, y);
        this.cdpDriver.getCdpUtility().runtimeEvaluate(script, false);
    }

    public void scrollIntoView(){
        String script = String.format(CdpScripts.SCROLL_INTO_VIEW_SCRIPT, this.referenceId);
        this.cdpDriver.getCdpUtility().runtimeEvaluate(script, false);
    }

    public void sendKeys(String text){
        String script = String.format(CdpScripts.SET_ELEMENT_VALUE_SCRIPT, this.referenceId, text);
        this.cdpDriver.getCdpUtility().runtimeEvaluate(script, false);
        System.out.println("Sent keys: " + text);
    }

    @Override
    public String toString(){
        return (parentElement == null ? "" : parentElement + " --> ") + by.toString();
    }
}
