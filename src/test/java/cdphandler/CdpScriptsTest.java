package cdphandler;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the {@link CdpScripts} JavaScript template strings.
 * Validates that placeholders, wrappers, and key scripts are well-formed.
 */
public class CdpScriptsTest {

    // -----------------------------------------------------------------------
    // Wrapper scripts
    // -----------------------------------------------------------------------

    @Test
    public void testWrapperPreScript() {
        Assert.assertEquals(CdpScripts.WRAPPER_PRE_SCRIPT, "(function() {");
    }

    @Test
    public void testWrapperPostScript() {
        Assert.assertEquals(CdpScripts.WRAPPER_POST_SCRIPT, "})();");
    }

    // -----------------------------------------------------------------------
    // Navigation scripts
    // -----------------------------------------------------------------------

    @Test
    public void testBackScript() {
        Assert.assertEquals(CdpScripts.BACK_SCRIPT, "window.history.back();");
    }

    @Test
    public void testForwardScript() {
        Assert.assertEquals(CdpScripts.FORWARD_SCRIPT, "window.history.forward();");
    }

    // -----------------------------------------------------------------------
    // IIFE-wrapped scripts should start with pre and end with post
    // -----------------------------------------------------------------------

    @Test
    public void testGetCurrentUrlIsIIFE() {
        Assert.assertTrue(CdpScripts.GET_CURRENT_URL_SCRIPT.startsWith(CdpScripts.WRAPPER_PRE_SCRIPT),
                "GET_CURRENT_URL_SCRIPT should start with IIFE wrapper");
        Assert.assertTrue(CdpScripts.GET_CURRENT_URL_SCRIPT.endsWith(CdpScripts.WRAPPER_POST_SCRIPT),
                "GET_CURRENT_URL_SCRIPT should end with IIFE wrapper");
    }

    @Test
    public void testGetTitleScript() {
        Assert.assertTrue(CdpScripts.GET_TITLE_SCRIPT.contains("document.title"),
                "GET_TITLE_SCRIPT should reference document.title");
    }

    @Test
    public void testWaitUntilDocumentReady() {
        Assert.assertTrue(CdpScripts.WAIT_UNTIL_DOCUMENT_READY.contains("document.readyState"),
                "WAIT_UNTIL_DOCUMENT_READY should check readyState");
        Assert.assertTrue(CdpScripts.WAIT_UNTIL_DOCUMENT_READY.contains("complete"),
                "WAIT_UNTIL_DOCUMENT_READY should check for 'complete'");
    }

    @Test
    public void testGetPageSource() {
        Assert.assertTrue(CdpScripts.GET_PAGE_SOURCE.contains("outerHTML"),
                "GET_PAGE_SOURCE should reference outerHTML");
    }

    // -----------------------------------------------------------------------
    // Locator scripts should contain the %s placeholder
    // -----------------------------------------------------------------------

    @Test
    public void testIdLocatorHasPlaceholder() {
        Assert.assertTrue(CdpScripts.ID_LOCATOR_SCRIPT.contains("%s"),
                "ID_LOCATOR_SCRIPT should have a %s placeholder for the ID value");
        Assert.assertTrue(CdpScripts.ID_LOCATOR_SCRIPT.contains("getElementById"),
                "ID_LOCATOR_SCRIPT should use getElementById");
    }

    @Test
    public void testCssLocatorHasPlaceholder() {
        Assert.assertTrue(CdpScripts.CSS_LOCATOR_SCRIPT.contains("%s"),
                "CSS_LOCATOR_SCRIPT should have a %s placeholder for the selector");
        Assert.assertTrue(CdpScripts.CSS_LOCATOR_SCRIPT.contains("querySelectorAll"),
                "CSS_LOCATOR_SCRIPT should use querySelectorAll");
    }

    @Test
    public void testXPathLocatorHasPlaceholder() {
        Assert.assertTrue(CdpScripts.XPATH_LOCATOR_SCRIPT.contains("%s"),
                "XPATH_LOCATOR_SCRIPT should have a %s placeholder for the expression");
        Assert.assertTrue(CdpScripts.XPATH_LOCATOR_SCRIPT.contains("document.evaluate"),
                "XPATH_LOCATOR_SCRIPT should use document.evaluate");
    }

    // -----------------------------------------------------------------------
    // Find element script structure
    // -----------------------------------------------------------------------

    @Test
    public void testFindElementScriptHasLocatorPlaceholder() {
        Assert.assertTrue(CdpScripts.FIND_ELEMENT_SCRIPT.contains("<locatorScript>"),
                "FIND_ELEMENT_SCRIPT should contain the <locatorScript> placeholder");
    }

    @Test
    public void testFindElementScriptUsesMap() {
        Assert.assertTrue(CdpScripts.FIND_ELEMENT_SCRIPT.contains("cdpElements"),
                "FIND_ELEMENT_SCRIPT should reference cdpElements Map");
        Assert.assertTrue(CdpScripts.FIND_ELEMENT_SCRIPT.contains("cdpElementIds"),
                "FIND_ELEMENT_SCRIPT should reference cdpElementIds WeakMap");
    }

    @Test
    public void testFindElementScriptReturnsIds() {
        Assert.assertTrue(CdpScripts.FIND_ELEMENT_SCRIPT.contains("return ids"),
                "FIND_ELEMENT_SCRIPT should return the ids array");
    }

    // -----------------------------------------------------------------------
    // Element interaction scripts
    // -----------------------------------------------------------------------

    @Test
    public void testFetchElementHasPlaceholder() {
        Assert.assertTrue(CdpScripts.FETCH_ELEMENT.contains("%s"),
                "FETCH_ELEMENT should have a %s placeholder for the element reference ID");
        Assert.assertTrue(CdpScripts.FETCH_ELEMENT.contains("cdpElements.get"),
                "FETCH_ELEMENT should get element from cdpElements map");
    }

    @Test
    public void testClearElementScript() {
        Assert.assertTrue(CdpScripts.CLEAR_ELEMENT_SCRIPT.contains("element.value = ''"),
                "CLEAR_ELEMENT_SCRIPT should clear the element value");
    }

    @Test
    public void testGetAttributeScript() {
        Assert.assertTrue(CdpScripts.GET_ATTRIBUTE_SCRIPT.contains("getAttribute"),
                "GET_ATTRIBUTE_SCRIPT should use getAttribute");
    }

    @Test
    public void testGetInnerTextScript() {
        Assert.assertTrue(CdpScripts.GET_INNER_TEXT.contains("innerText"),
                "GET_INNER_TEXT should reference element.innerText");
    }

    @Test
    public void testIsDisplayedScript() {
        Assert.assertTrue(CdpScripts.IS_DISPLAYED_SCRIPT.contains("getComputedStyle"),
                "IS_DISPLAYED_SCRIPT should use getComputedStyle");
        Assert.assertTrue(CdpScripts.IS_DISPLAYED_SCRIPT.contains("getBoundingClientRect"),
                "IS_DISPLAYED_SCRIPT should check bounding rect");
    }

    @Test
    public void testIsEnabledScript() {
        Assert.assertTrue(CdpScripts.IS_ENABLED_SCRIPT.contains("disabled"),
                "IS_ENABLED_SCRIPT should check the disabled property");
    }

    @Test
    public void testScrollIntoViewScript() {
        Assert.assertTrue(CdpScripts.SCROLL_INTO_VIEW_SCRIPT.contains("scrollIntoViewIfNeeded"),
                "SCROLL_INTO_VIEW_SCRIPT should use scrollIntoViewIfNeeded");
    }

    @Test
    public void testSetElementValueScript() {
        Assert.assertTrue(CdpScripts.SET_ELEMENT_VALUE_SCRIPT.contains("element.value"),
                "SET_ELEMENT_VALUE_SCRIPT should set element.value");
    }

    // -----------------------------------------------------------------------
    // Cleanup script
    // -----------------------------------------------------------------------

    @Test
    public void testCleanupScriptRegistersInterval() {
        Assert.assertTrue(CdpScripts.CDP_ELEMENTS_CLEANUP_SCRIPT.contains("setInterval"),
                "Cleanup script should register setInterval for periodic cleanup");
        Assert.assertTrue(CdpScripts.CDP_ELEMENTS_CLEANUP_SCRIPT.contains("isConnected"),
                "Cleanup script should check element.isConnected");
    }

    // -----------------------------------------------------------------------
    // String.format compatibility — scripts with %s should survive formatting
    // -----------------------------------------------------------------------

    @Test
    public void testIdLocatorFormatting() {
        String result = String.format(CdpScripts.ID_LOCATOR_SCRIPT, "myElementId");
        Assert.assertTrue(result.contains("myElementId"),
                "Formatted ID script should contain the substituted ID");
        Assert.assertFalse(result.contains("%s"),
                "Formatted ID script should have no remaining placeholders");
    }

    @Test
    public void testCssLocatorFormatting() {
        String result = String.format(CdpScripts.CSS_LOCATOR_SCRIPT, "div.container");
        Assert.assertTrue(result.contains("div.container"),
                "Formatted CSS script should contain the substituted selector");
    }

    @Test
    public void testXPathLocatorFormatting() {
        String result = String.format(CdpScripts.XPATH_LOCATOR_SCRIPT, "//button[@id='submit']");
        Assert.assertTrue(result.contains("//button[@id='submit']"),
                "Formatted XPath script should contain the substituted expression");
    }
}
