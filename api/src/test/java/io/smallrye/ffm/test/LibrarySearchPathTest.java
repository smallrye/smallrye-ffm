package io.smallrye.ffm.test;

import static java.lang.invoke.MethodHandles.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Bootstraps;

/**
 * Tests for {@link Bootstraps#setLibrarySearchPaths(MethodHandles.Lookup, String...)},
 * absolute path handling in {@link Bootstraps#libraryLookup}, and search path fallback.
 */
public final class LibrarySearchPathTest {

    /**
     * Construct a new instance.
     */
    public LibrarySearchPathTest() {
    }

    /**
     * Verify that a restricted (non-full-privilege) lookup is rejected
     * by {@link Bootstraps#setLibrarySearchPaths}.
     */
    @Test
    public void testRestrictedLookupRejected() {
        assertThrows(SecurityException.class, () -> Bootstraps.setLibrarySearchPaths(publicLookup(), "/nonexistent"));
    }

    /**
     * Verify that a full-privilege lookup is accepted
     * by {@link Bootstraps#setLibrarySearchPaths}.
     */
    @Test
    public void testFullPrivilegeLookupAccepted() {
        // should not throw; registering bogus paths is fine — they just won't match at load time
        Bootstraps.setLibrarySearchPaths(lookup(), "/nonexistent/path");
    }

    /**
     * Verify that when search paths are set but none contain the library,
     * the standard OS library search mechanism is used as a fallback.
     */
    @Test
    public void testFallbackToStandardLookup() {
        assumeTrue(LibrarySearchPathTest.class.getModule().isNativeAccessEnabled());
        // register a bogus search path for this class
        Bootstraps.setLibrarySearchPaths(lookup(), "/nonexistent/path/that/does/not/exist");
        // the standard lookup should still find libc functions via the default lookup
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup defaultLookup = Bootstraps.defaultLookup(lookup(), "_", SymbolLookup.class,
                    Bootstraps.emptySymbolLookup(lookup(), "_", SymbolLookup.class));
            assertTrue(defaultLookup.find("strlen").isPresent(), "strlen should be found via default lookup");
        }
    }

    /**
     * Verify that an absolute path in the library name is loaded directly
     * without applying {@link System#mapLibraryName(String)}.
     * Uses zlib if it can be found at a known absolute path.
     */
    @Test
    public void testAbsolutePathLibraryLookup() {
        assumeTrue(LibrarySearchPathTest.class.getModule().isNativeAccessEnabled());
        // try to find zlib at known absolute paths
        String[] candidates = {
                "/opt/homebrew/lib/libz.dylib", // macOS Homebrew (Apple Silicon)
                "/usr/local/lib/libz.dylib", // macOS Homebrew (Intel)
                "/usr/lib/x86_64-linux-gnu/libz.so", // Debian/Ubuntu x86_64
                "/usr/lib64/libz.so", // Fedora/RHEL x86_64
                "/usr/lib/libz.so", // Arch Linux
        };
        String foundPath = null;
        for (String candidate : candidates) {
            if (java.nio.file.Files.exists(java.nio.file.Path.of(candidate))) {
                foundPath = candidate;
                break;
            }
        }
        assumeTrue(foundPath != null, "No zlib found at known absolute paths");
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup empty = Bootstraps.emptySymbolLookup(lookup(), "_", SymbolLookup.class);
            SymbolLookup zlib = Bootstraps.libraryLookup(lookup(), foundPath, SymbolLookup.class, arena, empty);
            assertTrue(zlib.find("zlibVersion").isPresent(),
                    "zlibVersion should be found when loading via absolute path: " + foundPath);
        }
    }

    /**
     * Verify that search paths are consulted before the standard lookup
     * by registering the directory containing zlib.
     */
    @Test
    public void testSearchPathFindsLibrary() {
        assumeTrue(LibrarySearchPathTest.class.getModule().isNativeAccessEnabled());
        // try to find zlib at known absolute paths, then extract the directory
        String[] candidates = {
                "/opt/homebrew/lib/libz.dylib",
                "/usr/local/lib/libz.dylib",
                "/usr/lib/x86_64-linux-gnu/libz.so",
                "/usr/lib64/libz.so",
                "/usr/lib/libz.so",
        };
        String foundDir = null;
        for (String candidate : candidates) {
            java.nio.file.Path path = java.nio.file.Path.of(candidate);
            if (java.nio.file.Files.exists(path)) {
                foundDir = path.getParent().toString();
                break;
            }
        }
        assumeTrue(foundDir != null, "No zlib found at known absolute paths");
        // register the directory containing zlib as a search path for this class
        Bootstraps.setLibrarySearchPaths(lookup(), foundDir);
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup empty = Bootstraps.emptySymbolLookup(lookup(), "_", SymbolLookup.class);
            SymbolLookup zlib = Bootstraps.libraryLookup(lookup(), "z", SymbolLookup.class, arena, empty);
            assertTrue(zlib.find("zlibVersion").isPresent(),
                    "zlibVersion should be found via search path: " + foundDir);
        }
    }
}
