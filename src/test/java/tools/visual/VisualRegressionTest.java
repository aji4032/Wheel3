package tools.visual;

import cdphandler.CdpBy;
import cdphandler.CdpTestBase;
import cdphandler.ICdpElement;
import org.testng.annotations.Test;

public class VisualRegressionTest extends CdpTestBase {

    @Test
    public void testVisualRegression() {
        getDriver().get("https://example.com");
        
        // This will create a baseline on the first run, and pass.
        // On subsequent runs, it will compare against the baseline.
        verifyScreen("example-com-homepage");
        
        // Now, modify the page to cause a visual failure intentionally
        ICdpElement heading = getDriver().findElement(CdpBy.ByCssSelector("heading", "h1"));
        getDriver().getCdpUtility().runtimeEvaluate("document.querySelector('h1').innerText = 'Visual Test Modified';", false);
        
        // We expect this to fail or show a diff in the report if you don't want it to fail the build immediately.
        // But since assertMatchesBaseline calls Log.fail, this will throw an exception and fail the test.
        // For demonstration, we just let it fail so the user sees the diff in the report.
        try {
            verifyScreen("example-com-homepage");
        } catch (AssertionError e) {
            // Expected to fail because we modified the DOM
            tools.Log.getLogger(VisualRegressionTest.class).info("Caught expected visual failure: " + e.getMessage());
        }
    }
}
