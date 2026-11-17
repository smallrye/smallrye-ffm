package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.size_t;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Variadic;

/**
 * Tests for arguments narrower than {@code int}, including variadic arguments, which are subject to the C
 * default argument promotions.
 */
public final class NarrowArgumentTest {

    /**
     * Construct a new instance.
     */
    public NarrowArgumentTest() {
    }

    /**
     * Test that a {@code boolean} variadic argument is passed as an {@code int}.
     */
    @Test
    public void testBoolean() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals("1", format(buf -> snprintfBoolean(buf, 32, "%d", true)));
        assertEquals("0", format(buf -> snprintfBoolean(buf, 32, "%d", false)));
    }

    /**
     * Test that a {@code byte} variadic argument is passed as a sign-extended {@code int}.
     */
    @Test
    public void testByte() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals("-7", format(buf -> snprintfByte(buf, 32, "%d", (byte) -7)));
    }

    /**
     * Test that a {@code short} variadic argument is passed as a sign-extended {@code int}.
     */
    @Test
    public void testShort() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals("-300", format(buf -> snprintfShort(buf, 32, "%d", (short) -300)));
    }

    /**
     * Test that a {@code char} variadic argument is passed as a zero-extended {@code int}.
     */
    @Test
    public void testChar() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals("65", format(buf -> snprintfChar(buf, 32, "%d", 'A')));
        assertEquals("65535", format(buf -> snprintfChar(buf, 32, "%d", '￿')));
    }

    /**
     * Test that a {@code char} argument, which is an unsigned 16-bit value passed as an {@code int}, is linked.
     */
    @Test
    public void testCharParameter() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals(65, abs('A'));
        assertEquals(65535, abs('\uffff'));
    }

    private static String format(Function<MemorySegment, Integer> call) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(32);
            int len = call.apply(buf);
            String result = buf.getString(0);
            assertEquals(result.length(), len);
            return result;
        }
    }

    @Link
    private static native int abs(char value);

    @Link(name = "snprintf")
    private static native int snprintfBoolean(MemorySegment buf, @As(size_t) long size, String format,
            @Variadic boolean value);

    @Link(name = "snprintf")
    private static native int snprintfByte(MemorySegment buf, @As(size_t) long size, String format, @Variadic byte value);

    @Link(name = "snprintf")
    private static native int snprintfShort(MemorySegment buf, @As(size_t) long size, String format, @Variadic short value);

    @Link(name = "snprintf")
    private static native int snprintfChar(MemorySegment buf, @As(size_t) long size, String format, @Variadic char value);
}
