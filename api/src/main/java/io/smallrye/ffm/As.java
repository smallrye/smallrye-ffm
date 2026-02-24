package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Link the argument or return type of the method as the given type.
 */
@Retention(CLASS)
@Target({ PARAMETER, METHOD })
public @interface As {
    /**
     * {@return the type to link as}
     */
    AsType value();
}
