package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Lib;
import io.smallrye.ffm.Link;

/**
 * Tests for linking against libraries loaded by the class loader, via {@code <<LOADER>>}.
 */
public final class LoaderLookupTest {

    /**
     * Construct a new instance.
     */
    public LoaderLookupTest() {
    }

    /**
     * Verify that a function from a library loaded with {@link System#load(String)}
     * is found through {@code @Lib("<<LOADER>>")}.
     */
    @Test
    public void testLoadedLibraryIsFound() {
        assumeTrue(LoaderLookupTest.class.getModule().isNativeAccessEnabled());
        String[] candidates = {
                "/opt/homebrew/lib/libz.dylib", // macOS Homebrew (Apple Silicon)
                "/usr/local/lib/libz.dylib", // macOS Homebrew (Intel)
                "/usr/lib/x86_64-linux-gnu/libz.so.1", // Debian/Ubuntu x86_64
                "/usr/lib64/libz.so.1", // Fedora/RHEL x86_64
                "/usr/lib/libz.so.1", // Arch Linux
        };
        String foundPath = null;
        for (String candidate : candidates) {
            if (Files.exists(Path.of(candidate))) {
                foundPath = candidate;
                break;
            }
        }
        assumeTrue(foundPath != null, "zlib not found at any known absolute path");
        System.load(foundPath);
        assertNotNull(zlibVersion());
    }

    @Link
    @Lib("<<LOADER>>")
    private static native String zlibVersion();
}
