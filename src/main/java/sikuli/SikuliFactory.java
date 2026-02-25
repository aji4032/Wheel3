package sikuli;

import java.lang.reflect.Field;

import org.sikuli.script.ImagePath;
import org.sikuli.script.Pattern;
import tools.Log;

/**
 * Factory class for initializing Sikuli elements in Page Object classes.
 * It uses reflection to find fields annotated with {@link FindPatternBy} and
 * initializes them as {@link Pattern} objects.
 */
public class SikuliFactory {
    private SikuliFactory() {
    }

    /**
     * Initializes the fields of the given page object that are annotated with
     * {@link FindPatternBy}.
     * 
     * @param imagePath The base directory path where images are located. Can be
     *                  null if path is already set.
     * @param page      The page object instance whose fields should be initialized.
     */
    public static void initElements(String imagePath, Object page) {
        if (imagePath != null)
            ImagePath.add(imagePath);

        Field[] fields = page.getClass().getDeclaredFields();
        for (Field field : fields) {
            FindPatternBy annotation = field.getAnnotation(FindPatternBy.class);
            if (annotation == null)
                continue;
            String fileName = annotation.imageName();
            double similar = annotation.similarity();
            int dx = annotation.xOffset();
            int dy = annotation.yOffset();
            try {
                field.setAccessible(true);
                field.set(page, new Pattern(fileName).similar(similar).targetOffset(dx, dy));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Log.fail(e.getMessage());
            }
        }
    }
}
