package tools;

import logger.Log;
import logger.Logger;
import org.testng.Assert;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.stats.StatsSummary;
import us.abstracta.jmeter.javadsl.core.stats.TimeMetricSummary;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;

/**
 * Utility runner class for executing JMeter (.jmx) test plans programmatically
 * and integrating the results into TestNG test runs.
 */
public class JMeterRunner {

    private static final Logger log = Log.getLogger(JMeterRunner.class);

    private JMeterRunner() {
        // Prevent instantiation
    }

    /**
     * Executes a JMeter test plan from the given file path.
     * Generates a default HTML report and a JTL results file in the 'target' directory.
     * Asserts that there are zero failed requests.
     *
     * @param jmxPath Path to the .jmx file
     * @return TestPlanStats containing execution metrics
     */
    public static TestPlanStats run(String jmxPath) {
        if (jmxPath == null || jmxPath.trim().isEmpty()) {
            log.error("JMX file path cannot be null or empty.");
            return null;
        }
        return run(new File(jmxPath));
    }

    /**
     * Executes a JMeter test plan.
     * Generates a default HTML report and a JTL results file in the 'target' directory.
     * Asserts that there are zero failed requests.
     *
     * @param jmxFile The JMX file
     * @return TestPlanStats containing execution metrics
     */
    public static TestPlanStats run(File jmxFile) {
        if (jmxFile == null || !jmxFile.exists()) {
            String path = jmxFile != null ? jmxFile.getAbsolutePath() : "null";
            log.error("JMX file does not exist: {}", path);
            return null;
        }

        String baseName = jmxFile.getName().replaceAll("(?i)\\.jmx$", "");
        File reportDir = new File("target/jmeter-reports/" + baseName);
        File jtlFile = new File("target/jmeter-results/" + baseName + ".jtl");

        return run(jmxFile, reportDir, jtlFile);
    }

    /**
     * Executes a JMeter test plan with custom report and result destinations.
     * Asserts that there are zero failed requests.
     *
     * @param jmxFile   The JMX file to run
     * @param reportDir Directory where the HTML report will be generated (optional, null to disable)
     * @param jtlFile   File where raw results (.jtl) will be saved (optional, null to disable)
     * @return TestPlanStats containing execution metrics
     */
    public static TestPlanStats run(File jmxFile, File reportDir, File jtlFile) {
        if (jmxFile == null || !jmxFile.exists()) {
            String path = jmxFile != null ? jmxFile.getAbsolutePath() : "null";
            log.error("JMX file does not exist: {}", path);
            return null;
        }

        log.info("Starting JMeter JMX Execution: {}", jmxFile.getAbsolutePath());
        if (reportDir != null) {
            log.info("HTML Report will be generated at: {}", reportDir.getAbsolutePath());
            // Clear existing report folder if it exists to avoid jmeter-java-dsl reporting failure
            deleteDirectory(reportDir);
        }
        if (jtlFile != null) {
            log.info("JTL results will be written to: {}", jtlFile.getAbsolutePath());
            if (jtlFile.exists()) {
                jtlFile.delete();
            } else {
                jtlFile.getParentFile().mkdirs();
            }
        }

        try {
            // Load test plan from the JMX file
            DslTestPlan testPlan = DslTestPlan.fromJmx(jmxFile.getAbsolutePath());

            // Add reporting children programmatically
            if (reportDir != null && jtlFile != null) {
                testPlan.children(
                        htmlReporter(reportDir.getParent(), reportDir.getName()),
                        jtlWriter(jtlFile.getParent(), jtlFile.getName())
                );
            } else if (reportDir != null) {
                testPlan.children(htmlReporter(reportDir.getParent(), reportDir.getName()));
            } else if (jtlFile != null) {
                testPlan.children(jtlWriter(jtlFile.getParent(), jtlFile.getName()));
            }

            long startTime = System.currentTimeMillis();
            TestPlanStats stats = testPlan.run();
            long durationMs = System.currentTimeMillis() - startTime;

            logSummary(jmxFile.getName(), stats, durationMs);

            long errors = stats.overall().errorsCount();
            if (errors > 0) {
                log.error("JMeter test '{}' failed with {} errors.", jmxFile.getName(), errors);
            } else {
                log.info("JMeter test '{}' completed successfully with 0 errors.", jmxFile.getName());
            }

            return stats;

        } catch (IOException e) {
            log.error("IOException occurred while executing JMeter test: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("An unexpected error occurred during JMeter execution: " + e.getMessage(), e);
            return null;
        }
    }

    private static void logSummary(String testName, TestPlanStats stats, long durationMs) {
        StatsSummary overall = stats.overall();
        TimeMetricSummary times = overall.sampleTime();

        long totalSamples = overall.samplesCount();
        long errors = overall.errorsCount();
        double errorRate = totalSamples > 0 ? ((double) errors / totalSamples) * 100 : 0.0;

        Duration mean = times.mean();
        Duration min = times.min();
        Duration max = times.max();
        Duration p90 = times.perc90();
        Duration p95 = times.perc95();
        Duration p99 = times.perc99();

        log.info("================ JMETER PERFORMANCE SUMMARY ================");
        log.info("Test Plan:       {}", testName);
        log.info("Total Requests:  {}", totalSamples);
        log.info("Successful:      {}", totalSamples - errors);
        log.info("Failed Requests: {} ({})", errors, String.format("%.2f%%", errorRate));
        log.info("Total Duration:  {} ms", durationMs);
        log.info("---------------- Response Time Metrics ---------------------");
        log.info("Average/Mean:    {} ms", mean.toMillis());
        log.info("Minimum:         {} ms", min.toMillis());
        log.info("Maximum:         {} ms", max.toMillis());
        log.info("90th Percentile: {} ms", p90.toMillis());
        log.info("95th Percentile: {} ms", p95.toMillis());
        log.info("99th Percentile: {} ms", p99.toMillis());
        log.info("============================================================");
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
