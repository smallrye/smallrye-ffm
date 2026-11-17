package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.AsType;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Out;

/**
 * Tests for {@code @Out String[]} parameters ({@code const char **} out-parameters).
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

    @Link
    @As(AsType.stdc_long)
    private static native long strtol(String str, @Out String[] endptr, int base);
}
