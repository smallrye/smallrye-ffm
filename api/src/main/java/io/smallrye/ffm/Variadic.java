package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Mark the first variadic argument of a variadic function call.
 * If given on a method, this indicates that the call is variadic but there are no variadic parameters
 * in the declaration (used for cases where the target function is variadic to avoid linkage problems).
 */
@Retention(CLASS)
@Target({ PARAMETER, METHOD })
public @interface Variadic {
}
