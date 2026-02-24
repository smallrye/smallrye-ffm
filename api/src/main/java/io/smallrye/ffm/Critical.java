package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate that the method is critical.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Critical {
    boolean heap() default false;
}
