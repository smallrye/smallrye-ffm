package io.smallrye.ffm;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * Link the annotated native method with the given configuration.
 * <p>
 * The method must be declared to be {@code native}.
 * If the method is not {@code static}, the receiver is ignored on invocation.
 * <h2>Valid argument types</h2>
 * The type of each method parameter must be one of:
 * <ul>
 * <li>A primitive type</li>
 * <li>An array of primitive type</li>
 * <li>{@link MemorySegment}</li>
 * <li>{@link String}</li>
 * <li>{@link Errno}</li>
 * <li>{@link ErrnoConsumer}</li>
 * <li>{@link LastErrorConsumer}</li>
 * <li>{@link WSALastErrorConsumer}</li>
 * <li>{@link Arena} or {@link SegmentAllocator} (max 1)</li>
 * </ul>
 * Additionally, a method parameter may be annotated using the {@link As @As} annotation,
 * which is used to modify the native type and/or copying behavior of the parameter.
 * <h2>Return type</h2>
 * The return type must be one of:
 * <ul>
 * <li>{@code void}</li>
 * <li>A primitive type</li>
 * <li>An array of primitive type</li>
 * <li>{@link MemorySegment}</li>
 * <li>{@link String}</li>
 * <li>{@link Errno}</li>
 * </ul>
 * <h2>Custom arena</h2>
 * If a parameter is given whose type is {@code Arena}, then that arena will be used
 * for any temporary allocations which may be needed to copy input or output values.
 * This parameter is not propagated to the native method.
 * <h2>Copied arguments</h2>
 * A primitive array, a {@link MemorySegment} which is not {@linkplain MemorySegment#isNative() native}
 * (a heap segment), and a {@link String} are copied into native memory for the call
 * unless they are annotated with {@link Out @Out} but not {@link In @In},
 * and arrays and heap segments are copied back after the call
 * unless they are annotated with {@link In @In} but not {@link Out @Out}.
 * The copy is allocated from a confined arena which is closed when the method returns,
 * or from the {@code Arena} or {@code SegmentAllocator} parameter if there is one.
 * So a native function which keeps the pointer after it returns (for example, one which reads
 * a buffer later) must be given a native {@code MemorySegment} from an arena which lives as long as
 * the native code uses it, or such an arena parameter; with a temporary copy it would later read
 * memory which has been freed and reused.
 * A {@code MemorySegment} which is already native is passed as it is.
 * With {@link Critical @Critical(heap = true)}, arrays and heap segments are passed without a copy,
 * and the pointer is only valid during the call.
 * <h2>{@code errno} handling</h2>
 * If a parameter is given whose type is {@code ErrnoConsumer}, then the value
 * of {@code errno} will be propagated to that consumer before the method returns.
 * This parameter is not propagated to the native method.
 * Note that {@code errno} is generally not cleared by libraries, so checking {@code errno}
 * alone is insufficient to determine whether an error has occurred (it may retain a value
 * from a previous call).
 *
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Link {

    /**
     * {@return the name of the symbol to link (defaults to the member name)}
     */
    String name() default "";
}
