package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Restrict the parameter to be output-only.
 */
@Target(PARAMETER)
@Retention(CLASS)
public @interface Out {
}
