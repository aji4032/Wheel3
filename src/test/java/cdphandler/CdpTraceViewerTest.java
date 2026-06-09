package cdphandler;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.File;

public class CdpTraceViewerTest extends CdpTestBase {

    @Test
    public void testTraceGeneration() {
        ICdpDriver driver = getDriver();
        File traceZip = new File("target/traces/test_trace.zip");

        // Clean up previous files if any
        if (traceZip.exists()) {
            traceZip.delete();
        }

        File extractDir = new File("target/traces/extracted_test_trace");
        if (extractDir.exists()) {
            deleteDirectory(extractDir);
        }

        // Start Tracing
        driver.startTracing(traceZip);

        // Execute typical browser operations
        driver.get("https://example.com");

        // Find and read elements
        ICdpElement h1 = driver.findElement(CdpBy.ByCssSelector("Header", "h1"));
        Assert.assertEquals(h1.getText(), "Example Domain");

        // Interact with element (clicks)
        ICdpElement moreInfoLink = driver.findElement(CdpBy.ByCssSelector("Link", "a"));
        moreInfoLink.click();

        // Trigger a Fetch/XHR request so we can verify response body capture
        // Use httpbin.org which returns a well-known JSON response
        driver.getCdpUtility().runtimeEvaluate(
                "fetch('https://httpbin.org/json').then(r=>r.text()).then(t=>console.log('fetch-done'))", false);
        // Give the fetch time to complete before stopping the trace
        driver.sleep(java.time.Duration.ofSeconds(3));

        // Stop Tracing and export ZIP
        driver.stopTracing();

        // Verify ZIP file exists and contains files
        Assert.assertTrue(traceZip.exists(), "Trace ZIP file was not generated!");
        Assert.assertTrue(traceZip.length() > 0, "Trace ZIP file is empty!");

        // Extract ZIP and verify internal structure
        tools.FileUtilities.extractZipFile(traceZip, extractDir);

        Assert.assertTrue(new File(extractDir, "index.html").exists(), "index.html is missing in trace archive!");
        Assert.assertTrue(new File(extractDir, "trace-data.js").exists(), "trace-data.js is missing in trace archive!");

        File screenshotsDir = new File(extractDir, "screenshots");
        Assert.assertTrue(screenshotsDir.exists() && screenshotsDir.isDirectory(), "screenshots folder is missing in trace archive!");
        File[] screenshots = screenshotsDir.listFiles();
        Assert.assertTrue(screenshots != null && screenshots.length > 0, "No screenshots were captured in trace archive!");

        // Page source HTML is now inlined into trace-data.js (not written to a sources/ folder)
        // Verify trace-data.js contains pageSource content
        String traceDataContent;
        try {
            traceDataContent = java.nio.file.Files.readString(new File(extractDir, "trace-data.js").toPath());
        } catch (Exception e) {
            traceDataContent = "";
        }
        Assert.assertTrue(traceDataContent.contains("pageSourceBefore") || traceDataContent.contains("pageSourceAfter"),
                "No inlined page source found in trace-data.js!");

        // Verify Fetch response body was captured
        Assert.assertTrue(traceDataContent.contains("responseBody"),
                "No XHR/Fetch response bodies captured in trace-data.js! The body fetch may have deadlocked.");

        // Verify test source code caller info was captured
        Assert.assertTrue(traceDataContent.contains("sourceCode"),
                "No test source code metadata captured in trace-data.js!");
        Assert.assertTrue(traceDataContent.contains("CdpTraceViewerTest.java"),
                "CdpTraceViewerTest.java file reference is missing in trace sourceCode!");
        Assert.assertTrue(traceDataContent.contains("testTraceGeneration"),
                "Test method name 'testTraceGeneration' is missing in trace sourceCode!");
        Assert.assertTrue(traceDataContent.contains("snippet"),
                "No source code snippet lines captured in trace-data.js!");
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
}
