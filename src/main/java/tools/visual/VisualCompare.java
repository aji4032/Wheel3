package tools.visual;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.model.Rectangle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VisualCompare {

    public static ImageComparisonResult compareImages(File expected, File actual, File diffOutput) throws IOException {
        BufferedImage expectedImage = ImageIO.read(expected);
        BufferedImage actualImage = ImageIO.read(actual);

        ImageComparison imageComparison = new ImageComparison(expectedImage, actualImage);
        ImageComparisonResult result = imageComparison.compareImages();

        if (result.getImageComparisonState() == ImageComparisonState.MISMATCH && diffOutput != null) {
            ImageComparisonUtil.saveImage(diffOutput, result.getResult());
        }
        return result;
    }

    public static ImageComparisonResult compareImagesWithMask(File expected, File actual, File diffOutput, List<Rectangle> ignoredAreas) throws IOException {
        BufferedImage expectedImage = ImageIO.read(expected);
        BufferedImage actualImage = ImageIO.read(actual);

        ImageComparison imageComparison = new ImageComparison(expectedImage, actualImage);
        if (ignoredAreas != null && !ignoredAreas.isEmpty()) {
            imageComparison.setExcludedAreas(ignoredAreas);
        }
        
        ImageComparisonResult result = imageComparison.compareImages();

        if (result.getImageComparisonState() == ImageComparisonState.MISMATCH && diffOutput != null) {
            ImageComparisonUtil.saveImage(diffOutput, result.getResult());
        }
        return result;
    }
}
