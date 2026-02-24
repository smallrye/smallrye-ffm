package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Mark a parameter as the buffer to copy the return value into, instead of returning it
 * as the return value of the method.
 * <p>
 * This is particularly useful in the case where a function returns a structure or a union.
 */
@Target(PARAMETER)
@Retention(CLASS)
public @interface Return {
}
