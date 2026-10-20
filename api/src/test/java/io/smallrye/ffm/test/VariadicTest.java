package io.smallrye.ffm.test;

import static io.smallrye.ffm.AsType.size_t;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.As;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Variadic;

public final class VariadicTest {

    @Test
    public void testDescriptorsCarryVariadicMarker() throws IOException {
        Set<String> descriptors = downcallDescriptors();
        assertTrue(descriptors.contains("(*u**~sI)sI"), "parameter-level @Variadic in " + descriptors);
        assertTrue(descriptors.contains("(*u**~)sI"), "method-level @Variadic in " + descriptors);
    }

    @Test
    public void testVariadicArgument() {
        assumeTrue(VariadicTest.class.getModule().isNativeAccessEnabled());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(32);
            assertEquals(5, snprintf(buf, 32, "%d", 12345));
            assertEquals("12345", buf.getString(0));
        }
    }

    @Test
    public void testNoVariadicArguments() {
        assumeTrue(VariadicTest.class.getModule().isNativeAccessEnabled());
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(32);
            assertEquals(5, snprintfFixed(buf, 32, "hello"));
            assertEquals("hello", buf.getString(0));
        }
    }

    private static Set<String> downcallDescriptors() throws IOException {
        ClassModel model;
        try (InputStream in = VariadicTest.class.getResourceAsStream("VariadicTest.class")) {
            model = ClassFile.of().parse(Objects.requireNonNull(in).readAllBytes());
        }
        Set<String> descriptors = new HashSet<>();
        for (PoolEntry entry : model.constantPool()) {
            if (entry instanceof InvokeDynamicEntry indy) {
                MemberRefEntry bootstrap = indy.bootstrap().bootstrapMethod().reference();
                if (bootstrap.owner().name().equalsString("io/smallrye/ffm/Bootstraps")
                        && bootstrap.name().equalsString("downcall")) {
                    descriptors.add(indy.name().stringValue());
                }
            }
        }
        return descriptors;
    }

    /**
     * Formats one integer.
     *
     * @param buf the output buffer (must not be {@code null})
     * @param size the buffer size
     * @param format the format string (must not be {@code null})
     * @param value the variadic integer argument
     * @return the number of characters that would have been written
     */
    @Link
    private static native int snprintf(MemorySegment buf, @As(size_t) long size, String format, @Variadic int value);

    /**
     * Formats a string that takes no arguments.
     *
     * @param buf the output buffer (must not be {@code null})
     * @param size the buffer size
     * @param format the format string (must not be {@code null})
     * @return the number of characters that would have been written
     */
    @Link(name = "snprintf")
    @Variadic
    private static native int snprintfFixed(MemorySegment buf, @As(size_t) long size, String format);
}
