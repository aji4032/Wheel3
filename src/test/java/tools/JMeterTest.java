package tools;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Standard TestNG test class wrapper for running JMeter JMX test plans.
 * This class allows execution of JMeter tests seamlessly via TestNG suites,
 * IDE runners, or Maven command-line arguments.
 */
public class JMeterTest {

    private static final Logger log = Log.getLogger(JMeterTest.class);

    /**
     * Runs a JMeter test plan using a path passed via TestNG XML parameter "jmxFile".
     * E.g. <parameter name="jmxFile" value="src/test/resources/my-perf-test.jmx"/>
     *
     * @param jmxFileParam The JMX file path parameter
     */
    @Test
    @Parameters({"jmxFile"})
    public void runJmxTestFromParameter(String jmxFileParam) {
        log.info("TestNG Parameter 'jmxFile' received: {}", jmxFileParam);
        JMeterRunner.run(jmxFileParam);
    }

    /**
     * Runs a JMeter test plan using a path passed via the system property "jmxFile".
     * E.g. mvn test -Dtest=JMeterTest -DjmxFile=src/test/resources/my-perf-test.jmx
     */
    @Test
    public void runJmxTestFromSystemProperty() {
        String jmxFileProp = System.getProperty("jmxFile");
        if (jmxFileProp != null && !jmxFileProp.trim().isEmpty()) {
            log.info("System Property 'jmxFile' received: {}", jmxFileProp);
            JMeterRunner.run(jmxFileProp);
        } else {
            log.warn("No 'jmxFile' system property specified. Skipping execution via system property.");
            log.info("To run a test, use: mvn test -Dtest=JMeterTest -DjmxFile=path/to/test.jmx");
        }
    }
}
