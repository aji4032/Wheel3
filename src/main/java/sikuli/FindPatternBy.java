package sikuli;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(FIELD)
public @interface FindPatternBy {
    String imageName();
    double similarity() default 0.85;
    int xOffset() default 0;
    int yOffset() default 0;
}
