package mcp;

import cdphandler.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Unit tests for {@link McpToolDispatcher}.
 * Uses a lightweight {@link StubCdpDriver} to avoid needing a real browser.
 */
public class McpToolDispatcherTest {

    private McpToolDispatcher dispatcher;
    private StubCdpDriver stubDriver;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public void setUp() {
        stubDriver = new StubCdpDriver();
        dispatcher = new McpToolDispatcher(stubDriver);
    }

    // -----------------------------------------------------------------------
    // navigate
    // -----------------------------------------------------------------------

    @Test
    public void testNavigate() {
        ObjectNode args = mapper.createObjectNode();
        args.put("url", "https://example.com");

        JsonNode result = dispatcher.dispatch("navigate", args);

        Assert.assertEquals(stubDriver.lastNavigatedUrl, "https://example.com");
        String text = extractText(result);
        Assert.assertTrue(text.contains("navigated to"), "Result should confirm navigation");
    }

    // -----------------------------------------------------------------------
    // get_page_title
    // -----------------------------------------------------------------------

    @Test
    public void testGetPageTitle() {
        stubDriver.titleToReturn = "My Test Page";

        JsonNode result = dispatcher.dispatch("get_page_title", null);

        String text = extractText(result);
        Assert.assertEquals(text, "My Test Page");
    }

    // -----------------------------------------------------------------------
    // get_current_url
    // -----------------------------------------------------------------------

    @Test
    public void testGetCurrentUrl() {
        stubDriver.currentUrlToReturn = "https://stub.example.com/page";

        JsonNode result = dispatcher.dispatch("get_current_url", null);

        Assert.assertEquals(extractText(result), "https://stub.example.com/page");
    }

    // -----------------------------------------------------------------------
    // go_back, go_forward, refresh
    // -----------------------------------------------------------------------

    @Test
    public void testGoBack() {
        JsonNode result = dispatcher.dispatch("go_back", null);
        Assert.assertTrue(stubDriver.backCalled);
        Assert.assertTrue(extractText(result).contains("back"));
    }

    @Test
    public void testGoForward() {
        JsonNode result = dispatcher.dispatch("go_forward", null);
        Assert.assertTrue(stubDriver.forwardCalled);
        Assert.assertTrue(extractText(result).contains("forward"));
    }

    @Test
    public void testRefresh() {
        JsonNode result = dispatcher.dispatch("refresh", null);
        Assert.assertTrue(stubDriver.refreshCalled);
        Assert.assertTrue(extractText(result).contains("refresh"));
    }

    // -----------------------------------------------------------------------
    // take_screenshot
    // -----------------------------------------------------------------------

    @Test
    public void testTakeScreenshot() {
        stubDriver.screenshotBase64 = "iVBORw0KGgoAAAANSUhEU==";

        JsonNode result = dispatcher.dispatch("take_screenshot", null);

        JsonNode content = result.get("content");
        Assert.assertNotNull(content, "Result should contain 'content' array");
        Assert.assertTrue(content.isArray());
        JsonNode item = content.get(0);
        Assert.assertEquals(item.get("type").asText(), "image");
        Assert.assertEquals(item.get("mimeType").asText(), "image/png");
        Assert.assertEquals(item.get("data").asText(), "iVBORw0KGgoAAAANSUhEU==");
    }

    // -----------------------------------------------------------------------
    // press_key
    // -----------------------------------------------------------------------

    @Test
    public void testPressKey() {
        ObjectNode args = mapper.createObjectNode();
        args.put("key", "Enter");

        JsonNode result = dispatcher.dispatch("press_key", args);

        Assert.assertEquals(stubDriver.lastKeyPressed, CdpKey.Enter);
        Assert.assertTrue(extractText(result).contains("Enter"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPressKeyInvalid() {
        ObjectNode args = mapper.createObjectNode();
        args.put("key", "NonExistentKey");

        dispatcher.dispatch("press_key", args);
    }

    // -----------------------------------------------------------------------
    // Unknown tool
    // -----------------------------------------------------------------------

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnknownToolThrows() {
        dispatcher.dispatch("non_existent_tool", null);
    }

    // -----------------------------------------------------------------------
    // Missing required arguments
    // -----------------------------------------------------------------------

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNavigateMissingUrlThrows() {
        dispatcher.dispatch("navigate", mapper.createObjectNode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testClickMissingDescriptionThrows() {
        dispatcher.dispatch("click", mapper.createObjectNode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTypeMissingDescriptionThrows() {
        ObjectNode args = mapper.createObjectNode();
        args.put("text", "hello");
        dispatcher.dispatch("type", args);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTypeMissingTextThrows() {
        ObjectNode args = mapper.createObjectNode();
        args.put("description", "search box");
        dispatcher.dispatch("type", args);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPressKeyMissingKeyThrows() {
        dispatcher.dispatch("press_key", mapper.createObjectNode());
    }

    // -----------------------------------------------------------------------
    // Result structure validation
    // -----------------------------------------------------------------------

    @Test
    public void testTextResultStructure() {
        JsonNode result = dispatcher.dispatch("get_page_title", null);

        Assert.assertTrue(result.has("content"), "Result should have 'content'");
        JsonNode content = result.get("content");
        Assert.assertTrue(content.isArray(), "Content should be an array");
        Assert.assertTrue(content.size() > 0, "Content array should not be empty");
        JsonNode item = content.get(0);
        Assert.assertEquals(item.get("type").asText(), "text");
        Assert.assertTrue(item.has("text"), "Content item should have 'text' field");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String extractText(JsonNode result) {
        return result.get("content").get(0).get("text").asText();
    }

    // -----------------------------------------------------------------------
    // Stub ICdpDriver
    // -----------------------------------------------------------------------

    /**
     * Minimal stub that records method calls for verification.
     * Only implements methods used by McpToolDispatcher.
     */
    static class StubCdpDriver implements ICdpDriver {

        String lastNavigatedUrl;
        String titleToReturn = "Stub Title";
        String currentUrlToReturn = "https://stub.test";
        String pageSourceToReturn = "<html><body>stub</body></html>";
        String screenshotBase64 = "base64data";
        boolean backCalled, forwardCalled, refreshCalled;
        CdpKey lastKeyPressed;
        CdpBy lastFindBy;
        StubCdpElement lastElement = new StubCdpElement();

        @Override
        public void back() {
            backCalled = true;
        }

        @Override
        public String captureScreenshot() {
            return screenshotBase64;
        }

        @Override
        public void close() {
        }

        @Override
        public void closeBrowser() {
        }

        @Override
        public void closeTab() {
        }

        @Override
        public void closeWindow() {
        }

        @Override
        public ICdpElement findElement(CdpBy by) {
            lastFindBy = by;
            return lastElement;
        }

        @Override
        public ICdpElement findElement(CdpBy by, Duration d) {
            lastFindBy = by;
            return lastElement;
        }

        @Override
        public List<ICdpElement> findElements(CdpBy by) {
            lastFindBy = by;
            return List.of(lastElement);
        }

        @Override
        public List<ICdpElement> findElements(CdpBy by, Duration d) {
            lastFindBy = by;
            return List.of(lastElement);
        }

        @Override
        public void forward() {
            forwardCalled = true;
        }

        @Override
        public void fullScreenWindow() {
        }

        @Override
        public void get(String url) {
            lastNavigatedUrl = url;
        }

        @Override
        public CdpUtility getCdpUtility() {
            return null;
        }

        @Override
        public int getCurrentModifierValue() {
            return 0;
        }

        @Override
        public String getCurrentUrl() {
            return currentUrlToReturn;
        }

        @Override
        public Duration getDefaultTimeout() {
            return Duration.ofSeconds(30);
        }

        @Override
        public Duration getPageLoadTimeout() {
            return Duration.ofSeconds(60);
        }

        @Override
        public String getPageSource() {
            return pageSourceToReturn;
        }

        @Override
        public Duration getPollingInterval() {
            return Duration.ofMillis(50);
        }

        @Override
        public String getTitle() {
            return titleToReturn;
        }

        @Override
        public String getWindowHandle() {
            return "handle-1";
        }

        @Override
        public List<String> getWindowHandles() {
            return List.of("handle-1");
        }

        @Override
        public CdpRect getWindowRect() {
            return null;
        }

        @Override
        public boolean isElementPresent(CdpBy by) {
            return true;
        }

        @Override
        public void keyDown(CdpKey key) {
        }

        @Override
        public void keyUp(CdpKey key) {
        }

        @Override
        public void keyPress(CdpKey key) {
            lastKeyPressed = key;
        }

        @Override
        public void maximizeWindow() {
        }

        @Override
        public void minimizeWindow() {
        }

        @Override
        public void refresh() {
            refreshCalled = true;
        }

        @Override
        public void sendKeys(String text) {
        }

        @Override
        public void setDefaultTimeout(Duration d) {
        }

        @Override
        public void setPageLoadTimeout(Duration d) {
        }

        @Override
        public void setPollingInterval(Duration d) {
        }

        @Override
        public void setWindowRect(CdpRect rect) {
        }

        @Override
        public void sleep(Duration d) {
        }

        @Override
        public void switchToWindow(String handle) {
        }
    }

    /**
     * Minimal stub element.
     */
    static class StubCdpElement implements ICdpElement {
        @Override
        public String captureScreenshot() {
            return "base64";
        }

        @Override
        public void clear() {
        }

        @Override
        public void click() {
        }

        @Override
        public void doubleClick() {
        }

        @Override
        public void dragDrop(int xOffset, int yOffset) {
        }

        @Override
        public ICdpElement findElement(CdpBy by) {
            return this;
        }

        @Override
        public ICdpElement findElement(CdpBy by, Duration d) {
            return this;
        }

        @Override
        public List<ICdpElement> findElements(CdpBy by) {
            return List.of(this);
        }

        @Override
        public List<ICdpElement> findElements(CdpBy by, Duration d) {
            return List.of(this);
        }

        @Override
        public String getAttribute(String name) {
            return "";
        }

        @Override
        public CdpBy getBy() {
            return CdpBy.ById("stub", "stub");
        }

        @Override
        public ICdpDriver getCdpDriver() {
            return null;
        }

        @Override
        public CdpPoint getCenterLocation() {
            return new CdpPoint(50, 25);
        }

        @Override
        public String getCssValue(String prop) {
            return "";
        }

        @Override
        public CdpPoint getLocation() {
            return new CdpPoint(0, 0);
        }

        @Override
        public CdpRect getRect() {
            return new CdpRect(new CdpPoint(0, 0), new CdpDimension(100, 50));
        }

        @Override
        public String getReferenceId() {
            return "ref-1";
        }

        @Override
        public int getScrollHeight() {
            return 0;
        }

        @Override
        public int getScrollLeft() {
            return 0;
        }

        @Override
        public int getScrollTop() {
            return 0;
        }

        @Override
        public CdpDimension getSize() {
            return new CdpDimension(100, 50);
        }

        @Override
        public String getText() {
            return "stub text";
        }

        @Override
        public boolean isDisplayed() {
            return true;
        }

        @Override
        public boolean isElementActionable() {
            return true;
        }

        @Override
        public boolean isElementActionable(Duration timeout) {
            return true;
        }

        @Override
        public boolean isElementObscured() {
            return false;
        }

        @Override
        public boolean isElementPresent(CdpBy by) {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void mouseMove() {
        }

        @Override
        public void mouseMove(int xOffset, int yOffset) {
        }

        @Override
        public void scrollBy(int x, int y) {
        }

        @Override
        public void scrollIntoView() {
        }

        @Override
        public void sendKeys(String text) {
        }
    }
}
