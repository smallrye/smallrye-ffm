package io.smallrye.ffm;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Create a function pointer which calls a static method of the same class from native code.
 * <p>
 * The annotated method must be declared {@code static native}, take a single {@link java.lang.foreign.Arena Arena}
 * parameter and return {@link java.lang.foreign.MemorySegment MemorySegment}. Each call returns a new upcall stub
 * allocated in the given arena; the stub is valid while the arena is alive.
 * <p>
 * The {@linkplain #value() target} is a {@code static} method of the same class. Its parameters and return value are
 * mapped like the parameters and return value of a {@link Link @Link} method, in the other direction:
 * <ul>
 * <li>A primitive parameter or return value can carry an {@link As @As} annotation to give its native type.</li>
 * <li>A {@code MemorySegment} parameter receives the native pointer as a zero-length segment.</li>
 * <li>A {@code String} parameter receives a copy of the NUL-terminated UTF-8 string the native pointer refers to,
 * or {@code null} for a null pointer. The copy is made before the target runs, so the native side may free the string
 * after the call.</li>
 * </ul>
 * <h2>Exceptions</h2>
 * An exception which escapes an upcall terminates the JVM. So if the target throws, the exception is passed to the
 * {@linkplain Thread#getUncaughtExceptionHandler() uncaught exception handler} of the calling thread, and the upcall
 * returns zero, {@code false} or a null pointer to native code.
 * <h2>Native image</h2>
 * GraalVM {@code native-image} needs every upcall shape at build time. The function descriptor of the stub must be
 * registered for upcalls, for example with {@code RuntimeForeignAccess.registerForUpcall}.
 *
 * <pre>
 * {@code @Upcall}("log")
 * private static native MemorySegment logCallback(Arena arena);
 *
 * private static void log(MemorySegment data, String ruleMessage) {
 *     ...
 * }
 * </pre>
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Upcall {
    /**
     * {@return the name of the {@code static} method of the same class which the function pointer calls}
     * There must be exactly one method with this name.
     */
    String value();
}
