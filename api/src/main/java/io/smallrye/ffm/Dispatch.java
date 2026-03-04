package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Create a downcall dispatcher for the annotated native method with the given configuration.
 * <p>
 * The method must be declared to be {@code native}.
 * If the method is not {@code static}, the receiver is ignored on invocation.
 * <p>
 * The first argument must be of type {@code MemorySegment} and cannot be {@code null}.
 * The subsequent arguments are interpreted in the same manner as if the {@link Link @Link} annotation
 * had been given.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Dispatch {
}
