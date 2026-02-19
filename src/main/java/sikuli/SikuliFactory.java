package sikuli;

import java.lang.reflect.Field;

import org.sikuli.script.ImagePath;
import org.sikuli.script.Pattern;
import tools.Log;

public class SikuliFactory {
    private SikuliFactory(){}

    public static void initElements(String imagePath, Object page) {
        if(imagePath != null)
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
