package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Lib;
import io.smallrye.ffm.Link;

/**
 * Tests for {@code @Lib} values which are absolute paths.
 */
public final class AbsoluteLibPathTest {

    private static final String DEBIAN_ZLIB = "/usr/lib/x86_64-linux-gnu/libz.so.1";
    private static final String RHEL_ZLIB = "/usr/lib64/libz.so.1";

    /**
     * Construct a new instance.
     */
    public AbsoluteLibPathTest() {
    }

    /**
     * Verify that a function is linked from a library given by absolute path (Debian/Ubuntu layout).
     */
    @Test
    public void testDebianPath() {
        assumeTrue(AbsoluteLibPathTest.class.getModule().isNativeAccessEnabled());
        assumeTrue(Files.exists(Path.of(DEBIAN_ZLIB)), "zlib not found at " + DEBIAN_ZLIB);
        assertNotNull(zlibVersionDebian());
    }

    /**
     * Verify that a function is linked from a library given by absolute path (Fedora/RHEL layout).
     */
    @Test
    public void testRhelPath() {
        assumeTrue(AbsoluteLibPathTest.class.getModule().isNativeAccessEnabled());
        assumeTrue(Files.exists(Path.of(RHEL_ZLIB)), "zlib not found at " + RHEL_ZLIB);
        assertNotNull(zlibVersionRhel());
    }

    @Link(name = "zlibVersion")
    @Lib(DEBIAN_ZLIB)
    private static native String zlibVersionDebian();

    @Link(name = "zlibVersion")
    @Lib(RHEL_ZLIB)
    private static native String zlibVersionRhel();
}
