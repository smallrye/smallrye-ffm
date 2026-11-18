package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.size_t;
import static io.smallrye.ffm.AsType.stdc_unsigned_char;
import static io.smallrye.ffm.AsType.stdc_unsigned_short;
import static io.smallrye.ffm.AsType.u16;
import static io.smallrye.ffm.AsType.u8;
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
     * Test that non-variadic unsigned 16-bit arguments are linked and passed correctly. Their layout is {@code int},
     * so their conversion must produce an {@code int} whatever the Java parameter type is.
     */
    @Test
    public void testUnsignedShortParameter() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals(65, abs('A'));
        assertEquals(65535, abs('\uffff'));
        assertEquals(0xfedc, absU16Short((short) 0xfedc));
        assertEquals(0xfedc, absU16Int(0xfedc));
        assertEquals(0xfedc, absU16Int(0x1fedc));
        assertEquals(0xfedc, absU16Long(0xfedcL));
    }

    /**
     * Test that non-variadic unsigned 8-bit arguments are passed correctly. Their layout is {@code int} too, and
     * their conversions already produce an {@code int}.
     */
    @Test
    public void testUnsignedByteParameter() {
        assumeTrue(NarrowArgumentTest.class.getModule().isNativeAccessEnabled());
        assertEquals(200, absByte((byte) 200));
        assertEquals(200, absShort((short) 200));
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

    @Link(name = "abs")
    private static native int absByte(@As(u8) byte value);

    @Link(name = "abs")
    private static native int absShort(@As(stdc_unsigned_char) short value);

    @Link(name = "abs")
    private static native int absU16Short(@As(u16) short value);

    @Link(name = "abs")
    private static native int absU16Int(@As(stdc_unsigned_short) int value);

    @Link(name = "abs")
    private static native int absU16Long(@As(u16) long value);

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
