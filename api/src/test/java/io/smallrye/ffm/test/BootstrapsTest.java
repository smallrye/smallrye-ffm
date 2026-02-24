package io.smallrye.ffm.test;

import static java.lang.invoke.MethodHandles.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Bootstraps;
import io.smallrye.ffm.Link;

public final class BootstrapsTest {
    public BootstrapsTest() {
    }

    @Test
    public void testBadLinkage() {
        CallSite cs = Bootstraps.linkSymbol(lookup(), "non-existent", MethodType.methodType(MemorySegment.class),
                Linker.nativeLinker().defaultLookup());
        assertThrows(UnsatisfiedLinkError.class, () -> cs.getTarget().invoke());
    }

    @Test
    public void testGoodLinkage() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        SymbolLookup symbolLookup = Linker.nativeLinker().defaultLookup();
        // use libc's "tan" function
        Optional<MemorySegment> tan = symbolLookup.find("tan");
        assumeTrue(tan.isPresent());
        // link it
        CallSite cs = Bootstraps.linkSymbol(lookup(), "tan", MethodType.methodType(MemorySegment.class), symbolLookup);
        MemorySegment seg = (MemorySegment) cs.getTarget().invoke();
        assertEquals(seg.address(), tan.get().address());
    }

    @Test
    public void testDowncall() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        SymbolLookup symbolLookup = Linker.nativeLinker().defaultLookup();
        // use libc's "tan" function
        Optional<MemorySegment> tan = symbolLookup.find("tan");
        assumeTrue(tan.isPresent());
        // make the downcall handle
        CallSite callSite = Bootstraps.downcall(lookup(), "(D)D",
                MethodType.methodType(double.class, MemorySegment.class, double.class));
        // handle for (double)double (accepts function pointer as first argument)
        MethodHandle tanHandle = callSite.getTarget();
        assertEquals(0.0, (double) tanHandle.invokeExact(tan.get(), 0.0));
        assertTrue(Double.isNaN((double) tanHandle.invokeExact(tan.get(), Double.POSITIVE_INFINITY)));
        // try some narrowing and widening conversions
        Bootstraps.downcall(lookup(), "(D)D", MethodType.methodType(float.class, MemorySegment.class, float.class));
        Bootstraps.downcall(lookup(), "(D)D", MethodType.methodType(int.class, MemorySegment.class, char.class));
        Bootstraps.downcall(lookup(), "(F)F", MethodType.methodType(double.class, MemorySegment.class, double.class));
    }

    @Test
    public void testDowncallCapture() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        MethodType expectedType = MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class, int.class);
        CallSite cs = Bootstraps.downcall(lookup(), "e(sI)sI", expectedType);
        assertEquals(expectedType, cs.getTarget().type());
    }

    @Test
    public void testLinkFailure() throws Throwable {
        assumeTrue(BootstrapsTest.class.getModule().isNativeAccessEnabled());
        assertThrows(UnsatisfiedLinkError.class, BootstrapsTest::non_existing_function);
    }

    @Link
    private static native void non_existing_function();
}
