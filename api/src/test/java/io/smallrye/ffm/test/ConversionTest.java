package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.*;
import static java.lang.foreign.ValueLayout.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Dispatch;
import io.smallrye.ffm.Link;

/**
 * Value conversions between Java types and native types of a different width or kind.
 * The native side is an upcall stub, so the test controls exactly which native value is passed or returned.
 */
public final class ConversionTest {
    public ConversionTest() {
    }

    @BeforeEach
    public void checkNativeAccess() {
        assumeTrue(ConversionTest.class.getModule().isNativeAccessEnabled());
    }

    // identity functions, called back from native code

    private static byte idByte(byte v) {
        return v;
    }

    private static short idShort(short v) {
        return v;
    }

    private static int idInt(int v) {
        return v;
    }

    private static long idLong(long v) {
        return v;
    }

    private static int boolToInt(boolean v) {
        return v ? 1 : 0;
    }

    private static MemorySegment upcall(String name, Class<?> ret, Class<?> arg, FunctionDescriptor fd) {
        try {
            return Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findStatic(ConversionTest.class, name, MethodType.methodType(ret, arg)),
                    fd, Arena.ofAuto());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final MemorySegment ID_BYTE = upcall("idByte", byte.class, byte.class,
            FunctionDescriptor.of(JAVA_BYTE, JAVA_BYTE));
    private static final MemorySegment ID_SHORT = upcall("idShort", short.class, short.class,
            FunctionDescriptor.of(JAVA_SHORT, JAVA_SHORT));
    private static final MemorySegment ID_INT = upcall("idInt", int.class, int.class,
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MemorySegment ID_LONG = upcall("idLong", long.class, long.class,
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MemorySegment BOOL_TO_INT = upcall("boolToInt", int.class, boolean.class,
            FunctionDescriptor.of(JAVA_INT, JAVA_BOOLEAN));

    // boolean <- byte/short/char

    @Test
    public void testS8ReturnAsBoolean() {
        assertFalse(s8AsBoolean(ID_BYTE, (byte) 0));
        assertTrue(s8AsBoolean(ID_BYTE, (byte) 1));
        assertTrue(s8AsBoolean(ID_BYTE, (byte) 0x80));
    }

    @Test
    public void testS16ReturnAsBoolean() {
        assertFalse(s16AsBoolean(ID_SHORT, (short) 0));
        assertTrue(s16AsBoolean(ID_SHORT, (short) 0x100));
        assertTrue(s16AsBoolean(ID_SHORT, (short) -1));
    }

    @Test
    public void testBooleanParamFromByteShortChar() {
        assertEquals(0, byteAsBool(BOOL_TO_INT, (byte) 0));
        assertEquals(1, byteAsBool(BOOL_TO_INT, (byte) 2));
        assertEquals(0, shortAsBool(BOOL_TO_INT, (short) 0));
        assertEquals(1, shortAsBool(BOOL_TO_INT, (short) 0x100));
        assertEquals(0, charAsBool(BOOL_TO_INT, '\0'));
        assertEquals(1, charAsBool(BOOL_TO_INT, 'Ā'));
    }

    // byte <- short

    @Test
    public void testShortToS8() {
        assertEquals((byte) -1, shortAsS8(ID_BYTE, (short) 0x1ff));
        assertEquals((byte) 0x7f, s16AsByte(ID_SHORT, (short) 0x17f));
    }

    // char <- int

    @Test
    public void testS32ReturnAsChar() {
        assertEquals('A', s32AsChar(ID_INT, 0x1_0041));
        assertEquals('￿', s32AsChar(ID_INT, -1));
    }

    @Test
    public void testLibcIntReturnAsChar() {
        assertEquals('A', toupper('a'));
        assertEquals('1', toupper('1'));
        assertTrue(isdigit('7'));
        assertFalse(isdigit('x'));
    }

    // float/double <- unsigned 64-bit

    @Test
    public void testU64ReturnAsDouble() {
        for (long v : U64_SAMPLES) {
            assertEquals(new BigDecimal(Long.toUnsignedString(v)).doubleValue(), u64AsDouble(ID_LONG, v),
                    Long.toUnsignedString(v));
        }
    }

    @Test
    public void testU64ReturnAsFloat() {
        for (long v : U64_SAMPLES) {
            assertEquals(new BigDecimal(Long.toUnsignedString(v)).floatValue(), u64AsFloat(ID_LONG, v),
                    Long.toUnsignedString(v));
        }
    }

    private static final long[] U64_SAMPLES = {
            0, 1, 12345, Long.MAX_VALUE, Long.MIN_VALUE, -1,
            // values whose lowest bit decides the rounding
            0x8000_0000_0000_0401L, 0x8000_0000_0000_0400L, 0x8000_0080_0000_0001L, 0xffff_ffff_ffff_fbffL,
    };

    @Dispatch
    private static native @As(s8) boolean s8AsBoolean(MemorySegment fn, byte v);

    @Dispatch
    private static native @As(s16) boolean s16AsBoolean(MemorySegment fn, short v);

    @Dispatch
    private static native int byteAsBool(MemorySegment fn, @As(stdc_bool) byte v);

    @Dispatch
    private static native int shortAsBool(MemorySegment fn, @As(stdc_bool) short v);

    @Dispatch
    private static native int charAsBool(MemorySegment fn, @As(stdc_bool) char v);

    @Dispatch
    private static native byte shortAsS8(MemorySegment fn, @As(s8) short v);

    @Dispatch
    private static native @As(s16) byte s16AsByte(MemorySegment fn, short v);

    @Dispatch
    private static native @As(s32) char s32AsChar(MemorySegment fn, int v);

    @Dispatch
    private static native @As(u64) double u64AsDouble(MemorySegment fn, long v);

    @Dispatch
    private static native @As(u64) float u64AsFloat(MemorySegment fn, long v);

    @Link
    private static native @As(stdc_int) char toupper(@As(stdc_int) char c);

    @Link
    private static native @As(stdc_int) boolean isdigit(@As(stdc_int) char c);
}
