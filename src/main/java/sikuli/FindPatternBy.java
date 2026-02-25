package sikuli;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to mark fields in Page Object classes that represent Sikuli
 * patterns.
 * These fields are typically initialized using
 * {@link SikuliFactory#initElements(String, Object)}.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface FindPatternBy {
    /**
     * The name or path of the image file to be used for matching.
     * 
     * @return the image file name
     */
    String imageName();

    /**
     * The similarity score (0.0 to 1.0) for matching the pattern.
     * Higher values mean more precise matching.
     * 
     * @return the similarity score, default is 0.85
     */
    double similarity() default 0.85;

    /**
     * The x-axis offset from the center of the matched pattern.
     * 
     * @return the x offset, default is 0
     */
    int xOffset() default 0;

    /**
     * The y-axis offset from the center of the matched pattern.
     * 
     * @return the y offset, default is 0
     */
    int yOffset() default 0;
}
