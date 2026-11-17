package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;
import io.smallrye.ffm.Lib;
import io.smallrye.ffm.Link;

/**
 * Tests for {@code @Lib} values which are library file names, such as a versioned shared library name.
 */
public final class LibFileNameTest {

    /**
     * Construct a new instance.
     */
    public LibFileNameTest() {
    }

    /**
     * Verify that a function is linked from a library given by its soname.
     */
    @Test
    public void testSoname() {
        assumeTrue(LibFileNameTest.class.getModule().isNativeAccessEnabled());
        assumeTrue(OS.current() == OS.LINUX);
        assertNotNull(zlibVersion());
    }

    @Link
    @Lib("libz.so.1")
    private static native String zlibVersion();
}
