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

    @Link
    private static native @As(stdc_int) long atoi(String numberString);

    @Link
    private static native @As(stdc_long) long atol(String numberString);

    @Link
    private static native @As(stdc_long_long) long atoll(String numberString);

    @Link(name = "atoi")
    private static native Errno atoi_as_errno(String numberString);
}
