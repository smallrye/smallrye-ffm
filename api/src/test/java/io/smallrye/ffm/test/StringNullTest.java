package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;
import io.smallrye.ffm.Link;

/**
 * Tests for {@code NULL} and long C strings mapped to {@code String}.
 */
public final class StringNullTest {

    /**
     * Construct a new instance.
     */
    public StringNullTest() {
    }

    /**
     * Test that a {@code NULL} string result is returned as {@code null}.
     */
    @Test
    public void testNullResult() {
        assumeTrue(StringNullTest.class.getModule().isNativeAccessEnabled());
        assertNull(getenv("SMALLRYE_FFM_SURELY_UNSET_VARIABLE"));
    }

    /**
     * Test that a string result is not truncated to a fixed length.
     */
    @Test
    public void testLongResult() {
        assumeTrue(StringNullTest.class.getModule().isNativeAccessEnabled());
        String expected = "x".repeat(5000);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment dest = arena.allocate(expected.length() + 1);
            assertEquals(expected, strcpy(dest, expected));
        }
    }

    /**
     * Test that a {@code null} string argument is passed as {@code NULL}.
     */
    @Test
    public void testNullArgument() {
        assumeTrue(StringNullTest.class.getModule().isNativeAccessEnabled());
        int lcAll = switch (OS.current()) {
            case LINUX -> 6;
            default -> 0;
        };
        // a NULL locale queries the current locale without changing it
        assertNotNull(setlocale(lcAll, null));
    }

    @Link
    private static native String getenv(String name);

    @Link
    private static native String strcpy(MemorySegment dest, String src);

    @Link
    private static native String setlocale(int category, String locale);
}
