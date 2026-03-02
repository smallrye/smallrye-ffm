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
    /**
     * {@return {@code true} if the heap should be accessible from this native call, or {@code false} otherwise}
     */
    boolean heap() default false;
}
