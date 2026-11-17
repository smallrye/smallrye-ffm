package io.smallrye.ffm.maven;

import static java.lang.constant.ConstantDescs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.MethodParameterInfo;
import io.smallrye.classfile.attribute.MethodParametersAttribute;
import io.smallrye.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import io.smallrye.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;

/**
 * The transform goal rejects parameter and return types which it cannot map to a C type.
 */
public final class UnsupportedTypeTest {
    private static final Annotation LINK = Annotation.of(ClassDesc.of("io.smallrye.ffm.Link"));
    private static final Annotation OUT = Annotation.of(ClassDesc.of("io.smallrye.ffm.Out"));

    @TempDir
    Path dir;

    public UnsupportedTypeTest() {
    }

    @Test
    public void testStringArrayInputIsRejected() throws Exception {
        writeNative("strlenArray", MethodTypeDesc.of(CD_long, CD_String.arrayType()), List.of());
        MojoFailureException e = assertThrows(MojoFailureException.class, this::transform);
        assertTrue(e.getMessage().contains("org.example.Natives.strlenArray"), e.getMessage());
        assertTrue(e.getMessage().contains("parameter 0 (p0)"), e.getMessage());
        assertTrue(e.getMessage().contains("java.lang.String[]"), e.getMessage());
    }

    @Test
    public void testObjectParameterIsRejected() throws Exception {
        writeNative("takesObject", MethodTypeDesc.of(CD_int, CD_int, CD_Object), List.of());
        MojoFailureException e = assertThrows(MojoFailureException.class, this::transform);
        assertTrue(e.getMessage().contains("org.example.Natives.takesObject"), e.getMessage());
        assertTrue(e.getMessage().contains("parameter 1 (p1)"), e.getMessage());
        assertTrue(e.getMessage().contains("java.lang.Object"), e.getMessage());
    }

    @Test
    public void testNestedArrayParameterIsRejected() throws Exception {
        writeNative("takesMatrix", MethodTypeDesc.of(CD_void, CD_int.arrayType(2)), List.of());
        MojoFailureException e = assertThrows(MojoFailureException.class, this::transform);
        assertTrue(e.getMessage().contains("int[][]"), e.getMessage());
    }

    @Test
    public void testListReturnIsRejected() throws Exception {
        writeNative("returnsList", MethodTypeDesc.of(CD_List), List.of());
        MojoFailureException e = assertThrows(MojoFailureException.class, this::transform);
        assertTrue(e.getMessage().contains("org.example.Natives.returnsList"), e.getMessage());
        assertTrue(e.getMessage().contains("return type java.util.List"), e.getMessage());
    }

    @Test
    public void testArrayReturnIsRejected() throws Exception {
        writeNative("returnsBytes", MethodTypeDesc.of(CD_byte.arrayType()), List.of());
        MojoFailureException e = assertThrows(MojoFailureException.class, this::transform);
        assertTrue(e.getMessage().contains("return type byte[]"), e.getMessage());
    }

    @Test
    public void testStringArrayOutIsAccepted() throws Exception {
        writeNative("getString", MethodTypeDesc.of(CD_int, CD_String, CD_String.arrayType()),
                List.of(List.of(), List.of(OUT)));
        transform();
    }

    private void transform() throws Exception {
        TransformMojo mojo = new TransformMojo();
        Field field = TransformMojo.class.getDeclaredField("classesDirectory");
        field.setAccessible(true);
        field.set(mojo, dir.toFile());
        mojo.execute();
    }

    /**
     * Write a class with one {@code @Link} method, as {@code javac -parameters} would compile it.
     */
    private void writeNative(String name, MethodTypeDesc type, List<List<Annotation>> paramAnnotations) throws Exception {
        Path pkg = Files.createDirectories(dir.resolve("org/example"));
        byte[] bytes = ClassFile.of().build(ClassDesc.of("org.example.Natives"), zb -> {
            zb.withMethod(name, type, ClassFile.ACC_STATIC | ClassFile.ACC_NATIVE, mb -> {
                mb.with(RuntimeInvisibleAnnotationsAttribute.of(LINK));
                if (!paramAnnotations.isEmpty()) {
                    mb.with(RuntimeInvisibleParameterAnnotationsAttribute.of(paramAnnotations));
                }
                mb.with(MethodParametersAttribute.of(IntStream.range(0, type.parameterCount())
                        .mapToObj(i -> MethodParameterInfo.ofParameter(Optional.of("p" + i), 0))
                        .toList()));
            });
        });
        Files.write(pkg.resolve("Natives.class"), bytes);
        assertEquals(1, Files.list(pkg).count());
    }
}
