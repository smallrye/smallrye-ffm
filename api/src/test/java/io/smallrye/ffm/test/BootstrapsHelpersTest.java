package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.invoke.MethodHandleInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.smallrye.ffm.Bootstraps;

/**
 * {@code Bootstraps} creates method handles to its own private conversion helpers by name and descriptor.
 * On HotSpot such a reference is only resolved when the branch holding it runs, so a missing helper goes unnoticed
 * until a particular combination of Java and native types is linked; GraalVM {@code native-image} resolves them all
 * and fails the build. This test checks every such reference against the methods the class actually declares.
 */
public final class BootstrapsHelpersTest {
    private static final String BOOTSTRAPS = Bootstraps.class.getName().replace('.', '/');

    public BootstrapsHelpersTest() {
    }

    @Test
    public void testEveryHelperReferencedByNameIsDeclared() throws IOException {
        ClassModel cm = parse(Bootstraps.class);
        Set<String> declared = new TreeSet<>();
        for (MethodModel mm : cm.methods()) {
            declared.add(mm.methodName().stringValue() + mm.methodType().stringValue());
        }
        Set<String> referenced = new TreeSet<>();
        // method handle constants (the build folds `staticMethod(Bootstraps.class, ...)` calls into these)
        for (PoolEntry pe : cm.constantPool()) {
            if (pe instanceof MethodHandleEntry mhe
                    && mhe.kind() == MethodHandleInfo.REF_invokeStatic
                    && mhe.reference().owner().asInternalName().equals(BOOTSTRAPS)) {
                referenced.add(mhe.reference().name().stringValue() + mhe.reference().type().stringValue());
            }
        }
        // `staticMethod(Bootstraps.class, "name", "descriptor", ...)` calls which were not folded
        for (MethodModel mm : cm.methods()) {
            CodeModel code = mm.code().orElse(null);
            if (code == null) {
                continue;
            }
            List<LoadableConstantEntry> constants = new ArrayList<>();
            for (var element : code) {
                if (element instanceof ConstantInstruction.LoadConstantInstruction lci) {
                    constants.add(lci.constantEntry());
                } else if (element instanceof InvokeInstruction ii
                        && ii.owner().asInternalName().equals(BOOTSTRAPS)
                        && ii.name().equalsString("staticMethod")) {
                    // the preceding constants are owner class, name, descriptor (then the isInterface flag)
                    int n = constants.size();
                    if (n >= 3 && constants.get(n - 3) instanceof ClassEntry owner
                            && owner.asInternalName().equals(BOOTSTRAPS)
                            && constants.get(n - 2) instanceof StringEntry name
                            && constants.get(n - 1) instanceof StringEntry desc) {
                        referenced.add(name.stringValue() + desc.stringValue());
                    }
                    constants.clear();
                } else if (element instanceof Instruction) {
                    if (!(element instanceof ConstantInstruction)) {
                        // keep only an uninterrupted run of constant loads
                        constants.clear();
                    }
                }
            }
        }
        assertFalse(referenced.isEmpty(), "No by-name helper references found; is the scan still valid?");
        Set<String> missing = new TreeSet<>(referenced);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(), "Bootstraps references helpers it does not declare: " + missing);
    }

    private static ClassModel parse(Class<?> clazz) throws IOException {
        try (InputStream is = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
            assertNotNull(is, "Class file not found for " + clazz);
            return ClassFile.of().parse(is.readAllBytes());
        }
    }
}
