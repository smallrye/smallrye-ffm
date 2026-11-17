package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.size_t;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.NativeIntValued;
import io.smallrye.ffm.Variadic;

/**
 * Tests for {@link NativeIntValued} parameters.
 */
public final class NativeIntValuedParamTest {

    /**
     * Construct a new instance.
     */
    public NativeIntValuedParamTest() {
    }

    /**
     * Test that a {@code NativeIntValued} implementation is passed as its native value.
     */
    @Test
    public void testParameter() {
        assumeTrue(NativeIntValuedParamTest.class.getModule().isNativeAccessEnabled());
        assertEquals(5, abs(Level.LOW));
        assertEquals(4, abs(Level.HIGH));
    }

    /**
     * Test that a {@code NativeIntValued} implementation is passed as its native value in a variadic position.
     */
    @Test
    public void testVariadicParameter() {
        assumeTrue(NativeIntValuedParamTest.class.getModule().isNativeAccessEnabled());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(32);
            assertEquals(2, snprintf(buf, 32, "%d", Level.LOW));
            assertEquals("-5", buf.getString(0));
        }
    }

    /**
     * A type with a native value which differs from its ordinal.
     */
    public enum Level implements NativeIntValued {
        LOW,
        HIGH,
        ;

        public int nativeValue() {
            return ordinal() == 0 ? -5 : 4;
        }

        public boolean isPresent() {
            return true;
        }
    }

    @Link
    private static native int abs(Level value);

    @Link
    private static native int snprintf(MemorySegment buf, @As(size_t) long size, String format, @Variadic Level value);
}
