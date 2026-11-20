package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Errno;
import io.smallrye.ffm.Link;

public final class ReturnTypesTest {
    public ReturnTypesTest() {
    }

    @Test
    public void testStdcLongReturn() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        assertEquals(123, atol("123"));
        assertEquals(-123, atol("-123"));
        assertEquals(0, atol("0"));
    }

    @Test
    public void testStdcLongLongReturn() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        assertEquals(123, atoll("123"));
        assertEquals(-123, atoll("-123"));
        assertEquals(0, atoll("0"));
    }

    @Test
    public void testStdcIntReturn() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        assertEquals(123, atoi("123"));
        assertEquals(-123, atoi("-123"));
        assertEquals(0, atoi("0"));
    }

    @Test
    public void testErrnoReturn() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        assertEquals(Errno.EDOM, atoi_as_errno(String.valueOf(Errno.EDOM.nativeValue())));
    }

    /**
     * Test that unsigned 16-bit return values are linked and converted correctly. Their layout is {@code int}, so the
     * value must be narrowed to 16 bits before it is converted to the Java return type. {@code abs} is used because it
     * is available everywhere and returns its argument unchanged for these values.
     */
    @Test
    public void testUnsignedShortReturn() throws Throwable {
        assumeTrue(ReturnTypesTest.class.getModule().isNativeAccessEnabled());
        assertEquals('A', absChar(65));
        assertEquals('\uffff', absChar(0xffff));
        assertEquals('\ufedc', absChar(0x1fedc));
        assertEquals((short) 0xfedc, absU16Short(0xfedc));
        assertEquals(0xfedc, absU16Int(0xfedc));
        assertEquals(0xfedc, absU16Int(0x1fedc));
        assertEquals(0xfedcL, absU16Long(0xfedc));
        assertEquals(0xfedcL, absU16Long(0x1fedc));
    }

    /**
     * Test that unsigned 8-bit return values are linked and converted correctly. Their layout is {@code int} too.
     */
    @Test
    public void testUnsignedByteReturn() throws Throwable {
        assumeTrue(ReturnTypesTest.class.getModule().isNativeAccessEnabled());
        assertEquals((byte) 200, absU8Byte(200));
        assertEquals((short) 200, absU8Short(200));
        assertEquals(200, absU8Int(200));
        assertEquals(200, absU8Int(0x1c8));
        assertEquals(200L, absU8Long(200));
        assertEquals(200L, absU8Long(0x1c8));
    }

    /**
     * Test that signed 8- and 16-bit return values are sign-extended.
     */
    @Test
    public void testSignedNarrowReturn() throws Throwable {
        assumeTrue(ReturnTypesTest.class.getModule().isNativeAccessEnabled());
        assertEquals(-56, absS8Int(200));
        assertEquals(-56L, absS8Long(200));
        assertEquals(-292, absS16Int(0xfedc));
        assertEquals(-292L, absS16Long(0xfedc));
    }

    @Link
    private static native @As(stdc_int) long atoi(String numberString);

    @Link
    private static native @As(stdc_long) long atol(String numberString);

    @Link
    private static native @As(stdc_long_long) long atoll(String numberString);

    @Link(name = "atoi")
    private static native Errno atoi_as_errno(String numberString);

    @Link(name = "abs")
    private static native char absChar(int value);

    @Link(name = "abs")
    private static native @As(u16) short absU16Short(int value);

    @Link(name = "abs")
    private static native @As(stdc_unsigned_short) int absU16Int(int value);

    @Link(name = "abs")
    private static native @As(u16) long absU16Long(int value);

    @Link(name = "abs")
    private static native @As(u8) byte absU8Byte(int value);

    @Link(name = "abs")
    private static native @As(stdc_unsigned_char) short absU8Short(int value);

    @Link(name = "abs")
    private static native @As(u8) int absU8Int(int value);

    @Link(name = "abs")
    private static native @As(u8) long absU8Long(int value);

    @Link(name = "abs")
    private static native @As(s8) int absS8Int(int value);

    @Link(name = "abs")
    private static native @As(s8) long absS8Long(int value);

    @Link(name = "abs")
    private static native @As(s16) int absS16Int(int value);

    @Link(name = "abs")
    private static native @As(s16) long absS16Long(int value);
}
