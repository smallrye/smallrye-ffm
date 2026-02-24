package io.smallrye.ffm.test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

public final class TestUtils {

    private static final StackWalker SW = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static String getFullStack() {
        return SW.walk(s -> {
            StringBuilder b = new StringBuilder();
            s.forEach(sf -> b.append(sf.toStackTraceElement()).append(" @").append(sf.getByteCodeIndex()).append('\n'));
            return b.toString();
        });
    }

    public static String dumpMethod(Class<?> clazz, String methodName) {
        ClassFile cf = ClassFile.of();
        InputStream is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
        if (is == null) {
            throw new IllegalArgumentException("Class not found: " + clazz.getName());
        }
        ClassModel cm;
        try (var __ = is) {
            cm = cf.parse(is.readAllBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open class file " + clazz.getName(), e);
        }
        StringBuilder b = new StringBuilder(1024);
        for (ClassElement ce : cm) {
            if (ce instanceof MethodModel mm && mm.methodName().equalsString(methodName)) {
                b.append(mm.toDebugString());
                b.append('\n');
            }
        }
        if (b.isEmpty()) {
            throw new IllegalArgumentException("No methods named " + methodName + " foud on " + clazz);
        }
        return b.toString();
    }
}
