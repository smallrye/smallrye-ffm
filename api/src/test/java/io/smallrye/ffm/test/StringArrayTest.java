package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;
import io.smallrye.ffm.As;
import io.smallrye.ffm.AsType;
import io.smallrye.ffm.In;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Out;

/**
 * Tests for {@code String[]} parameters (arrays of {@code char *}).
 */
public final class StringArrayTest {

    /**
     * Construct a new instance.
     */
    public StringArrayTest() {
    }

    /**
     * {@code strtol} stores a pointer to the first character after the number in {@code endptr}.
     */
    @Test
    public void testStringOut() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        String[] end = new String[1];
        assertEquals(123, strtol("123abc", end, 10));
        assertArrayEquals(new String[] { "abc" }, end);
    }

    /**
     * Elements which the callee leaves {@code NULL} are {@code null}.
     */
    @Test
    public void testStringOutNull() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        String[] end = { "x", "y" };
        assertEquals(42, strtol("42", end, 10));
        assertArrayEquals(new String[] { "", null }, end);
    }

    /**
     * Without {@code @In} or {@code @Out}, the elements are passed in and read back.
     */
    @Test
    public void testStringInOut() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        String[] end = { "ignored" };
        assertEquals(123, strtolInOut("123abc", end, 10));
        assertArrayEquals(new String[] { "abc" }, end);
    }

    /**
     * With {@code @In} only, the elements are not read back.
     */
    @Test
    public void testStringInOnly() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        String[] end = { "unchanged" };
        assertEquals(123, strtolIn("123abc", end, 10));
        assertArrayEquals(new String[] { "unchanged" }, end);
    }

    /**
     * A {@code null} array is passed as {@code NULL}.
     */
    @Test
    public void testNullArray() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        assertEquals(7, strtolInOut("7", null, 10));
    }

    /**
     * An input array is followed by a {@code NULL} element, so that functions which expect a {@code NULL}-terminated
     * array find its end, and {@code null} elements are passed as {@code NULL}.
     */
    @Test
    public void testNullTerminatedInput() {
        assumeTrue(StringArrayTest.class.getModule().isNativeAccessEnabled());
        assumeFalse(OS.current() == OS.WINDOWS);
        String[] option = { "rw=yes,ro" };
        String[] value = new String[1];
        assertEquals(1, getsubopt(option, new String[] { "ro", "rw" }, value));
        assertArrayEquals(new String[] { "ro" }, option);
        assertArrayEquals(new String[] { "yes" }, value);
        assertEquals(0, getsubopt(option, new String[] { "ro", "rw" }, value));
        assertArrayEquals(new String[] { "" }, option);
        // the null element ends the array, so "rw" is not found
        option[0] = "rw";
        assertEquals(-1, getsubopt(option, new String[] { "ro", null, "rw" }, value));
    }

    @Link
    @As(AsType.stdc_long)
    private static native long strtol(String str, @Out String[] endptr, int base);

    @Link(name = "strtol")
    @As(AsType.stdc_long)
    private static native long strtolInOut(String str, String[] endptr, int base);

    @Link(name = "strtol")
    @As(AsType.stdc_long)
    private static native long strtolIn(String str, @In String[] endptr, int base);

    @Link
    private static native int getsubopt(String[] optionp, @In String[] tokens, @Out String[] valuep);
}
