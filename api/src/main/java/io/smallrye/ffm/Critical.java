package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate that the method is critical.
 *
 * @see java.lang.foreign.Linker.Option#critical(boolean)
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Critical {
    /**
     * {@return {@code true} if the heap should be accessible from this native call, or {@code false} otherwise}
     * If {@code true}, primitive arrays and heap segments are passed to the native function without being copied;
     * the pointer is only valid until the function returns.
     */
    boolean heap() default false;
}
