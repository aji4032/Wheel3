package tools;

import org.testng.Assert;
import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.io.File;
import java.io.IOException;

import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * Integration test class to verify that the JMeterRunner executes JMX files,
 * records statistics correctly, generates HTML reports, and reports errors.
 */
public class JMeterRunnerTest {

    @Test
    public void testJMeterRunnerWithDynamicJmx() throws IOException {
        String tempJmxPath = "src/test/resources/temp-test.jmx";
        File jmxFile = new File(tempJmxPath);
        
        // Ensure the directory exists
        jmxFile.getParentFile().mkdirs();

        // 1. Programmatically define a simple test plan and save it to a JMX file
        // We run 1 thread with 2 iterations hitting a reliable mock endpoint.
        testPlan(
                threadGroup(1, 2,
                        httpSampler("MockEchoSampler", "https://postman-echo.com/get")
                )
        ).saveAsJmx(jmxFile.getAbsolutePath());

        Assert.assertTrue(jmxFile.exists(), "Programmatically generated JMX file should exist.");

        // 2. Execute the JMX file using our JMeterRunner
        TestPlanStats stats = JMeterRunner.run(jmxFile);

        // 3. Verify statistics
        Assert.assertNotNull(stats, "Execution stats should not be null.");
        Assert.assertEquals(stats.overall().samplesCount(), 2, "Expected 2 total samples executed.");
        Assert.assertEquals(stats.overall().errorsCount(), 0, "Expected 0 failed requests.");
        Assert.assertNotNull(stats.overall().sampleTime().mean(), "Mean response time should be recorded.");

        // 4. Verify output files are generated
        File reportDir = new File("target/jmeter-reports/temp-test");
        File jtlFile = new File("target/jmeter-results/temp-test.jtl");

        Assert.assertTrue(reportDir.exists() && reportDir.isDirectory(), "HTML Report directory should be generated.");
        Assert.assertTrue(jtlFile.exists(), "JTL results file should be generated.");
    }
}
