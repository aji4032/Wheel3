package cdphandler;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the {@link CdpBy} record and its factory methods.
 */
public class CdpByTest {

    // -----------------------------------------------------------------------
    // Factory method tests
    // -----------------------------------------------------------------------

    @Test
    public void testById() {
        CdpBy by = CdpBy.ById("username", "txtUser");
        Assert.assertEquals(by.name(), "username");
        Assert.assertEquals(by.type(), CdpLocatorType.ID);
        Assert.assertEquals(by.locator(), "txtUser");
    }

    @Test
    public void testByCssSelector() {
        CdpBy by = CdpBy.ByCssSelector("login button", "button.login-btn");
        Assert.assertEquals(by.name(), "login button");
        Assert.assertEquals(by.type(), CdpLocatorType.CSS);
        Assert.assertEquals(by.locator(), "button.login-btn");
    }

    @Test
    public void testByXPath() {
        CdpBy by = CdpBy.ByXPath("heading", "//h1[@class='title']");
        Assert.assertEquals(by.name(), "heading");
        Assert.assertEquals(by.type(), CdpLocatorType.XPATH);
        Assert.assertEquals(by.locator(), "//h1[@class='title']");
    }

    // -----------------------------------------------------------------------
    // Direct constructor tests
    // -----------------------------------------------------------------------

    @Test
    public void testDirectConstructor() {
        CdpBy by = new CdpBy("search", CdpLocatorType.NATURAL_LANGUAGE, "the search box");
        Assert.assertEquals(by.name(), "search");
        Assert.assertEquals(by.type(), CdpLocatorType.NATURAL_LANGUAGE);
        Assert.assertEquals(by.locator(), "the search box");
    }

    // -----------------------------------------------------------------------
    // toString tests
    // -----------------------------------------------------------------------

    @Test
    public void testToString() {
        CdpBy by = CdpBy.ById("email", "emailField");
        String result = by.toString();
        Assert.assertTrue(result.contains("email"), "toString should contain the name");
        Assert.assertTrue(result.contains("ID"), "toString should contain the type");
        Assert.assertTrue(result.contains("emailField"), "toString should contain the locator");
    }

    @Test
    public void testToStringFormat() {
        CdpBy by = CdpBy.ByCssSelector("btn", "#submit");
        String expected = "{'name': 'btn', 'type': 'CSS', 'locator': '#submit'}";
        Assert.assertEquals(by.toString(), expected);
    }

    // -----------------------------------------------------------------------
    // Record equality tests
    // -----------------------------------------------------------------------

    @Test
    public void testEqualRecords() {
        CdpBy by1 = CdpBy.ById("field", "myId");
        CdpBy by2 = CdpBy.ById("field", "myId");
        Assert.assertEquals(by1, by2, "Records with same values should be equal");
    }

    @Test
    public void testNotEqualDifferentType() {
        CdpBy by1 = CdpBy.ById("field", "myId");
        CdpBy by2 = CdpBy.ByCssSelector("field", "myId");
        Assert.assertNotEquals(by1, by2, "Records with different types should not be equal");
    }

    @Test
    public void testNotEqualDifferentLocator() {
        CdpBy by1 = CdpBy.ById("field", "id1");
        CdpBy by2 = CdpBy.ById("field", "id2");
        Assert.assertNotEquals(by1, by2, "Records with different locators should not be equal");
    }

    @Test
    public void testNotEqualDifferentName() {
        CdpBy by1 = CdpBy.ById("field1", "myId");
        CdpBy by2 = CdpBy.ById("field2", "myId");
        Assert.assertNotEquals(by1, by2, "Records with different names should not be equal");
    }

    // -----------------------------------------------------------------------
    // Edge case tests
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyStrings() {
        CdpBy by = CdpBy.ById("", "");
        Assert.assertEquals(by.name(), "");
        Assert.assertEquals(by.locator(), "");
    }

    @Test
    public void testSpecialCharactersInLocator() {
        CdpBy by = CdpBy.ByCssSelector("complex", "div > span.cls[data-id='foo']");
        Assert.assertEquals(by.locator(), "div > span.cls[data-id='foo']");
    }

    @Test
    public void testXPathWithFunctions() {
        CdpBy by = CdpBy.ByXPath("text node", "//span[contains(text(),'Hello')]");
        Assert.assertEquals(by.locator(), "//span[contains(text(),'Hello')]");
    }
}
