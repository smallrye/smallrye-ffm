package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;

/**
 * Indicate that the annotated parameter (of type {@link MemorySegment})
 * is to be used as the capture buffer for call state capture.
 * <p>
 * It is usually preferable to use a {@link ErrnoConsumer}, a {@link LastErrorConsumer},
 * and/or a {@link WSALastErrorConsumer} instead for simplicity.
 * <p>
 * The memory segment should be allocated with a layout of {@link Linker.Option#captureStateLayout()}.
 */
@Target(PARAMETER)
@Retention(CLASS)
public @interface Capture {
    /**
     * {@return {@code true} to always catch {@code errno}, or {@code false} to only catch {@code errno} if an
     * {@link ErrnoConsumer} is present}
     */
    boolean errno() default false;

    /**
     * {@return {@code true} to always catch {@code LastError}, or {@code false} to only catch {@code LastError} if a
     * {@link LastErrorConsumer} is present (Windows only)}
     */
    boolean lastError() default false;

    /**
     * {@return {@code true} to always catch {@code WSALastError}, or {@code false} to only catch {@code WSALastError} if a
     * {@link WSALastErrorConsumer} is present (Windows only)}
     */
    boolean wsaLastError() default false;
}
