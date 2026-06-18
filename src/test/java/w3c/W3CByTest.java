package w3c;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for W3CBy, W3CLocatorType and mapping in W3CWindow/W3CElement.
 */
public class W3CByTest {

    @Test
    public void testByXPath() {
        W3CBy by = W3CBy.ByXPath("submit btn", "//button[@type='submit']");
        Assert.assertEquals(by.name(), "submit btn");
        Assert.assertEquals(by.type(), W3CLocatorType.XPATH);
        Assert.assertEquals(by.locator(), "//button[@type='submit']");
    }

    @Test
    public void testByAutomationId() {
        W3CBy by = W3CBy.ByAutomationId("username field", "txtUser");
        Assert.assertEquals(by.name(), "username field");
        Assert.assertEquals(by.type(), W3CLocatorType.AUTOMATION_ID);
        Assert.assertEquals(by.locator(), "txtUser");
    }

    @Test
    public void testByClassName() {
        W3CBy by = W3CBy.ByClassName("container", "main-container");
        Assert.assertEquals(by.name(), "container");
        Assert.assertEquals(by.type(), W3CLocatorType.CLASS_NAME);
        Assert.assertEquals(by.locator(), "main-container");
    }

    @Test
    public void testByName() {
        W3CBy by = W3CBy.ByName("login link", "login");
        Assert.assertEquals(by.name(), "login link");
        Assert.assertEquals(by.type(), W3CLocatorType.NAME);
        Assert.assertEquals(by.locator(), "login");
    }

    @Test
    public void testToString() {
        W3CBy by = W3CBy.ByXPath("ok btn", "//button[text()='OK']");
        String expected = "{'name': 'ok btn', 'type': 'XPATH', 'locator': '//button[text()='OK']'}";
        Assert.assertEquals(by.toString(), expected);
    }

    @Test
    public void testW3CWindowLocatorMapping() {
        W3CWindow window = new W3CWindow(null, null, null, null);
        Assert.assertEquals(window.mapLocatorType(W3CLocatorType.AUTOMATION_ID), "accessibility id");
        Assert.assertEquals(window.mapLocatorType(W3CLocatorType.CLASS_NAME), "class name");
        Assert.assertEquals(window.mapLocatorType(W3CLocatorType.NAME), "name");
        Assert.assertEquals(window.mapLocatorType(W3CLocatorType.XPATH), "xpath");
    }

    @Test
    public void testW3CElementLocatorMapping() {
        W3CElement element = new W3CElement(null, null, null, null, null, null);
        Assert.assertEquals(element.mapLocatorType(W3CLocatorType.AUTOMATION_ID), "accessibility id");
        Assert.assertEquals(element.mapLocatorType(W3CLocatorType.CLASS_NAME), "class name");
        Assert.assertEquals(element.mapLocatorType(W3CLocatorType.NAME), "name");
        Assert.assertEquals(element.mapLocatorType(W3CLocatorType.XPATH), "xpath");
    }
}
