package io.smallrye.ffm;

import java.util.NoSuchElementException;

/**
 * An object which has a native value that fits into an {@code int}.
 * Objects of this type may be passed into a native method.
 * The default conversion will be {@link AsType#stdc_int}.
 * <p>
 * To support being used as an output value from native methods,
 * classes of this type should also contain a {@code public static} method
 * named {@code ofNativeValue} which accepts an {@code int} and returns
 * an instance of the class itself.
 * The method may throw {@link NoSuchElementException} if the native value
 * does not correspond to a known instance, or it may return a sentinel
 * value which indicates that the value is unknown.
 */
public interface NativeIntValued {
    /**
     * {@return the native value of this object}
     *
     * @throws NoSuchElementException if there is no native value for this object on this platform
     */
    int nativeValue() throws NoSuchElementException;

    /**
     * {@return {@code true} if there is a native value for this object}
     */
    boolean isPresent();
}
