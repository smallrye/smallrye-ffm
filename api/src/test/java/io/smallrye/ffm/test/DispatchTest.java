package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Dispatch;

public final class DispatchTest {
    public DispatchTest() {
    }

    @Test
    public void testDispatch() {
        assumeTrue(DispatchTest.class.getModule().isNativeAccessEnabled());
        MemorySegment tan = Linker.nativeLinker().defaultLookup().find("tan").orElseThrow();
        assertEquals(0.0, dispatch(tan, 0.0), 0.01);
        assertEquals(1.0, dispatch(tan, Math.PI * 0.25), 0.01);
    }

    @Dispatch
    private static native double dispatch(MemorySegment ddFunc, double arg);
}
