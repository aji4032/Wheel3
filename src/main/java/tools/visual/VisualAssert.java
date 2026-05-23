package tools.visual;

import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import org.apache.commons.io.FileUtils;
import tools.Log;
import tools.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class VisualAssert {

    private static final Logger log = Log.getLogger(VisualAssert.class);
    private static final String BASELINE_DIR = "src/test/resources/baselines/";
    private static final String ACTUALS_DIR = "target/visual-actuals/";
    private static final String DIFFS_DIR = "target/visual-diffs/";

    public static void assertMatchesBaseline(String baselineName, String actualBase64Image) {
        try {
            byte[] actualBytes = Base64.getDecoder().decode(actualBase64Image);
            
            File baselineFile = new File(BASELINE_DIR + baselineName + ".png");
            File actualFile = new File(ACTUALS_DIR + baselineName + ".png");
            File diffFile = new File(DIFFS_DIR + baselineName + "-diff.png");

            actualFile.getParentFile().mkdirs();
            diffFile.getParentFile().mkdirs();
            FileUtils.writeByteArrayToFile(actualFile, actualBytes);

            if (!baselineFile.exists()) {
                baselineFile.getParentFile().mkdirs();
                FileUtils.copyFile(actualFile, baselineFile);
                log.info("Baseline did not exist for {}. Created new baseline.", baselineName);
                return; // pass
            }

            ImageComparisonResult result = VisualCompare.compareImages(baselineFile, actualFile, diffFile);
            
            if (result.getImageComparisonState() == ImageComparisonState.MISMATCH) {
                byte[] expectedBytes = Files.readAllBytes(baselineFile.toPath());
                byte[] diffBytes = diffFile.exists() ? Files.readAllBytes(diffFile.toPath()) : null;
                
                log.visualFail("Visual Regression Mismatch for: " + baselineName, expectedBytes, actualBytes, diffBytes);
            } else {
                log.info("Visual test passed for: {}", baselineName);
            }
        } catch (IOException e) {
            log.fail("Error during visual assertion for " + baselineName, e);
        }
    }
}
