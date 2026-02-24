package io.smallrye.ffm.maven;

import static io.smallrye.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.*;

import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.smallrye.classfile.AccessFlags;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.AnnotationValue;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassBuilder;
import io.smallrye.classfile.ClassElement;
import io.smallrye.classfile.CodeBuilder;
import io.smallrye.classfile.CodeElement;
import io.smallrye.classfile.CodeModel;
import io.smallrye.classfile.MethodElement;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.Opcode;
import io.smallrye.classfile.PseudoInstruction;
import io.smallrye.classfile.TypeKind;
import io.smallrye.classfile.attribute.CodeAttribute;
import io.smallrye.classfile.attribute.MethodParameterInfo;
import io.smallrye.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import io.smallrye.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;
import io.smallrye.classfile.constantpool.LoadableConstantEntry;
import io.smallrye.classfile.extras.reflect.AccessFlag;
import io.smallrye.classfile.instruction.ConstantInstruction;
import io.smallrye.classfile.instruction.InvokeInstruction;

public final class Generator {

    public static boolean processElement(final ClassBuilder zb, final ClassElement ce) {
        if (ce instanceof MethodModel mm
                && (processMethodForConstants(mm, zb) || processNativeMethod(mm, zb))) {
            return true;
        } else {
            zb.with(ce);
            return false;
        }
    }

    public static boolean processMethodForConstants(MethodModel mm, ClassBuilder zb) {
        if (mm.code().isEmpty()) {
            return false;
        }
        CodeModel cm = mm.code().get();
        List<CodeElement> elementList = cm.elementList();
        ArrayList<CodeElement> newElements = new ArrayList<>(elementList.size());
        boolean found = false;
        for (CodeElement ce : elementList) {
            newElements.add(ce);
            int size = newElements.size();
            int seek = size - 1;
            if (size >= 5 && newElements.get(seek) instanceof InvokeInstruction ii
                    && ii.opcode() == Opcode.INVOKESTATIC
                    && ii.owner().asInternalName().equals("io/smallrye/ffm/Bootstraps")
                    && ii.method().name().equalsString("staticMethod")) {
                while (newElements.get(--seek) instanceof PseudoInstruction) {
                }
                if (newElements.get(seek) instanceof ConstantInstruction.IntrinsicConstantInstruction ic3
                        && ic3.constantValue() instanceof Integer isInterface) {
                    while (newElements.get(--seek) instanceof PseudoInstruction) {
                    }
                    if (newElements.get(seek) instanceof ConstantInstruction.LoadConstantInstruction ldc2
                            && ldc2.constantValue() instanceof String methodTypeStr) {
                        while (newElements.get(--seek) instanceof PseudoInstruction) {
                        }
                        if (newElements.get(seek) instanceof ConstantInstruction.LoadConstantInstruction ldc1
                                && ldc1.constantValue() instanceof String methodName) {
                            while (newElements.get(--seek) instanceof PseudoInstruction) {
                            }
                            if (newElements.get(seek) instanceof ConstantInstruction.LoadConstantInstruction ldc0
                                    && ldc0.constantValue() instanceof ClassDesc owner) {
                                found = true;
                                newElements.subList(seek, size).clear();
                                LoadableConstantEntry lce = zb.constantPool().methodHandleEntry(
                                        MethodHandleDesc.ofMethod(
                                                isInterface.intValue() == 0 ? DirectMethodHandleDesc.Kind.STATIC
                                                        : DirectMethodHandleDesc.Kind.INTERFACE_STATIC,
                                                owner,
                                                methodName,
                                                MethodTypeDesc.ofDescriptor(methodTypeStr)));
                                newElements.add(ConstantInstruction.ofLoad(Opcode.LDC_W, lce));
                            }
                        }
                    }
                }
            }
        }
        if (found) {
            zb.withMethod(mm.methodName(), mm.methodType(), mm.flags().flagsMask(), mb -> {
                for (MethodElement me : mm.elementList()) {
                    if (me instanceof CodeAttribute) {
                        mb.withCode(cb -> newElements.forEach(cb::with));
                    } else {
                        mb.with(me);
                    }
                }
            });
        }
        return found;
    }

    public static boolean processNativeMethod(MethodModel mm, ClassBuilder zb) {
        // gather the method-level annotations
        Optional<RuntimeInvisibleAnnotationsAttribute> ria = mm.findAttribute(Attributes.runtimeInvisibleAnnotations());
        int variadic = -1;
        if (!mm.flags().has(AccessFlag.NATIVE)) {
            return false;
        }
        boolean critical = false;
        boolean heap = false;
        boolean link = false;
        String outputCharset = null;
        String name = mm.methodName().stringValue();
        ClassDesc returnType = mm.methodTypeSymbol().returnType();
        String retAsType = returnType.descriptorString().equals("V") ? "void" : defaultAsType(returnType);
        if (ria.isPresent()) {
            List<Annotation> annotations = ria.get().annotations();
            for (Annotation annotation : annotations) {
                switch (annotation.className().stringValue()) {
                    case "Lio/smallrye/ffm/Variadic;" -> {
                        if (variadic == -1) {
                            variadic = -2;
                        } else {
                            throw new IllegalArgumentException("Multiple variadic annotations not allowed");
                        }
                    }
                    case "Lio/smallrye/ffm/Link;" -> {
                        link = true;
                        for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "name" -> {
                                    if (value instanceof AnnotationValue.OfString os) {
                                        name = os.stringValue();
                                    }
                                }
                            }
                        }
                    }
                    case "Lio/smallrye/ffm/Critical;" -> {
                        critical = true;
                        for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "heap" -> {
                                    if (value instanceof AnnotationValue.OfBoolean ob) {
                                        heap = ob.booleanValue();
                                    }
                                }
                            }
                        }
                    }
                    case "Lio/smallrye/ffm/As;" -> {
                        for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "value" -> {
                                    if (value instanceof AnnotationValue.OfEnum oe) {
                                        retAsType = oe.constantName().stringValue();
                                    }
                                }
                            }
                        }
                    }
                    case "Lio/smallrye/ffm/AsCharset;" -> {
                        loop: for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "value" -> {
                                    if (value instanceof AnnotationValue.OfString os) {
                                        outputCharset = os.stringValue();
                                    }
                                    break loop;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!link) {
            return false;
        }

        MethodTypeDesc mtd = mm.methodTypeSymbol();
        int paramCnt = mtd.parameterCount();

        List<List<Annotation>> paListList = mm
                .findAttribute(Attributes.runtimeInvisibleParameterAnnotations())
                .map(RuntimeInvisibleParameterAnnotationsAttribute::parameterAnnotations)
                .orElse(Collections.nCopies(paramCnt, List.of()));

        int annOffs = paramCnt - paListList.size();

        Compilers compilers = new Compilers();
        List<Step> steps = new ArrayList<>(paramCnt * 2);
        int varOffs = mm.flags().has(AccessFlag.STATIC) ? 0 : 1;

        // LV indexes
        int capture = -1;

        // handle return right away
        steps.add(new ReturnStep(mtd.returnType(), retAsType));
        // resolve the symbol
        steps.add(new ResolvedSymbolStep(name, true));
        if (critical || heap) {
            steps.add(new CriticalStep(heap));
        }
        int insertionPoint = steps.size();
        int slot = varOffs;
        for (int i = 0; i < paramCnt; i++) {
            List<Annotation> annotations = i < annOffs ? List.of() : paListList.get(i);
            boolean in = false, out = false;
            String charset = null;
            String asType = defaultAsType(mtd.parameterType(i));
            // first check annotations
            for (Annotation annotation : annotations) {
                switch (annotation.className().stringValue()) {
                    case "Lio/smallrye/ffm/As;" -> {
                        // explicit type
                        loop: for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "value" -> {
                                    if (value instanceof AnnotationValue.OfEnum oe) {
                                        asType = oe.constantName().stringValue();
                                    }
                                    break loop;
                                }
                            }
                        }
                    }
                    case "Lio/smallrye/ffm/AsCharset;" -> {
                        loop: for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "value" -> {
                                    if (value instanceof AnnotationValue.OfString os) {
                                        charset = os.stringValue();
                                    }
                                    break loop;
                                }
                            }
                        }
                    }
                    // explicit copy-in policy
                    case "Lio/smallrye/ffm/In;" -> in = true;
                    // explicit copy-out policy
                    case "Lio/smallrye/ffm/Out;" -> out = true;
                    case "Lio/smallrye/ffm/Variadic;" -> {
                        if (variadic == -1) {
                            variadic = i;
                        } else {
                            throw new IllegalArgumentException("Multiple variadic annotations not allowed");
                        }
                    }
                    case "Lio/smallrye/ffm/Capture;" -> {
                        if (capture == -1) {
                            capture = i;
                        } else {
                            throw new IllegalArgumentException("Multiple capture annotations not allowed");
                        }
                        for (AnnotationElement element : annotation.elements()) {
                            AnnotationValue value = element.value();
                            switch (element.name().stringValue()) {
                                case "errno" -> {
                                    if (value instanceof AnnotationValue.OfBoolean ob) {
                                        compilers.errno |= ob.booleanValue();
                                    }
                                }
                                case "lastError" -> {
                                    if (value instanceof AnnotationValue.OfBoolean ob) {
                                        compilers.lastError |= ob.booleanValue();
                                    }
                                }
                                case "wsaLastError" -> {
                                    if (value instanceof AnnotationValue.OfBoolean ob) {
                                        compilers.wsaLastError |= ob.booleanValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (capture == i) {
                if (mtd.parameterType(i).descriptorString().equals("Ljava/lang/foreign/MemorySegment;")) {
                    compilers.captureStep = new UserProvidedCaptureStep(compilers, slot++);
                    continue;
                } else {
                    throw new IllegalArgumentException(
                            "Invalid type for capture parameter (must be interface java.lang.foreign.MemorySegment");
                }
            }
            if (!in && !out) {
                in = out = true;
            }
            // now examine the parameter itself
            switch (mtd.parameterType(i).descriptorString()) {
                case "[B", "[C", "[D", "[F", "[I", "[J", "[S" -> {
                    if (heap) {
                        steps.add(new SimpleArgumentStep(mtd.parameterType(i), asType, slot));
                    } else {
                        if (compilers.allocatorStep == null) {
                            compilers.allocatorStep = new OnDemandArenaStep();
                        }
                        steps.add(new PrimitiveArrayArgumentCopyStep(compilers, mtd.parameterType(i), slot, in, out));
                    }
                }
                case "Ljava/lang/foreign/MemorySegment;" -> {
                    if (heap) {
                        steps.add(new SimpleArgumentStep(mtd.parameterType(i), asType, slot));
                    } else {
                        if (compilers.allocatorStep == null) {
                            compilers.allocatorStep = new OnDemandArenaStep();
                        }
                        steps.add(new SegmentArgumentStep(compilers, slot, in, out));
                    }
                }
                case "Ljava/lang/String;" -> {
                    if (compilers.allocatorStep == null || compilers.allocatorStep instanceof OnDemandArenaStep) {
                        compilers.allocatorStep = new AlwaysArenaStep();
                    }
                    steps.add(new StringArgumentStep(compilers, slot, charset));
                }
                case "Ljava/lang/foreign/SegmentAllocator;", "Ljava/lang/foreign/Arena;" -> {
                    if (!(compilers.allocatorStep instanceof UserSuppliedSegmentAllocatorStep)) {
                        compilers.allocatorStep = new UserSuppliedSegmentAllocatorStep(slot);
                    }
                }
                case "Lio/smallrye/ffm/ErrnoConsumer;" -> {
                    if (compilers.captureStep == null) {
                        compilers.captureStep = new AllocatedCaptureStep(compilers);
                    }
                    compilers.errno = true;
                    steps.add(new ErrnoConsumerStep(compilers, slot));
                }
                case "Lio/smallrye/ffm/LastErrorConsumer;" -> {
                    if (compilers.captureStep == null) {
                        compilers.captureStep = new AllocatedCaptureStep(compilers);
                    }
                    compilers.lastError = true;
                    steps.add(new LastErrorConsumerStep(compilers, slot));
                }
                case "Lio/smallrye/ffm/WSALastErrorConsumer;" -> {
                    if (compilers.captureStep == null) {
                        compilers.captureStep = new AllocatedCaptureStep(compilers);
                    }
                    compilers.wsaLastError = true;
                    steps.add(new WSALastErrorConsumerStep(compilers, slot));
                }
                default -> steps.add(new SimpleArgumentStep(mtd.parameterType(i), asType, slot));
            }
            slot += TypeKind.from(mtd.parameterType(i)).slotSize();
        }
        switch (mtd.returnType().descriptorString()) {
            case "Ljava/lang/String;" -> steps.add(new StringResultStep(outputCharset));
        }

        if (compilers.captureStep != null) {
            if (compilers.allocatorStep == null) {
                compilers.allocatorStep = new AlwaysArenaStep();
            }
            steps.add(insertionPoint, compilers.captureStep);
        }
        if (compilers.allocatorStep != null) {
            steps.add(insertionPoint, compilers.allocatorStep);
        }

        steps.addLast(new InvokeStep());

        zb.withMethod(mm.methodName().stringValue(), mm.methodTypeSymbol(), mm.flags().flagsMask() & ~ACC_NATIVE, mb -> {
            for (MethodElement me : mm.elementList()) {
                if (me instanceof AccessFlags || me instanceof CodeElement) {
                    // skip it
                } else {
                    mb.with(me);
                }
            }
            mb.withCode(cb -> {
                mm.findAttribute(Attributes.methodParameters()).ifPresent(mpa -> {
                    List<MethodParameterInfo> parameters = mpa.parameters();
                    int paramSlot;
                    if (mm.flags().has(AccessFlag.STATIC)) {
                        paramSlot = 0;
                    } else {
                        ClassDesc thisType = mm.parent().orElseThrow().thisClass().asSymbol();
                        cb.localVariable(0, "this", thisType, cb.startLabel(), cb.endLabel());
                        paramSlot = 1;
                    }
                    for (int i = 0; i < paramCnt; i++) {
                        ClassDesc paramType = mtd.parameterType(i);
                        final MethodParameterInfo param = parameters.get(i);
                        if (param.name().isPresent()) {
                            cb.localVariable(paramSlot, param.name().get().stringValue(), paramType, cb.startLabel(),
                                    cb.endLabel());
                        }
                        paramSlot += TypeKind.from(paramType).slotSize();
                    }
                });
                steps.getFirst().call(cb, steps, 0);
            });
        });
        return true;
    }

    private static String defaultAsType(final ClassDesc classDesc) {
        return switch (classDesc.descriptorString()) {
            case "B" -> "s8";
            case "C" -> "u16";
            case "D" -> "f64";
            case "F" -> "f32";
            case "J" -> "s64";
            case "S" -> "s16";
            case "Z" -> "stdc_bool";
            case "Ljava/lang/foreign/MemorySegment;", "[B", "[C", "[D", "[F", "[I", "[J", "[S", "Ljava/lang/String;" -> "ptr";
            default -> "s32";
        };
    }

    private static final ClassDesc CD_Arena = ClassDesc.of(Arena.class.getName());
    private static final ClassDesc CD_Bootstraps = ClassDesc.of("io.smallrye.ffm.Bootstraps");
    private static final ClassDesc CD_Charset = ClassDesc.of(Charset.class.getName());
    private static final ClassDesc CD_Errno = ClassDesc.of("io.smallrye.ffm.Errno");
    private static final ClassDesc CD_ErrnoConsumer = ClassDesc.of("io.smallrye.ffm.ErrnoConsumer");
    private static final ClassDesc CD_LastErrorConsumer = ClassDesc.of("io.smallrye.ffm.LastErrorConsumer");
    private static final ClassDesc CD_Linker_Option = ClassDesc.of(Linker.Option.class.getName());
    private static final ClassDesc CD_MemoryLayout = ClassDesc.of(MemoryLayout.class.getName());
    private static final ClassDesc CD_MemorySegment = ClassDesc.of(MemorySegment.class.getName());
    private static final ClassDesc CD_SegmentAllocator = ClassDesc.of(SegmentAllocator.class.getName());
    private static final ClassDesc CD_StructLayout = ClassDesc.of(StructLayout.class.getName());
    private static final ClassDesc CD_SymbolLookup = ClassDesc.of(SymbolLookup.class.getName());
    private static final ClassDesc CD_ValueLayout = ClassDesc.of(ValueLayout.class.getName());
    private static final ClassDesc CD_ValueLayout_OfByte = ClassDesc.of(ValueLayout.OfByte.class.getName());
    private static final ClassDesc CD_ValueLayout_OfChar = ClassDesc.of(ValueLayout.OfChar.class.getName());
    private static final ClassDesc CD_ValueLayout_OfDouble = ClassDesc.of(ValueLayout.OfDouble.class.getName());
    private static final ClassDesc CD_ValueLayout_OfFloat = ClassDesc.of(ValueLayout.OfFloat.class.getName());
    private static final ClassDesc CD_ValueLayout_OfInt = ClassDesc.of(ValueLayout.OfInt.class.getName());
    private static final ClassDesc CD_ValueLayout_OfLong = ClassDesc.of(ValueLayout.OfLong.class.getName());
    private static final ClassDesc CD_ValueLayout_OfShort = ClassDesc.of(ValueLayout.OfShort.class.getName());
    private static final ClassDesc CD_ValueLayout_OfBoolean = ClassDesc.of(ValueLayout.OfBoolean.class.getName());
    private static final ClassDesc CD_WSALastErrorConsumer = ClassDesc.of("io.smallrye.ffm.WSALastErrorConsumer");

    private static final DirectMethodHandleDesc CD_Bootstraps_charset = ofConstantBootstrap(
            CD_Bootstraps,
            "charset",
            CD_Charset);
    private static final DirectMethodHandleDesc CD_Bootstraps_symbolLookup = ofConstantBootstrap(
            CD_Bootstraps,
            "symbolLookup",
            CD_SymbolLookup,
            CD_boolean);
    private static final DirectMethodHandleDesc CD_Bootstraps_linkSymbol = ofCallsiteBootstrap(
            CD_Bootstraps,
            "linkSymbol",
            CD_CallSite,
            CD_SymbolLookup);
    private static final DirectMethodHandleDesc CD_Bootstraps_callStateHandle = ofConstantBootstrap(
            CD_Bootstraps,
            "callStateHandle",
            CD_VarHandle);
    private static final DirectMethodHandleDesc CD_Bootstraps_downcall = ofCallsiteBootstrap(
            CD_Bootstraps,
            "downcall",
            CD_CallSite);
    private static final DynamicConstantDesc<Object> DCD_errno_VarHandle = DynamicConstantDesc.ofNamed(
            CD_Bootstraps_callStateHandle,
            "errno",
            CD_VarHandle);
    private static final DynamicConstantDesc<Object> DCD_LastError_VarHandle = DynamicConstantDesc.ofNamed(
            CD_Bootstraps_callStateHandle,
            "LastError",
            CD_VarHandle);
    private static final DynamicConstantDesc<Object> DCD_WSALastError_VarHandle = DynamicConstantDesc.ofNamed(
            CD_Bootstraps_callStateHandle,
            "WSALastError",
            CD_VarHandle);

    static String valueLayoutName(ClassDesc primType) {
        return switch (primType.descriptorString().charAt(0)) {
            case 'B' -> "JAVA_BYTE";
            case 'C' -> "JAVA_CHAR";
            case 'D' -> "JAVA_DOUBLE";
            case 'F' -> "JAVA_FLOAT";
            case 'I' -> "JAVA_INT";
            case 'J' -> "JAVA_LONG";
            case 'S' -> "JAVA_SHORT";
            case 'Z' -> "JAVA_BOOLEAN";
            default -> throw new IllegalStateException();
        };
    }

    static ClassDesc valueLayoutType(ClassDesc primType) {
        return switch (primType.descriptorString().charAt(0)) {
            case 'B' -> CD_ValueLayout_OfByte;
            case 'C' -> CD_ValueLayout_OfChar;
            case 'D' -> CD_ValueLayout_OfDouble;
            case 'F' -> CD_ValueLayout_OfFloat;
            case 'I' -> CD_ValueLayout_OfInt;
            case 'J' -> CD_ValueLayout_OfLong;
            case 'S' -> CD_ValueLayout_OfShort;
            case 'Z' -> CD_ValueLayout_OfBoolean;
            default -> throw new IllegalStateException();
        };
    }

    static final class Compilers {
        AllocatorStep allocatorStep;
        CaptureStep captureStep;
        boolean errno = false;
        boolean lastError = false;
        boolean wsaLastError = false;
    }

    abstract static class Step {
        Step() {
        }

        void setUpDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            int next = index + 1;
            steps.get(next).setUpDesc(steps, next, sb);
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            int next = index + 1;
            steps.get(next).addParamDesc(steps, next, sb);
        }

        void setReturnDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            int next = index + 1;
            steps.get(next).setReturnDesc(steps, next, sb);
        }

        ClassDesc getReturnType(final List<Step> steps, final int index) {
            int next = index + 1;
            return steps.get(next).getReturnType(steps, next);
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            int next = index + 1;
            steps.get(next).addDowncallArgsDescs(steps, next, descs);
        }

        void call(CodeBuilder cb, final List<Step> steps, final int index) {
            int next = index + 1;
            steps.get(next).call(cb, steps, next);
        }
    }

    static final class ResolvedSymbolStep extends Step {
        private final String name;
        private final boolean defaultLookup;

        ResolvedSymbolStep(final String name, final boolean defaultLookup) {
            this.name = name;
            this.defaultLookup = defaultLookup;
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            // the function pointer
            descs.add(CD_MemorySegment);
            // remaining arguments
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            // get the resolved symbol before anything else (throws an exception if unresolved)
            cb.invokedynamic(DynamicCallSiteDesc.of(
                    CD_Bootstraps_linkSymbol,
                    name,
                    MethodTypeDesc.of(CD_MemorySegment),
                    DynamicConstantDesc.ofNamed(
                            CD_Bootstraps_symbolLookup,
                            "_",
                            CD_SymbolLookup,
                            defaultLookup ? TRUE : FALSE)));
            super.call(cb, steps, index);
        }
    }

    static final class ReturnStep extends Step {
        private final ClassDesc returnType;
        private final String asType;

        ReturnStep(final ClassDesc returnType, final String asType) {
            this.returnType = returnType;
            this.asType = asType;
        }

        void setReturnDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            String asType = this.asType;
            sb.append(switch (asType) {
                case "void" -> "V";
                default -> asTypeToDesc(asType);
            });
        }

        ClassDesc getReturnType(final List<Step> steps, final int index) {
            return returnType;
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            super.call(cb, steps, index);
            // return the result
            cb.return_(TypeKind.from(returnType));
        }
    }

    private static String asTypeToDesc(final String asType) {
        return switch (asType) {
            case "stdc_char" -> "c";
            case "stdc_signed_char" -> "sc";
            case "stdc_unsigned_char" -> "uc";
            case "stdc_short" -> "ss";
            case "stdc_unsigned_short" -> "us";
            case "stdc_int" -> "si";
            case "stdc_unsigned_int" -> "ui";
            case "stdc_long" -> "sl";
            case "stdc_unsigned_long" -> "ul";
            case "stdc_long_long", "s64" -> "sJ";
            case "stdc_unsigned_long_long", "u64" -> "uJ";
            case "stdc_bool" -> "Z";
            case "stdc_wchar_t" -> "w";
            case "s8" -> "sB";
            case "s16" -> "sS";
            case "s32" -> "sI";
            case "u8" -> "uB";
            case "u16" -> "uS";
            case "u32" -> "uI";
            case "f32" -> "F";
            case "f64" -> "D";
            case "intptr" -> "s*";
            case "uintptr" -> "u*";
            case "ptr" -> "*";
            default -> throw new IllegalStateException();
        };
    }

    static final class CriticalStep extends Step {
        private final boolean heap;

        CriticalStep(final boolean heap) {
            this.heap = heap;
        }

        void setUpDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append(heap ? 'R' : 'r');
            super.setUpDesc(steps, index, sb);
        }
    }

    static final class InvokeStep extends Step {

        InvokeStep() {
        }

        void setUpDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append('(');
            steps.getFirst().addParamDesc(steps, 0, sb);
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append(')');
            steps.getFirst().setReturnDesc(steps, 0, sb);
        }

        void setReturnDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append('V');
        }

        ClassDesc getReturnType(final List<Step> steps, final int index) {
            return CD_void;
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            // no operation
        }

        public void call(final CodeBuilder b0, final List<Step> steps, final int index) {
            // get the downcall descriptor
            List<ClassDesc> downcallDescs = new ArrayList<>(steps.size());
            steps.getFirst().addDowncallArgsDescs(steps, 0, downcallDescs);
            StringBuilder descBuilder = new StringBuilder(64);
            steps.getFirst().setUpDesc(steps, 0, descBuilder);
            ClassDesc returnType = steps.getFirst().getReturnType(steps, 0);
            MethodTypeDesc downcallDesc = MethodTypeDesc.of(returnType, downcallDescs);
            // do not call super (we are the end)
            // write the actual call
            b0.invokedynamic(DynamicCallSiteDesc.of(
                    CD_Bootstraps_downcall,
                    descBuilder.toString(),
                    downcallDesc));
        }
    }

    static abstract class AllocatorStep extends Step {
        AllocatorStep() {
        }

        abstract void loadAllocator(CodeBuilder cb);
    }

    static abstract class ArenaStep extends AllocatorStep {
        int slot = -1;

        ArenaStep() {
        }

        /**
         * Set up the arena.
         * The arena should be pushed on to the stack.
         *
         * @param cb the code builder (must not be {@code null})
         */
        void setupArena(final CodeBuilder cb) {
            cb.invokestatic(CD_Arena, "ofConfined", MethodTypeDesc.of(CD_Arena), true);
        }

        /**
         * Release the arena.
         *
         * @param cb the code builder (must not be {@code null})
         */
        void releaseArena(final CodeBuilder cb) {
            // free our arena
            cb.aload(slot);
            cb.invokeinterface(CD_Arena, "close", MethodTypeDesc.of(CD_void));
        }

        void loadAllocator(final CodeBuilder cb) {
            cb.aload(slot);
        }

        public void call(final CodeBuilder b0, final List<Step> steps, final int index) {
            slot = b0.allocateLocal(TypeKind.REFERENCE);
            b0.localVariable(slot, "arena", CD_Arena, b0.newBoundLabel(), b0.endLabel());
            setupArena(b0);
            b0.astore(slot);
            b0.trying(b1 -> super.call(b1, steps, index), c1 -> c1.catchingAll(b2 -> {
                int ex = b2.allocateLocal(TypeKind.REFERENCE);
                b2.localVariable(ex, "ex" + slot, CD_MemorySegment, b2.newBoundLabel(), b2.endLabel());
                b2.astore(ex);
                b2.trying(this::releaseArena, c3 -> c3.catchingAll(b4 -> {
                    b4.aload(ex);
                    b4.swap();
                    b4.invokevirtual(CD_Throwable, "addSuppressed", MethodTypeDesc.of(CD_void, CD_Throwable));
                }));
                b2.aload(ex);
                b2.athrow();
            }));
            releaseArena(b0);
        }
    }

    static final class OnDemandArenaStep extends ArenaStep {
        OnDemandArenaStep() {
        }

        void setupArena(final CodeBuilder b0) {
            b0.aconst_null();
        }

        void releaseArena(final CodeBuilder b0) {
            b0.aload(slot);
            b0.ifThen(Opcode.IFNONNULL, super::releaseArena);
        }

        void loadAllocator(final CodeBuilder b0) {
            b0.aload(slot);
            b0.dup();
            b0.ifThen(Opcode.IFNULL, b1 -> {
                b1.pop();
                super.setupArena(b1);
                b1.astore(slot);
                b1.aload(slot);
            });
        }
    }

    static final class AlwaysArenaStep extends ArenaStep {
        AlwaysArenaStep() {
        }
    }

    static final class UserSuppliedSegmentAllocatorStep extends AllocatorStep {
        private final int slot;

        UserSuppliedSegmentAllocatorStep(final int slot) {
            this.slot = slot;
        }

        void loadAllocator(final CodeBuilder cb) {
            cb.aload(slot);
        }
    }

    static abstract class CaptureStep extends Step {
        final Compilers compilers;

        CaptureStep(final Compilers compilers) {
            this.compilers = compilers;
        }

        void setUpDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            if (compilers.errno) {
                sb.append('e');
            }
            if (compilers.lastError) {
                sb.append('E');
            }
            if (compilers.wsaLastError) {
                sb.append('W');
            }
            super.setUpDesc(steps, index, sb);
        }

        abstract int captureSlot();
    }

    static final class UserProvidedCaptureStep extends CaptureStep {
        private final int slot;

        UserProvidedCaptureStep(final Compilers compilers, final int slot) {
            super(compilers);
            this.slot = slot;
        }

        int captureSlot() {
            return slot;
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(CD_MemorySegment);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            cb.aload(slot);
            super.call(cb, steps, index);
        }
    }

    static final class AllocatedCaptureStep extends CaptureStep {
        private int slot = -1;

        AllocatedCaptureStep(final Compilers compilers) {
            super(compilers);
        }

        int captureSlot() {
            return slot;
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(CD_MemorySegment);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            slot = cb.allocateLocal(TypeKind.REFERENCE);
            cb.localVariable(slot, "capture", CD_MemorySegment, cb.newBoundLabel(), cb.endLabel());
            compilers.allocatorStep.loadAllocator(cb);
            cb.invokestatic(CD_Linker_Option, "captureStateLayout", MethodTypeDesc.of(CD_StructLayout), true);
            cb.invokeinterface(CD_SegmentAllocator, "allocate", MethodTypeDesc.of(CD_MemorySegment, CD_MemoryLayout));
            cb.dup();
            cb.astore(slot);
            super.call(cb, steps, index);
        }
    }

    static final class ErrnoConsumerStep extends Step {
        private final Compilers compilers;
        private final int slot;

        ErrnoConsumerStep(final Compilers compilers, final int slot) {
            this.compilers = compilers;
            this.slot = slot;
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            super.call(cb, steps, index);
            cb.aload(slot);
            cb.loadConstant(DCD_errno_VarHandle);
            cb.aload(compilers.captureStep.captureSlot());
            cb.lconst_0();
            cb.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(CD_int, CD_MemorySegment, CD_long));
            cb.invokestatic(CD_Errno, "ofNativeValue", MethodTypeDesc.of(CD_Errno, CD_int));
            cb.invokeinterface(CD_ErrnoConsumer, "accept", MethodTypeDesc.of(CD_void, CD_Errno));
        }
    }

    static final class LastErrorConsumerStep extends Step {
        private final Compilers compilers;
        private final int slot;

        LastErrorConsumerStep(final Compilers compilers, final int slot) {
            this.compilers = compilers;
            this.slot = slot;
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            super.call(cb, steps, index);
            cb.aload(slot);
            cb.loadConstant(DCD_LastError_VarHandle);
            cb.aload(compilers.captureStep.captureSlot());
            cb.lconst_0();
            cb.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(CD_int, CD_MemorySegment, CD_long));
            cb.invokeinterface(CD_LastErrorConsumer, "accept", MethodTypeDesc.of(CD_void, CD_int));
        }
    }

    static final class WSALastErrorConsumerStep extends Step {
        private final Compilers compilers;
        private final int slot;

        WSALastErrorConsumerStep(final Compilers compilers, final int slot) {
            this.compilers = compilers;
            this.slot = slot;
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            super.call(cb, steps, index);
            cb.aload(slot);
            cb.loadConstant(DCD_WSALastError_VarHandle);
            cb.aload(compilers.captureStep.captureSlot());
            cb.lconst_0();
            cb.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(CD_int, CD_MemorySegment, CD_long));
            cb.invokeinterface(CD_WSALastErrorConsumer, "accept", MethodTypeDesc.of(CD_void, CD_int));
        }
    }

    static final class SimpleArgumentStep extends Step {
        private final ClassDesc paramDesc;
        private final String asType;
        private final int slot;

        SimpleArgumentStep(final ClassDesc paramDesc, final String asType, final int slot) {
            this.paramDesc = paramDesc;
            this.asType = asType;
            this.slot = slot;
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append(asTypeToDesc(asType));
            super.addParamDesc(steps, index, sb);
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(paramDesc);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder b0, final List<Step> steps, final int index) {
            b0.loadLocal(TypeKind.from(paramDesc), slot);
            super.call(b0, steps, index);
        }
    }

    static final class SegmentArgumentStep extends Step {
        private final Compilers compilers;
        private final int slot;
        private final boolean in, out;

        SegmentArgumentStep(final Compilers compilers, final int slot, final boolean in, final boolean out) {
            this.compilers = compilers;
            this.slot = slot;
            this.in = in;
            this.out = out;
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append('*');
            super.addParamDesc(steps, index, sb);
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(CD_MemorySegment);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            cb.block(b0 -> {
                int isNative = -1;
                int temp = b0.allocateLocal(TypeKind.REFERENCE);
                b0.aconst_null();
                b0.localVariable(temp, "temp" + slot, CD_MemorySegment, b0.newBoundLabel(), b0.endLabel());
                b0.astore(temp);
                b0.lconst_0();
                int size = b0.allocateLocal(TypeKind.LONG);
                b0.localVariable(size, "size" + slot, CD_long, b0.newBoundLabel(), b0.endLabel());
                b0.lstore(size);
                b0.aload(slot);
                b0.invokeinterface(CD_MemorySegment, "isNative", MethodTypeDesc.of(CD_boolean));
                if (out) {
                    // save for later
                    isNative = b0.allocateLocal(TypeKind.BOOLEAN);
                    b0.dup();
                    b0.localVariable(isNative, "isNative" + slot, CD_boolean, b0.newBoundLabel(), b0.endLabel());
                    b0.istore(isNative);
                }
                b0.ifThenElse(b1 -> {
                    // buffer is native or null; just load it
                    b1.aload(slot);
                }, b1 -> {
                    // buffer is heap; allocate temp buf and copy to it
                    compilers.allocatorStep.loadAllocator(b1);
                    b1.aload(slot);
                    b1.invokeinterface(CD_MemorySegment, "byteSize", MethodTypeDesc.of(CD_long));
                    b1.dup2();
                    b1.lstore(size);
                    b1.invokeinterface(CD_SegmentAllocator, "allocate", MethodTypeDesc.of(CD_MemorySegment, CD_long));
                    b1.astore(temp);
                    if (in) {
                        // copy the data into the temporary segment
                        b1.aload(slot);
                        b1.lconst_0();
                        b1.aload(temp);
                        b1.lconst_0();
                        b1.lload(size);
                        b1.invokestatic(CD_MemorySegment, "copy", MethodTypeDesc.of(
                                CD_void,
                                CD_MemorySegment,
                                CD_long,
                                CD_MemorySegment,
                                CD_long,
                                CD_long), true);
                    }
                    b1.aload(temp);
                });
                super.call(cb, steps, index);
                if (out) {
                    b0.iload(isNative);
                    b0.ifThen(Opcode.IFEQ, b1 -> {
                        // copy the data out of the temporary segment
                        b1.aload(temp);
                        b1.lconst_0();
                        b1.aload(slot);
                        b1.lconst_0();
                        b1.lload(size);
                        b1.invokestatic(CD_MemorySegment, "copy", MethodTypeDesc.of(
                                CD_void,
                                CD_MemorySegment,
                                CD_long,
                                CD_MemorySegment,
                                CD_long,
                                CD_long), true);
                    });
                }
            });
        }
    }

    static final class StringArgumentStep extends Step {

        private final Compilers compilers;
        private final int slot;
        private final String charset;

        StringArgumentStep(final Compilers compilers, final int slot, final String charset) {
            this.compilers = compilers;
            this.slot = slot;
            this.charset = charset;
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append('*');
            super.addParamDesc(steps, index, sb);
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(CD_MemorySegment);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            this.compilers.allocatorStep.loadAllocator(cb);
            cb.aload(slot);
            //    default MemorySegment allocateFrom(String str) {
            //    default MemorySegment allocateFrom(String str, Charset charset) {
            if (charset == null || charset.equalsIgnoreCase("utf-8") || charset.equalsIgnoreCase("utf_8")) {
                cb.invokeinterface(CD_SegmentAllocator, "allocateFrom", MethodTypeDesc.of(CD_MemorySegment, CD_String));
            } else {
                cb.loadConstant(
                        DynamicConstantDesc.ofNamed(
                                CD_Bootstraps_charset,
                                charset,
                                CD_Charset));
                cb.invokeinterface(CD_SegmentAllocator, "allocateFrom",
                        MethodTypeDesc.of(CD_MemorySegment, CD_String, CD_Charset));
            }
            int strBuf = cb.allocateLocal(TypeKind.REFERENCE);
            cb.astore(strBuf);
            cb.localVariable(strBuf, "strBuf" + slot, CD_MemorySegment, cb.newBoundLabel(), cb.endLabel());
            cb.aload(strBuf);
            super.call(cb, steps, index);
        }
    }

    static class PrimitiveArrayArgumentCopyStep extends Step {
        private final Compilers compilers;
        private final ClassDesc arrayType;
        private final int arraySlot;
        private final boolean in;
        private final boolean out;
        private int segmentSlot;

        private PrimitiveArrayArgumentCopyStep(final Compilers compilers, final ClassDesc arrayType, final int arraySlot,
                final boolean in, final boolean out) {
            this.compilers = compilers;
            this.arrayType = arrayType;
            this.arraySlot = arraySlot;
            this.in = in;
            this.out = out;
        }

        void addParamDesc(final List<Step> steps, final int index, final StringBuilder sb) {
            sb.append('*');
            super.addParamDesc(steps, index, sb);
        }

        void addDowncallArgsDescs(final List<Step> steps, final int index, final List<ClassDesc> descs) {
            descs.add(CD_MemorySegment);
            super.addDowncallArgsDescs(steps, index, descs);
        }

        void call(final CodeBuilder b0, final List<Step> steps, final int index) {
            b0.block(b1 -> {
                compilers.allocatorStep.loadAllocator(b1);
                String layoutName = valueLayoutName(arrayType.componentType());
                ClassDesc layoutType = valueLayoutType(arrayType.componentType());
                b1.getstatic(CD_ValueLayout, layoutName, layoutType);
                if (in) {
                    // copy into temp buffer
                    b1.aload(arraySlot);
                    //    default MemorySegment allocateFrom(ValueLayout.Of<Type> elementLayout, <type>... elements) {
                    b1.invokeinterface(CD_SegmentAllocator, "allocateFrom",
                            MethodTypeDesc.of(CD_MemorySegment, layoutType, arrayType));
                } else {
                    // out-only
                    b1.aload(arraySlot);
                    b1.arraylength();
                    b1.i2l();
                    //    default MemorySegment allocate(MemoryLayout elementLayout, long count) {
                    b1.invokeinterface(CD_SegmentAllocator, "allocate",
                            MethodTypeDesc.of(CD_MemorySegment, CD_MemoryLayout, CD_long));
                }
                segmentSlot = b1.allocateLocal(TypeKind.REFERENCE);
                b1.localVariable(segmentSlot, "segment" + arraySlot, CD_MemorySegment, b1.newBoundLabel(), b1.endLabel());
                b1.dup();
                b1.astore(segmentSlot);
                // make the call
                super.call(b1, steps, index);
                // copy back to the array if needed
                if (out) {
                    // copy from temp buffer
                    //    static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
                    //                     Object dstArray, int dstIndex,
                    //                     int elementCount) {
                    b1.aload(segmentSlot); // srcSegment
                    b1.getstatic(CD_ValueLayout, layoutName, layoutType); // srcLayout
                    b1.lconst_0(); // srcOffset
                    b1.aload(arraySlot); // dstArray
                    b1.iconst_0(); // dstIndex
                    b1.aload(arraySlot);
                    b1.arraylength(); // elementCount
                    b1.invokestatic(CD_MemorySegment, "copy",
                            MethodTypeDesc.of(CD_void, CD_MemorySegment, CD_ValueLayout, CD_long, CD_Object, CD_int, CD_int),
                            true);
                }
            });
        }
    }

    static class StringResultStep extends Step {
        private final String charset;

        StringResultStep(final String charset) {
            this.charset = charset;
        }

        void call(final CodeBuilder cb, final List<Step> steps, final int index) {
            super.call(cb, steps, index);
            cb.lconst_0();
            if (charset == null) {
                cb.invokeinterface(CD_MemorySegment, "getString", MethodTypeDesc.of(CD_String, CD_long));
            } else {
                cb.loadConstant(
                        DynamicConstantDesc.ofNamed(
                                CD_Bootstraps_charset,
                                charset,
                                CD_Charset));
                cb.invokeinterface(CD_MemorySegment, "getString", MethodTypeDesc.of(CD_String, CD_long, CD_Charset));
            }
        }
    }
}
