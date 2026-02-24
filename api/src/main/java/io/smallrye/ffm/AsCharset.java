package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specify the character set for a string-typed
 * parameter or method return value.
 * The type must be one of:
 * <ul>
 * <li>{@code String}</li>
 * <li>{@code char[]} (as a UTF-16 source/target)</li>
 * </ul>
 * The default character encoding is the platform native encoding.
 */
@Retention(CLASS)
@Target({ PARAMETER, METHOD })
public @interface AsCharset {
    /**
     * {@return the character set name}
     */
    String value();
}
