package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.*;
import static java.lang.foreign.ValueLayout.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Dispatch;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Upcall;

/**
 * Function pointers created with {@code @Upcall}, called from native code.
 */
public final class UpcallTest {

    public UpcallTest() {
    }

    @BeforeEach
    public void checkNativeAccess() {
        assumeTrue(UpcallTest.class.getModule().isNativeAccessEnabled());
    }

    // qsort calls the comparator from native code

    @Link
    private static native void qsort(MemorySegment base, @As(size_t) long count, @As(size_t) long size,
            MemorySegment compare);

    @Upcall("compareInts")
    private static native MemorySegment compareIntsStub(Arena arena);

    private static int compareInts(MemorySegment a, MemorySegment b) {
        return Integer.compare(a.reinterpret(4).get(JAVA_INT, 0), b.reinterpret(4).get(JAVA_INT, 0));
    }

    @Test
    public void testQsortComparator() {
        int[] values = { 42, -7, 1000, 0, 13, -7, 5 };
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment array = arena.allocateFrom(JAVA_INT, values);
            qsort(array, values.length, JAVA_INT.byteSize(), compareIntsStub(arena));
            assertArrayEquals(new int[] { -7, -7, 0, 5, 13, 42, 1000 }, array.toArray(JAVA_INT));
        }
    }

    // a dispatcher calls the stub, so the test controls the native arguments

    private static final AtomicReference<Object> received = new AtomicReference<>();

    @Dispatch
    private static native void callWithString(MemorySegment fn, String value);

    @Upcall("onString")
    private static native MemorySegment onStringStub(Arena arena);

    private static void onString(String value) {
        received.set(value);
    }

    @Test
    public void testStringArgumentIsCopied() {
        try (Arena arena = Arena.ofConfined()) {
            callWithString(onStringStub(arena), "héllo wörld");
            assertEquals("héllo wörld", received.get());
        }
    }

    @Dispatch
    private static native void callWithByte(MemorySegment fn, @As(u8) int value);

    @Upcall("onUnsignedByte")
    private static native MemorySegment onUnsignedByteStub(Arena arena);

    private static void onUnsignedByte(@As(u8) int value) {
        received.set(value);
    }

    @Test
    public void testUnsignedArgument() {
        try (Arena arena = Arena.ofConfined()) {
            callWithByte(onUnsignedByteStub(arena), 0xff);
            assertEquals(255, received.get());
        }
    }

    @Dispatch
    private static native @As(stdc_int) long callForLong(MemorySegment fn, @As(stdc_int) long value);

    @Upcall("twice")
    private static native MemorySegment twiceStub(Arena arena);

    private static @As(stdc_int) long twice(@As(stdc_int) long value) {
        return value * 2;
    }

    @Test
    public void testReturnValue() {
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(-84, callForLong(twiceStub(arena), -42));
        }
    }

    // an exception must not reach native code

    @Dispatch
    private static native int callForInt(MemorySegment fn);

    @Upcall("fail")
    private static native MemorySegment failStub(Arena arena);

    private static int fail() {
        throw new IllegalStateException("failed in the upcall");
    }

    @Test
    public void testExceptionGoesToTheUncaughtExceptionHandler() {
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = thread.getUncaughtExceptionHandler();
        AtomicReference<Throwable> caught = new AtomicReference<>();
        thread.setUncaughtExceptionHandler((t, e) -> caught.set(e));
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(0, callForInt(failStub(arena)));
            assertInstanceOf(IllegalStateException.class, caught.get());
            assertEquals("failed in the upcall", caught.get().getMessage());
        } finally {
            thread.setUncaughtExceptionHandler(previous);
        }
    }
}
