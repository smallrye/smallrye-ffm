package io.smallrye.ffm;

import static java.lang.invoke.MethodHandles.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.cpu.CPU;
import io.smallrye.common.os.OS;

/**
 * Bootstraps and utilities for automatically linking {@code native} methods.
 */
public final class Bootstraps {
    private Bootstraps() {
    }

    private static final Linker.Option[] NO_OPTIONS = new Linker.Option[0];

    public static VarHandle callStateHandle(MethodHandles.Lookup lookup, String name, Class<VarHandle> type) {
        if (type != VarHandle.class) {
            throw wrongType();
        }
        return Linker.Option.captureStateLayout().varHandle(MemoryLayout.PathElement.groupElement(name))
                .withInvokeExactBehavior();
    }

    public static Charset charset(MethodHandles.Lookup lookup, String name, Class<Charset> type) {
        if (type != Charset.class) {
            throw wrongType();
        }
        // charset lookup is not dependent on caller, so we do not need to use lookup here
        return Charset.forName(name);
    }

    public static PaddingLayout paddingLayout(MethodHandles.Lookup lookup, String name, Class<PaddingLayout> type, long size) {
        if (type != PaddingLayout.class) {
            throw wrongType();
        }
        PaddingLayout base = MemoryLayout.paddingLayout(size);
        if (name != null && !name.isEmpty() && !name.equals("_")) {
            base = base.withName(name);
        }
        return base;
    }

    public static SequenceLayout sequenceLayout(MethodHandles.Lookup lookup, String name, Class<SequenceLayout> type,
            long count, MemoryLayout element) {
        if (type != SequenceLayout.class) {
            throw wrongType();
        }
        SequenceLayout base = MemoryLayout.sequenceLayout(count, element);
        if (name != null && !name.isEmpty() && !name.equals("_")) {
            base = base.withName(name);
        }
        return base;
    }

    public static StructLayout structLayout(MethodHandles.Lookup lookup, String name, Class<StructLayout> type,
            MemoryLayout... elements) {
        if (type != StructLayout.class) {
            throw wrongType();
        }
        StructLayout base = MemoryLayout.structLayout(elements);
        if (name != null && !name.isEmpty() && !name.equals("_")) {
            base = base.withName(name);
        }
        return base;
    }

    public static UnionLayout unionLayout(MethodHandles.Lookup lookup, String name, Class<UnionLayout> type,
            MemoryLayout... elements) {
        if (type != UnionLayout.class) {
            throw wrongType();
        }
        UnionLayout base = MemoryLayout.unionLayout(elements);
        if (name != null && !name.isEmpty() && !name.equals("_")) {
            base = base.withName(name);
        }
        return base;
    }

    /**
     * {@return a fixed allocator which only returns the given segment}
     * The segment is only returned one time;
     * subsequent allocation requests are rejected.
     *
     * @param segment the memory segment to return once (must not be {@code null})
     */
    public static SegmentAllocator fixedAllocator(MemorySegment segment) {
        Assert.checkNotNullParam("segment", segment);
        return new SegmentAllocator() {
            boolean used = false;

            public MemorySegment allocate(final long size, final long align) {
                if (size == 0) {
                    return MemorySegment.NULL;
                }
                if (!used) {
                    used = true;
                    if (size <= segment.byteSize() && align <= segment.maxByteAlignment()) {
                        return segment;
                    }
                }
                throw new IllegalArgumentException("Invalid allocation");
            }
        };
    }

    /**
     * Dynamic constant bootstrap to acquire a value layout.
     * The {@code kind} must be either a {@code ValueLayout} constant name
     * or a {@linkplain Linker#canonicalLayouts() canonical layout name}.
     *
     * @param lookup the caller lookup, provided by the JVM (must not be {@code null})
     * @param name the name for the returned value layout, or {@code _} for no name
     * @param type {@code ValueLayout.class}, provided by the JVM (must not be {@code null})
     * @param kind the kind (one of {@code JAVA_BYTE}, {@code JAVA_SHORT}, etc.,
     *        or {@code wchar_t}, {@code long}, etc.; must not be {@code null})
     * @return the value layout (not {@code null})
     */
    public static ValueLayout valueLayout(MethodHandles.Lookup lookup, String name, Class<ValueLayout> type, String kind) {
        if (type != ValueLayout.class) {
            throw wrongType();
        }
        ValueLayout base = switch (kind) {
            case "JAVA_BYTE" -> ValueLayout.JAVA_BYTE;
            case "JAVA_SHORT" -> ValueLayout.JAVA_SHORT;
            case "JAVA_CHAR" -> ValueLayout.JAVA_CHAR;
            case "JAVA_INT" -> ValueLayout.JAVA_INT;
            case "JAVA_LONG" -> ValueLayout.JAVA_LONG;
            case "JAVA_FLOAT" -> ValueLayout.JAVA_FLOAT;
            case "JAVA_DOUBLE" -> ValueLayout.JAVA_DOUBLE;
            case "JAVA_BOOLEAN" -> ValueLayout.JAVA_BOOLEAN;
            case "ADDRESS" -> ValueLayout.ADDRESS;
            // unaligned
            case "JAVA_SHORT_UNALIGNED" -> ValueLayout.JAVA_SHORT_UNALIGNED;
            case "JAVA_CHAR_UNALIGNED" -> ValueLayout.JAVA_CHAR_UNALIGNED;
            case "JAVA_INT_UNALIGNED" -> ValueLayout.JAVA_INT_UNALIGNED;
            case "JAVA_LONG_UNALIGNED" -> ValueLayout.JAVA_LONG_UNALIGNED;
            case "JAVA_FLOAT_UNALIGNED" -> ValueLayout.JAVA_FLOAT_UNALIGNED;
            case "JAVA_DOUBLE_UNALIGNED" -> ValueLayout.JAVA_DOUBLE_UNALIGNED;
            case "ADDRESS_UNALIGNED" -> ValueLayout.ADDRESS_UNALIGNED;
            // canonical layouts (maybe)
            default -> (ValueLayout) Linker.nativeLinker().canonicalLayouts().get(kind);
        };
        if (base == null) {
            throw new NoSuchElementException(name);
        }
        if (name != null && !name.isEmpty() && !name.equals("_")) {
            base = base.withName(name);
        }
        return base;
    }

    /**
     * Dynamic constant bootstrap which produces the {@link SymbolLookup} for the calling class.
     * This allows the same instance to be used for all lookups in the class.
     *
     * @param lookup the caller lookup, provided by the JVM (must not be {@code null})
     * @param name ignored
     * @param type {@code SymbolLookup.class}, provided by the JVM (must not be {@code null})
     * @param systemLinker {@code true} to use the system linker for lookups (e.g. {@code libc}),
     *        or {@code false} to use the lookup associated with the caller's class loader
     *        (e.g. in conjunction with {@link System#loadLibrary(String)})
     * @return the symbol lookup (not {@code null})
     */
    public static SymbolLookup symbolLookup(MethodHandles.Lookup lookup, String name, Class<SymbolLookup> type,
            boolean systemLinker) {
        if (type != SymbolLookup.class) {
            throw wrongType();
        }
        if (systemLinker) {
            return Linker.nativeLinker().defaultLookup();
        }
        // use a method handle here so that we can call {@code loaderLookup} on behalf of the caller
        MethodHandle loaderLookup;
        try {
            loaderLookup = lookup.findStatic(SymbolLookup.class, "loaderLookup", MethodType.methodType(SymbolLookup.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw toError(e);
        }
        try {
            return (SymbolLookup) loaderLookup.invokeExact();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * An invoke-dynamic bootstrap which links a symbol and returns its address as a constant.
     * If the symbol is not found, every invocation will throw an {@link UnsatisfiedLinkError}.
     *
     * @param lookup the caller lookup, provided by the JVM (must not be {@code null})
     * @param name the name of the symbol to link (must not be {@code null})
     * @param type {@code ()Ljava/lang/foreign/MemorySegment;}, provided by the JVM (must not be {@code null})
     * @param symbolLookup the symbol lookup returned by {@link #symbolLookup} (must not be {@code null})
     * @return a call site which either returns the linked symbol or throws an error (not {@code null})
     */
    public static CallSite linkSymbol(MethodHandles.Lookup lookup, String name, MethodType type, SymbolLookup symbolLookup) {
        if (type.parameterCount() != 0 || type.returnType() != MemorySegment.class) {
            throw wrongType();
        }
        // not caller-sensitive
        Optional<MemorySegment> result = symbolLookup.find(name);
        MethodHandle mh;
        if (result.isPresent()) {
            mh = MethodHandles.constant(MemorySegment.class, result.get());
        } else {
            mh = doThrow.bindTo(name).asType(type);
        }
        return new ConstantCallSite(mh);
    }

    /**
     * Invoke-dynamic bootstrap which invokes a downcall method handle.
     * The returned method handle accepts the symbol address as a {@link MemorySegment},
     * an optional {@link SegmentAllocator} if required by the layout,
     * an optional {@link MemorySegment} for captured call state if the corresponding flag(s) are given,
     * and one argument for each supplied argument type.
     * The return type of the returned method corresponds to the given return type specification.
     * The Java types given should either be equal in size or be wider than the value layout size (on every platform)
     * in order to guarantee that no so-called "garbage bits" are sent in to the native method.
     * <p>
     * The descriptor string is structured as follows:
     * <code><pre>
    descriptor := flag* '(' type* ')' retType

    flag := 'e' | 'E' | 'W' | 'r' | 'R'

    type := 'u' intType | 's' intType | 'e' | 'c' | 'w' | '*' | 'F' | 'D' | 'Z'

    retType := type | 'V'

    intType := 'B' | 'S' | 'I' | 'J' | 'c' | 's' | 'i' | 'l'
     * </pre></code>
     * The following flags are recognized:
     * <ul>
     * <li>{@code e} - indicate that {@code errno} shall be captured</li>
     * <li>{@code E} - indicate that {@code LastError} shall be captured</li>
     * <li>{@code W} - indicate that {@code WSALastError} shall be captured</li>
     * <li>{@code r} - indicate that the call is {@linkplain Linker.Option#critical(boolean) critical}
     * <li>{@code R} - indicate that the call is {@linkplain Linker.Option#critical(boolean) critical} and may access the Java
     * heap directly
     * </ul>
     * The following descriptor type sequences are recognized:
     * <ul>
     * <li>{@code uB} - unsigned byte (one byte)</li>
     * <li>{@code uS} - unsigned short/char (two bytes)</li>
     * <li>{@code uI} - unsigned int (four bytes)</li>
     * <li>{@code uJ} - unsigned long (eight bytes)</li>
     * <li>{@code u*} - unsigned pointer-sized integer (four or eight bytes)</li>
     * // TODO complete
     * </ul>
     *
     * @param lookup the caller lookup, provided by the JVM (must not be {@code null})
     * @param descStr the native descriptor string (must not be {@code null})
     * @param type the method type corresponding to the downcall handle, provided by the JVM (must not be {@code null})
     * @return a call site which invokes the downcall (not {@code null})
     */
    public static CallSite downcall(MethodHandles.Lookup lookup, String descStr, MethodType type) {
        MethodHandle handle;
        // the descriptors for downcall handles are indy-hostile, so map them more nicely
        boolean critical = false;
        boolean criticalHeap = false;
        boolean captureErrno = false;
        boolean captureLastError = false;
        boolean captureWSALastError = false;
        int variadic = -1;
        int argStart = -1;
        // always accept the function pointer as the first argument
        int argCount = 1;
        // initial scan & validation
        int nameLen = descStr.length();
        out: for (int h = 0; h < nameLen; h++) {
            switch (descStr.charAt(h)) {
                case '(' -> {
                    argStart = h + 1;
                    for (h++; h < nameLen; h++) {
                        switch (descStr.charAt(h)) {
                            case 'u', 's' -> {
                                h++;
                                argCount++;
                            }
                            case 'c', '*', 'Z', 'F', 'D' -> argCount++;
                            case '.' -> variadic = argCount;
                            case ')' -> {
                                for (h++; h < nameLen; h++) {
                                    switch (descStr.charAt(h)) {
                                        case 'u', 's' -> h++;
                                        case 'c', '*', 'Z', 'V' -> {
                                        }
                                    }
                                }
                                // OK
                                break out;
                            }
                            default -> throw invalidDesc(descStr);
                        }
                    }
                    // unexpected end of dec
                    throw invalidDesc(descStr);
                }
                case 'e' -> captureErrno = true;
                case 'E' -> captureLastError = true;
                case 'W' -> captureWSALastError = true;
                case 'r' -> critical = true;
                case 'R' -> criticalHeap = true;
                default -> throw invalidDesc(descStr);
            }
        }
        if (argStart == -1) {
            throw invalidDesc(descStr);
        }

        int size = 0;
        if (critical || criticalHeap) {
            size++;
        }
        int capture = 0;
        if (captureErrno)
            capture++;
        if (captureLastError)
            capture++;
        if (captureWSALastError)
            capture++;
        if (capture != 0) {
            size++;
            argCount++;
        }
        if (variadic >= 0) {
            size++;
        }
        Linker.Option[] options = size == 0 ? NO_OPTIONS : new Linker.Option[size];
        int i = 0;
        if (criticalHeap) {
            options[i++] = Linker.Option.critical(true);
        } else if (critical) {
            options[i++] = Linker.Option.critical(false);
        }
        if (capture != 0) {
            String[] array = new String[capture];
            int j = 0;
            if (captureErrno) {
                array[j++] = "errno";
            }
            if (captureLastError) {
                array[j++] = "GetLastError";
            }
            if (captureWSALastError) {
                array[j] = "WSAGetLastError";
            }
            options[i++] = Linker.Option.captureCallState(array);
        }
        if (variadic >= 0) {
            options[i] = Linker.Option.firstVariadicArg(variadic);
        }
        boolean isVar = false;
        // skip the param for the symbol
        int param = 1;
        List<MemoryLayout> layouts = new ArrayList<>(argCount);
        List<MethodHandle> convs = new ArrayList<>(argCount);
        MethodHandle returnConv = null;
        // first, the symbol
        convs.add(null);
        // next, capture (if any)
        if (capture > 0) {
            convs.add(null);
            if (type.parameterType(param++) != MemorySegment.class) {
                throw new IllegalArgumentException("Capture argument type must be MemorySegment");
            }
        }
        // next, segment allocator (if any)
        // todo: (for structure support)
        // finally, actual parameters
        boolean hasParamConv = false;
        for (int k = argStart; k < nameLen;) {
            switch (descStr.charAt(k)) {
                case '.' -> {
                    // marker, not an argument
                    isVar = true;
                    k++;
                }
                case ')' -> {
                    // return type
                    FunctionDescriptor desc;
                    k++;
                    switch (descStr.charAt(k)) {
                        case 'V' -> {
                            if (type.returnType() != void.class) {
                                throw new IllegalArgumentException("Return type mismatch");
                            }
                            desc = FunctionDescriptor.ofVoid(layouts.toArray(MemoryLayout[]::new));
                            k++;
                        }
                        default -> {
                            if (type.returnType() != void.class) {
                                // convert the type
                                returnConv = outputConvOf(descStr, k, type.returnType());
                            }
                            desc = FunctionDescriptor.of(layoutOf(descStr, k, false), layouts.toArray(MemoryLayout[]::new));
                            k += charCount(descStr, k);
                        }
                    }
                    if (k < nameLen) {
                        // extra chars
                        throw invalidDesc(descStr);
                    }
                    // call downcallHandle(...) on behalf of the caller, to satisfy security checks
                    MethodHandle linkDowncallHandle;
                    try {
                        linkDowncallHandle = lookup.findVirtual(Linker.class, "downcallHandle",
                                MethodType.methodType(MethodHandle.class, FunctionDescriptor.class, Linker.Option[].class));
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw toError(e);
                    }
                    try {
                        handle = (MethodHandle) linkDowncallHandle.invokeExact(Linker.nativeLinker(), desc, options);
                    } catch (RuntimeException | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new UndeclaredThrowableException(e);
                    }
                    MethodType downcallType = handle.type();
                    int cnt = downcallType.parameterCount();
                    if (cnt != type.parameterCount()) {
                        throw wrongType(type, downcallType);
                    }
                    // perform explicit cast to requested type (if needed)
                    if (hasParamConv) {
                        handle = MethodHandles.filterArguments(handle, 0, convs.toArray(MethodHandle[]::new));
                    }
                    if (returnConv != null) {
                        handle = MethodHandles.filterReturnValue(handle, returnConv);
                    }
                    // special case: add errno-resetting hook if needed
                    // todo: remove for JDKs which solve https://bugs.openjdk.org/browse/JDK-8378559
                    if (captureErrno) {
                        handle = MethodHandles.collectArguments(handle, 1, staticMethod(Bootstraps.class, "fillErrno",
                                "(Ljava/lang/foreign/MemorySegment;)Ljava/lang/foreign/MemorySegment;", false));
                    }
                    if (handle.type().equals(type)) {
                        return new ConstantCallSite(handle);
                    } else {
                        throw new IllegalArgumentException(
                                "Return type mismatch\n\t\tExpected: " + type + "\n\t\tWas: " + handle.type());
                    }
                }
                default -> {
                    MemoryLayout layout = layoutOf(descStr, k, isVar);
                    MethodHandle conv = inputConvOf(descStr, k, type.parameterType(param), isVar);
                    k += charCount(descStr, k);
                    if (conv != null) {
                        hasParamConv = true;
                    }
                    layouts.add(layout);
                    convs.add(conv);
                    param++;
                }
            }
        }
        throw invalidDesc(descStr);
    }

    private static IllegalArgumentException invalidDesc(final String name) {
        return new IllegalArgumentException("Invalid native descriptor: " + name);
    }

    private static int charCount(String desc, int offs) {
        return switch (desc.charAt(offs)) {
            case 'u', 's' -> 2;
            default -> 1;
        };
    }

    private static final ValueLayout stdc_long_layout;
    private static final ValueLayout stdc_char_layout;
    private static final ValueLayout stdc_wchar_layout;
    private static final ValueLayout intptr_layout;
    private static final ValueLayout uintptr_layout;
    private static final ValueLayout u32_layout;
    private static final boolean stdc_long_is_64bit;
    private static final boolean cpu_is_64bit = CPU.host().pointerSizeBits() == 64;
    static final boolean stdc_char_is_signed;
    static final boolean stdc_wchar_is_signed;
    private static final boolean stdc_wchar_is_32bit;

    static {
        Map<String, MemoryLayout> layouts = Linker.nativeLinker().canonicalLayouts();
        stdc_long_layout = (ValueLayout) layouts.get("long");
        stdc_long_is_64bit = stdc_long_layout.byteSize() == 8;
        stdc_char_is_signed = switch (CPU.host()) {
            case riscv -> false;
            case wasm32, x86, x64 -> true;
            case arm, aarch64, ppc, ppc32 -> switch (OS.current()) {
                case MAC -> true;
                default -> false;
            };
            default -> switch (OS.current()) {
                case Z -> false;
                default -> true;
            };
        };
        stdc_wchar_layout = (ValueLayout) layouts.get("wchar_t");
        stdc_char_layout = stdc_char_is_signed ? ValueLayout.JAVA_BYTE : ValueLayout.JAVA_INT;
        stdc_wchar_is_32bit = stdc_wchar_layout.byteSize() == 4;
        stdc_wchar_is_signed = switch (OS.current()) {
            case WINDOWS, AIX, Z -> false;
            default -> switch (CPU.host()) {
                case arm -> false;
                default -> true;
            };
        };
        intptr_layout = cpu_is_64bit ? ValueLayout.JAVA_LONG : ValueLayout.JAVA_INT;
        uintptr_layout = ValueLayout.JAVA_LONG;
        u32_layout = cpu_is_64bit ? ValueLayout.JAVA_LONG : ValueLayout.JAVA_INT;
    }

    private static MemoryLayout layoutOf(String desc, int offs, boolean variadic) {
        return switch (desc.charAt(offs)) {
            case 'u' -> switch (desc.charAt(offs + 1)) {
                case 'B', 'c', 's', 'S' -> ValueLayout.JAVA_INT;
                case 'I', 'i' -> u32_layout;
                case 'J', 'l' -> ValueLayout.JAVA_LONG;
                case '*' -> uintptr_layout;
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'e' -> ValueLayout.JAVA_INT;
            case 's' -> switch (desc.charAt(offs + 1)) {
                case 'B', 'c' -> ValueLayout.JAVA_BYTE;
                case 'S', 's' -> ValueLayout.JAVA_SHORT;
                case 'I', 'i' -> ValueLayout.JAVA_INT;
                case 'l' -> stdc_long_layout;
                case 'J' -> ValueLayout.JAVA_LONG;
                case '*' -> intptr_layout;
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'c' -> stdc_char_layout;
            case 'w' -> stdc_wchar_layout;
            case '*' -> ValueLayout.ADDRESS;
            case 'Z' -> ValueLayout.JAVA_BOOLEAN;
            case 'F' -> variadic ? ValueLayout.JAVA_DOUBLE : ValueLayout.JAVA_FLOAT;
            case 'D' -> ValueLayout.JAVA_DOUBLE;
            default -> throw new IllegalArgumentException("Invalid native descriptor");
        };
    }

    // Replaced at build time with an actual method handle constant (so don't delete isInterface)
    private static MethodHandle staticMethod(Class<?> owner, String name, String descriptor,
            @SuppressWarnings("unused") boolean isInterface) {
        try {
            return lookup().findStatic(owner, name,
                    MethodType.fromMethodDescriptorString(descriptor, Bootstraps.class.getClassLoader()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw toError(e);
        }
    }

    private static MethodHandle inputConvOf(String desc, int offs, Class<?> type, boolean variadic) {
        final char dsChar = type.descriptorString().charAt(0);
        return switch (desc.charAt(offs)) {
            case 'u' -> switch (desc.charAt(offs + 1)) {
                // downcall parameter is unsigned byte
                case 'B', 'c' -> inputConvOfU8(desc, offs, type, dsChar);
                // downcall parameter is unsigned short
                case 's', 'S' -> inputConvOfU16(desc, offs, type, dsChar);
                // downcall parameter is unsigned int
                case 'I', 'i' -> inputConvOfU32(desc, offs, type, dsChar);
                // downcall parameter is unsigned C long
                case 'l' ->
                    stdc_long_is_64bit ? inputConvOfU64(desc, offs, type, dsChar) : inputConvOfU32(desc, offs, type, dsChar);
                // downcall parameter is unsigned long
                case 'J' -> inputConvOfU64(desc, offs, type, dsChar);
                // downcall parameter is unsigned pointer-sized integer
                case '*' -> cpu_is_64bit ? inputConvOfU64(desc, offs, type, dsChar) : inputConvOfU32(desc, offs, type, dsChar);
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'e' -> inputConvOfU32(desc, offs, type, dsChar);
            case 's' -> switch (desc.charAt(offs + 1)) {
                // downcall parameter is signed byte
                case 'B', 'c' -> inputConvOfS8(desc, offs, type, dsChar);
                // downcall parameter is signed short
                case 's', 'S' -> inputConvOfS16(desc, offs, type, dsChar);
                // downcall parameter is signed int
                case 'i', 'I' -> inputConvOfS32(desc, offs, type, dsChar);
                // downcall parameter is signed C long
                case 'l' ->
                    stdc_long_is_64bit ? inputConvOfS64(desc, offs, type, dsChar) : inputConvOfS32(desc, offs, type, dsChar);
                // downcall parameter is signed long
                case 'J' -> inputConvOfS64(desc, offs, type, dsChar);
                // downcall parameter is signed pointer-sized integer
                case '*' -> cpu_is_64bit ? inputConvOfS64(desc, offs, type, dsChar) : inputConvOfS32(desc, offs, type, dsChar);
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'c' -> stdc_char_is_signed ? inputConvOfS8(desc, offs, type, dsChar) : inputConvOfU8(desc, offs, type, dsChar);
            case 'w' -> stdc_wchar_is_signed
                    ? stdc_wchar_is_32bit ? inputConvOfS32(desc, offs, type, dsChar) : inputConvOfS16(desc, offs, type, dsChar)
                    : stdc_wchar_is_32bit ? inputConvOfU32(desc, offs, type, dsChar) : inputConvOfU16(desc, offs, type, dsChar);
            case '*' -> inputConvOfPtr(desc, offs, type);
            case 'Z' -> inputConvOfBool(desc, offs, type, dsChar);
            case 'F' -> variadic ? inputConvOfF64(desc, offs, type, dsChar) : inputConvOfF32(desc, offs, type, dsChar);
            case 'D' -> inputConvOfF64(desc, offs, type, dsChar);
            default -> throw new IllegalArgumentException("Invalid native descriptor");
        };
    }

    private static MethodHandle inputConvOfPtr(final String desc, final int offs, final Class<?> type) {
        return switch (type.descriptorString()) {
            case "J" -> staticMethod(MemorySegment.class, "ofAddress", "(J)Ljava/lang/foreign/MemorySegment;", true);
            case "Ljava/lang/foreign/MemorySegment;" -> null;
            case "[B" -> staticMethod(MemorySegment.class, "ofArray", "([B)Ljava/lang/foreign/MemorySegment;", true);
            case "[C" -> staticMethod(MemorySegment.class, "ofArray", "([C)Ljava/lang/foreign/MemorySegment;", true);
            case "[D" -> staticMethod(MemorySegment.class, "ofArray", "([D)Ljava/lang/foreign/MemorySegment;", true);
            case "[F" -> staticMethod(MemorySegment.class, "ofArray", "([F)Ljava/lang/foreign/MemorySegment;", true);
            case "[I" -> staticMethod(MemorySegment.class, "ofArray", "([I)Ljava/lang/foreign/MemorySegment;", true);
            case "[J" -> staticMethod(MemorySegment.class, "ofArray", "([J)Ljava/lang/foreign/MemorySegment;", true);
            case "[S" -> staticMethod(MemorySegment.class, "ofArray", "([S)Ljava/lang/foreign/MemorySegment;", true);
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfBool(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> null;
            case 'B' -> staticMethod(Bootstraps.class, "b2z", "(B)Z", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2z", "(S)Z", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2z", "(C)Z", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2z", "(I)Z", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2z", "(J)Z", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2z", "(D)Z", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2z", "(F)Z", false);
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfS8(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2b", "(Z)B", false);
            case 'B' -> null;
            case 'S' -> staticMethod(Bootstraps.class, "s2b", "(S)B", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2b", "(I)B", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2b", "(J)B", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2b", "(D)B", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2b", "(F)B", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2b", "(Lio/smallrye/ffm/NativeIntValued;)B", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfU8(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2i", "(Z)I", false);
            case 'B' -> staticMethod(Byte.class, "toUnsignedInt", "(B)I", false); // == b2ub
            case 'S' -> staticMethod(Bootstraps.class, "s2ub", "(S)I", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2ub", "(C)I", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2ub", "(I)I", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2ub", "(J)I", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2ub", "(D)I", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2ub", "(F)I", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2ub", "(Lio/smallrye/ffm/NativeIntValued;)I", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfS16(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2s", "(Z)S", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2s", "(B)S", false);
            case 'S' -> null;
            case 'C' -> staticMethod(Bootstraps.class, "c2s", "(C)S", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2s", "(I)S", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2s", "(J)S", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2s", "(D)S", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2s", "(F)S", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2s", "(Lio/smallrye/ffm/NativeIntValued;)S", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfU16(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2c", "(Z)C", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2c", "(S)C", false);
            case 'C' -> null;
            case 'I' -> staticMethod(Bootstraps.class, "i2c", "(I)C", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2c", "(J)C", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2c", "(D)C", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2c", "(F)C", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2c", "(Lio/smallrye/ffm/NativeIntValued;)C", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfS32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2i", "(Z)I", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2i", "(B)I", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2i", "(S)I", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2i", "(C)I", false);
            case 'I' -> null;
            case 'J' -> staticMethod(Bootstraps.class, "l2i", "(J)I", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2i", "(D)I", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2i", "(F)I", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2i", "(Lio/smallrye/ffm/NativeIntValued;)I", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfU32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        if (cpu_is_64bit) {
            assert u32_layout.carrier() == long.class;
            return switch (dsChar) {
                case 'Z' -> staticMethod(Bootstraps.class, "z2l", "(Z)J", false);
                case 'B' -> staticMethod(Byte.class, "toUnsignedLong", "(B)J", false);
                case 'C' -> staticMethod(Bootstraps.class, "c2l", "(C)J", false);
                case 'S' -> staticMethod(Short.class, "toUnsignedLong", "(S)J", false);
                case 'I' -> staticMethod(Integer.class, "toUnsignedLong", "(I)J", false);
                case 'J' -> staticMethod(Bootstraps.class, "l2uil", "(J)J", false);
                case 'D' -> staticMethod(Bootstraps.class, "d2uil", "(D)J", false);
                case 'F' -> staticMethod(Bootstraps.class, "f2uil", "(F)J", false);
                case 'L' -> {
                    if (NativeIntValued.class.isAssignableFrom(type)) {
                        yield staticMethod(Bootstraps.class, "n2uil", "(Lio/smallrye/ffm/NativeIntValued;)J", false);
                    } else {
                        throw invalidTypeForDescType(desc, offs, type);
                    }
                }
                default -> throw invalidTypeForDescType(desc, offs, type);
            };
        } else {
            assert u32_layout.carrier() == int.class;
            return switch (dsChar) {
                case 'Z' -> staticMethod(Bootstraps.class, "z2i", "(Z)I", false);
                case 'B' -> staticMethod(Byte.class, "toUnsignedInt", "(B)I", false);
                case 'S' -> staticMethod(Short.class, "toUnsignedInt", "(S)I", false);
                case 'C' -> staticMethod(Bootstraps.class, "c2i", "(C)I", false);
                case 'I' -> null;
                case 'D' -> staticMethod(Bootstraps.class, "d2ui", "(D)I", false);
                case 'F' -> staticMethod(Bootstraps.class, "f2ui", "(F)I", false);
                case 'J' -> staticMethod(Bootstraps.class, "l2i", "(J)I", false);
                case 'L' -> {
                    if (NativeIntValued.class.isAssignableFrom(type)) {
                        yield staticMethod(Bootstraps.class, "n2i", "(Lio/smallrye/ffm/NativeIntValued;)I", false);
                    } else {
                        throw invalidTypeForDescType(desc, offs, type);
                    }
                }
                default -> throw invalidTypeForDescType(desc, offs, type);
            };
        }
    }

    private static MethodHandle inputConvOfS64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2l", "(Z)J", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2l", "(B)J", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2l", "(S)J", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2l", "(C)J", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2l", "(I)J", false);
            case 'J' -> null;
            case 'D' -> staticMethod(Bootstraps.class, "d2l", "(D)J", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2l", "(F)J", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2l", "(Lio/smallrye/ffm/NativeIntValued;)J", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfU64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2l", "(Z)J", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2l", "(B)J", false);
            case 'S' -> staticMethod(Short.class, "toUnsignedLong", "(S)J", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2l", "(C)J", false);
            case 'I' -> staticMethod(Integer.class, "toUnsignedLong", "(I)J", false);
            case 'J' -> null;
            case 'D' -> staticMethod(Bootstraps.class, "d2ul", "(D)J", false);
            case 'F' -> staticMethod(Bootstraps.class, "f2ul", "(F)J", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(Bootstraps.class, "n2uil", "(Lio/smallrye/ffm/NativeIntValued;)J", false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfF32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2f", "(Z)F", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2f", "(B)F", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2f", "(S)F", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2f", "(C)F", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2f", "(I)F", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2f", "(J)F", false);
            case 'D' -> staticMethod(Bootstraps.class, "d2f", "(D)F", false);
            case 'F' -> null;
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle inputConvOfF64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "z2d", "(Z)D", false);
            case 'B' -> staticMethod(Bootstraps.class, "b2d", "(B)D", false);
            case 'S' -> staticMethod(Bootstraps.class, "s2d", "(S)D", false);
            case 'C' -> staticMethod(Bootstraps.class, "c2d", "(C)D", false);
            case 'I' -> staticMethod(Bootstraps.class, "i2d", "(I)D", false);
            case 'J' -> staticMethod(Bootstraps.class, "l2d", "(J)D", false);
            case 'D' -> null;
            case 'F' -> staticMethod(Bootstraps.class, "f2d", "(F)D", false);
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOf(String desc, int offs, Class<?> type) {
        final char dsChar = type.descriptorString().charAt(0);
        return switch (desc.charAt(offs)) {
            case 'u' -> switch (desc.charAt(offs + 1)) {
                // downcall parameter is unsigned byte
                case 'B', 'c' -> outputConvOfU8(desc, offs, type, dsChar);
                // downcall parameter is unsigned short
                case 's', 'S' -> outputConvOfU16(desc, offs, type, dsChar);
                // downcall parameter is unsigned int
                case 'I', 'i' -> outputConvOfU32(desc, offs, type, dsChar);
                // downcall parameter is unsigned C long
                case 'l' ->
                    stdc_long_is_64bit ? outputConvOfU64(desc, offs, type, dsChar) : outputConvOfU32(desc, offs, type, dsChar);
                // downcall parameter is unsigned long
                case 'J' -> outputConvOfU64(desc, offs, type, dsChar);
                // downcall parameter is unsigned pointer-sized integer
                case '*' ->
                    cpu_is_64bit ? outputConvOfU64(desc, offs, type, dsChar) : outputConvOfU32(desc, offs, type, dsChar);
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'e' -> outputConvOfU32(desc, offs, type, dsChar);
            case 's' -> switch (desc.charAt(offs + 1)) {
                // downcall parameter is signed byte
                case 'B', 'c' -> outputConvOfS8(desc, offs, type, dsChar);
                // downcall parameter is signed short
                case 's', 'S' -> outputConvOfS16(desc, offs, type, dsChar);
                // downcall parameter is signed int
                case 'i', 'I' -> outputConvOfS32(desc, offs, type, dsChar);
                // downcall parameter is signed C long
                case 'l' ->
                    stdc_long_is_64bit ? outputConvOfS64(desc, offs, type, dsChar) : outputConvOfS32(desc, offs, type, dsChar);
                // downcall parameter is signed long
                case 'J' -> outputConvOfS64(desc, offs, type, dsChar);
                // downcall parameter is signed pointer-sized integer
                case '*' ->
                    cpu_is_64bit ? outputConvOfS64(desc, offs, type, dsChar) : outputConvOfS32(desc, offs, type, dsChar);
                default -> throw new IllegalArgumentException("Invalid native descriptor");
            };
            case 'c' ->
                stdc_char_is_signed ? outputConvOfS8(desc, offs, type, dsChar) : outputConvOfU8(desc, offs, type, dsChar);
            case 'w' -> stdc_wchar_is_signed
                    ? stdc_wchar_is_32bit ? outputConvOfS32(desc, offs, type, dsChar)
                            : outputConvOfS16(desc, offs, type, dsChar)
                    : stdc_wchar_is_32bit ? outputConvOfU32(desc, offs, type, dsChar)
                            : outputConvOfU16(desc, offs, type, dsChar);
            case '*' -> outputConvOfPtr(desc, offs, type);
            case 'Z' -> outputConvOfBool(desc, offs, type, dsChar);
            case 'F' -> outputConvOfF32(desc, offs, type, dsChar);
            case 'D' -> outputConvOfF64(desc, offs, type, dsChar);
            default -> throw new IllegalArgumentException("Invalid native descriptor");
        };
    }

    private static MethodHandle outputConvOfPtr(final String desc, final int offs, final Class<?> type) {
        return switch (type.descriptorString()) {
            case "Z" -> staticMethod(Bootstraps.class, "p2z", "(Ljava/lang/foreign/MemorySegment;)Z", false);
            case "I" -> staticMethod(Bootstraps.class, "p2i", "(Ljava/lang/foreign/MemorySegment;)I", false);
            case "J" -> staticMethod(Bootstraps.class, "p2l", "(Ljava/lang/foreign/MemorySegment;)J", false);
            case "Ljava/lang/foreign/MemorySegment;" -> null;
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfBool(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> null;
            case 'B' -> staticMethod(Bootstraps.class, "z2b", "(Z)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "z2s", "(Z)S", false);
            case 'C' -> staticMethod(Bootstraps.class, "z2c", "(Z)C", false);
            case 'I' -> staticMethod(Bootstraps.class, "z2i", "(Z)I", false);
            case 'J' -> staticMethod(Bootstraps.class, "z2l", "(Z)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "z2d", "(Z)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "z2f", "(Z)F", false);
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfS8(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "b2z", "(B)Z", false);
            case 'B' -> null;
            case 'S' -> staticMethod(Bootstraps.class, "b2s", "(B)S", false);
            case 'I' -> staticMethod(Bootstraps.class, "b2i", "(B)I", false);
            case 'J' -> staticMethod(Bootstraps.class, "b2l", "(B)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "b2d", "(B)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "b2f", "(B)F", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield MethodHandles.filterReturnValue(
                            staticMethod(Bootstraps.class, "b2i", "(B)I", false),
                            staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false));
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfU8(final String desc, final int offs, final Class<?> type, final char dsChar) {
        // bits 8-31 are zero, so we can sign-extend into any type safely
        return outputConvOfU32(desc, offs, type, dsChar);
    }

    private static MethodHandle outputConvOfS16(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "s2z", "(S)Z", false);
            case 'B' -> staticMethod(Bootstraps.class, "s2b", "(S)B", false);
            case 'C' -> staticMethod(Bootstraps.class, "s2c", "(S)C", false);
            case 'S' -> null;
            case 'I' -> staticMethod(Bootstraps.class, "s2i", "(S)I", false);
            case 'J' -> staticMethod(Bootstraps.class, "s2l", "(S)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "s2d", "(S)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "s2f", "(S)F", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield MethodHandles.filterReturnValue(
                            staticMethod(Bootstraps.class, "s2i", "(S)I", false),
                            staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false));
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfU16(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "c2z", "(C)Z", false);
            case 'B' -> staticMethod(Bootstraps.class, "c2b", "(C)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "c2s", "(C)S", false);
            case 'C' -> null;
            case 'I' -> staticMethod(Bootstraps.class, "c2i", "(C)I", false);
            case 'J' -> staticMethod(Bootstraps.class, "c2l", "(C)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "c2d", "(C)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "c2f", "(C)F", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield MethodHandles.filterReturnValue(
                            staticMethod(Bootstraps.class, "c2i", "(C)I", false),
                            staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false));
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfS32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "i2z", "(I)Z", false);
            case 'B' -> staticMethod(Bootstraps.class, "i2b", "(I)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "i2s", "(I)S", false);
            case 'C' -> staticMethod(Bootstraps.class, "i2c", "(I)C", false);
            case 'I' -> null;
            case 'J' -> staticMethod(Bootstraps.class, "i2l", "(I)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "i2d", "(I)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "i2f", "(I)F", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false);
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfU32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        if (cpu_is_64bit) {
            assert u32_layout.carrier() == long.class;
            // bits 32-63 are always zero so we can sign-extend
            return outputConvOfS64(desc, offs, type, dsChar);
        } else {
            assert u32_layout.carrier() == int.class;
            return switch (dsChar) {
                case 'J' -> staticMethod(Integer.class, "toUnsignedLong", "(I)J", false);
                case 'D' -> staticMethod(Bootstraps.class, "ui2d", "(I)D", false);
                case 'F' -> staticMethod(Bootstraps.class, "ui2f", "(I)F", false);
                case 'L' -> {
                    if (NativeIntValued.class.isAssignableFrom(type)) {
                        yield staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false);
                    } else {
                        throw invalidTypeForDescType(desc, offs, type);
                    }
                }
                default -> outputConvOfS32(desc, offs, type, dsChar);
            };
        }
    }

    private static MethodHandle outputConvOfS64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'Z' -> staticMethod(Bootstraps.class, "l2z", "(J)Z", false);
            case 'B' -> staticMethod(Bootstraps.class, "l2b", "(J)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "l2s", "(J)S", false);
            case 'C' -> staticMethod(Bootstraps.class, "l2c", "(J)C", false);
            case 'I' -> staticMethod(Bootstraps.class, "l2i", "(J)I", false);
            case 'J' -> null;
            case 'D' -> staticMethod(Bootstraps.class, "l2d", "(J)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "l2f", "(J)F", false);
            case 'L' -> {
                if (NativeIntValued.class.isAssignableFrom(type)) {
                    yield MethodHandles.filterReturnValue(
                            staticMethod(Bootstraps.class, "l2i", "(J)I", false),
                            staticMethod(type, "ofNativeValue", "(I)" + type.descriptorString(), false));
                } else {
                    throw invalidTypeForDescType(desc, offs, type);
                }
            }
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfU64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'D' -> staticMethod(Bootstraps.class, "ul2d", "(J)D", false);
            case 'F' -> staticMethod(Bootstraps.class, "ul2f", "(J)F", false);
            default -> outputConvOfS64(desc, offs, type, dsChar);
        };
    }

    private static MethodHandle outputConvOfF32(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'B' -> staticMethod(Bootstraps.class, "f2b", "(F)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "f2s", "(F)S", false);
            case 'C' -> staticMethod(Bootstraps.class, "f2c", "(F)C", false);
            case 'I' -> staticMethod(Bootstraps.class, "f2i", "(F)I", false);
            case 'Z' -> staticMethod(Bootstraps.class, "f2z", "(F)Z", false);
            case 'J' -> staticMethod(Bootstraps.class, "f2l", "(F)J", false);
            case 'D' -> staticMethod(Bootstraps.class, "f2d", "(F)D", false);
            case 'F' -> null;
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    private static MethodHandle outputConvOfF64(final String desc, final int offs, final Class<?> type, final char dsChar) {
        return switch (dsChar) {
            case 'B' -> staticMethod(Bootstraps.class, "d2b", "(D)B", false);
            case 'S' -> staticMethod(Bootstraps.class, "d2s", "(D)S", false);
            case 'C' -> staticMethod(Bootstraps.class, "d2c", "(D)C", false);
            case 'I' -> staticMethod(Bootstraps.class, "d2i", "(D)I", false);
            case 'Z' -> staticMethod(Bootstraps.class, "d2z", "(D)Z", false);
            case 'J' -> staticMethod(Bootstraps.class, "d2l", "(D)J", false);
            case 'D' -> null;
            case 'F' -> staticMethod(Bootstraps.class, "d2f", "(D)F", false);
            default -> throw invalidTypeForDescType(desc, offs, type);
        };
    }

    // converters (referenced by method handle)

    // native int value enum to...

    @SuppressWarnings("unused")
    private static byte n2b(NativeIntValued val) {
        return (byte) val.nativeValue();
    }

    @SuppressWarnings("unused")
    private static int n2ub(NativeIntValued val) {
        return val.nativeValue() & 0xff;
    }

    @SuppressWarnings("unused")
    private static short n2s(NativeIntValued val) {
        return (short) val.nativeValue();
    }

    @SuppressWarnings("unused")
    private static char n2c(NativeIntValued val) {
        return (char) val.nativeValue();
    }

    @SuppressWarnings("unused")
    private static int n2i(NativeIntValued val) {
        return val.nativeValue();
    }

    @SuppressWarnings("unused")
    private static long n2uil(NativeIntValued val) {
        return val.nativeValue() & 0xffff_ffffL;
    }

    @SuppressWarnings("unused")
    private static long n2l(NativeIntValued val) {
        return val.nativeValue();
    }

    // pointer to...

    @SuppressWarnings("unused")
    private static boolean p2z(MemorySegment val) {
        return val != null && val != MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private static int p2i(MemorySegment val) {
        if (cpu_is_64bit) {
            throw new IllegalArgumentException("Pointer cannot be passed as 32-bit integer on 64-bit platforms");
        }
        if (val.isNative()) {
            return (int) val.address();
        }
        throw new IllegalArgumentException("Heap pointer cannot be passed as integer");
    }

    @SuppressWarnings("unused")
    private static long p2l(MemorySegment val) {
        if (val.isNative()) {
            return val.address();
        }
        throw new IllegalArgumentException("Heap pointer cannot be passed as integer");
    }

    // boolean to...

    @SuppressWarnings("unused")
    private static byte z2b(boolean val) {
        return (byte) z2i(val);
    }

    @SuppressWarnings("unused")
    private static char z2c(boolean val) {
        return (char) z2i(val);
    }

    @SuppressWarnings("unused")
    private static short z2s(boolean val) {
        return (short) z2i(val);
    }

    @SuppressWarnings("unused")
    private static int z2i(boolean val) {
        return val ? 1 : 0;
    }

    @SuppressWarnings("unused")
    private static long z2l(boolean val) {
        return z2i(val);
    }

    @SuppressWarnings("unused")
    private static float z2f(boolean val) {
        return z2i(val);
    }

    @SuppressWarnings("unused")
    private static double z2d(boolean val) {
        return z2i(val);
    }

    // byte to...

    @SuppressWarnings("unused")
    private static short b2s(byte val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static int b2i(byte val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static long b2l(byte val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static float b2f(byte val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static double b2d(byte val) {
        return val;
    }

    // short to...

    @SuppressWarnings("unused")
    private static int s2ub(short val) {
        return val & 0xff;
    }

    @SuppressWarnings("unused")
    private static char s2c(short val) {
        return (char) val;
    }

    @SuppressWarnings("unused")
    private static int s2i(short val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static long s2l(short val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static float s2f(short val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static double s2d(short val) {
        return val;
    }

    // char to...

    @SuppressWarnings("unused")
    private static byte c2b(char val) {
        return (byte) val;
    }

    @SuppressWarnings("unused")
    private static int c2ub(char val) {
        return val & 0xff;
    }

    @SuppressWarnings("unused")
    private static short c2s(char val) {
        return (short) val;
    }

    @SuppressWarnings("unused")
    private static int c2i(char val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static long c2l(char val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static float c2f(char val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static double c2d(char val) {
        return val;
    }

    // int to...

    @SuppressWarnings("unused")
    private static byte i2b(int val) {
        return (byte) val;
    }

    @SuppressWarnings("unused")
    private static int i2ub(int val) {
        return val & 0xff;
    }

    @SuppressWarnings("unused")
    private static short i2s(int val) {
        return (short) val;
    }

    @SuppressWarnings("unused")
    private static long i2l(int val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static float i2f(int val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static double i2d(int val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static boolean i2z(int val) {
        return val != 0;
    }

    // long to...

    @SuppressWarnings("unused")
    private static byte l2b(long val) {
        return (byte) val;
    }

    @SuppressWarnings("unused")
    private static int l2ub(long val) {
        return (int) (val & 0xff);
    }

    @SuppressWarnings("unused")
    private static short l2s(long val) {
        return (short) val;
    }

    @SuppressWarnings("unused")
    private static char l2c(long val) {
        return (char) val;
    }

    @SuppressWarnings("unused")
    private static int l2i(long val) {
        return (int) val;
    }

    @SuppressWarnings("unused")
    private static long l2uil(long val) {
        return Integer.toUnsignedLong((int) val);
    }

    @SuppressWarnings("unused")
    private static float l2f(long val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static double l2d(long val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static boolean l2z(long val) {
        return val != 0;
    }

    // float to...

    @SuppressWarnings("unused")
    private static byte f2b(float val) {
        return (byte) Math.min(Math.max((int) val, Byte.MIN_VALUE), Byte.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static int f2ub(float val) {
        return Math.min(Math.max((int) val, 0), 0xff);
    }

    @SuppressWarnings("unused")
    private static short f2s(float val) {
        return (short) Math.min(Math.max((int) val, Short.MIN_VALUE), Short.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static char f2c(float val) {
        return (char) Math.min(Math.max((int) val, Character.MIN_VALUE), Character.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static int f2i(float val) {
        return (int) val;
    }

    @SuppressWarnings("unused")
    private static long f2uil(float val) {
        return Math.min(Math.max((long) val, 0), 0xffff_ffffL);
    }

    @SuppressWarnings("unused")
    private static int f2ui(float val) {
        return (int) f2uil(val);
    }

    @SuppressWarnings("unused")
    private static long f2l(float val) {
        return (long) val;
    }

    @SuppressWarnings("unused")
    private static long f2ul(float val) {
        return d2ul(val);
    }

    @SuppressWarnings("unused")
    private static double f2d(float val) {
        return val;
    }

    @SuppressWarnings("unused")
    private static boolean f2z(float val) {
        return val != 0;
    }

    // double to...

    @SuppressWarnings("unused")
    private static byte d2b(double val) {
        return (byte) Math.min(Math.max((int) val, Byte.MIN_VALUE), Byte.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static int d2ub(double val) {
        return Math.min(Math.max((int) val, 0), 0xff);
    }

    @SuppressWarnings("unused")
    private static short d2s(double val) {
        return (short) Math.min(Math.max((int) val, Short.MIN_VALUE), Short.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static char d2c(double val) {
        return (char) Math.min(Math.max((int) val, Character.MIN_VALUE), Character.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    private static int d2i(double val) {
        return (int) val;
    }

    @SuppressWarnings("unused")
    private static long d2uil(double val) {
        return Math.min(Math.max((long) val, 0), 0xffff_ffffL);
    }

    @SuppressWarnings("unused")
    private static int d2ui(double val) {
        return (int) d2uil(val);
    }

    @SuppressWarnings("unused")
    private static long d2l(double val) {
        return (long) val;
    }

    @SuppressWarnings("unused")
    private static long d2ul(double val) {
        if (Double.isNaN(val) || val < 0) {
            return 0;
        }
        if (val > Math.pow(2, 64) - 1) {
            return 0xffff_ffff_ffff_ffffL;
        }
        if (val <= Long.MAX_VALUE) {
            return (long) val;
        }
        long lv = Long.MAX_VALUE + (long) (val - (double) Long.MAX_VALUE) + 1;
        if (lv >= 0) {
            return Long.MIN_VALUE;
        }
        return lv;
    }

    @SuppressWarnings("unused")
    private static float d2f(double val) {
        return (float) val;
    }

    @SuppressWarnings("unused")
    private static boolean d2z(double val) {
        return val != 0;
    }

    private static final MethodHandle errnoHandle;

    static {
        // todo: this should not be needed on 26+
        // ref: https://bugs.openjdk.org/browse/JDK-8378559
        Linker linker = Linker.nativeLinker();
        errnoHandle = linker.downcallHandle(linker.defaultLookup().findOrThrow(switch (OS.current()) {
            case WINDOWS, Z -> "_errno";
            case LINUX -> "__errno_location";
            case MAC -> "__error";
            case AIX -> "_Errno";
            case SOLARIS -> "___errno";
            default -> throw new UnsupportedOperationException("Cannot resolve errno function on current OS");
        }), FunctionDescriptor.of(ValueLayout.ADDRESS), Linker.Option.critical(false));
    }

    private static final VarHandle errnoGroupHandle = callStateHandle(lookup(), "errno", VarHandle.class);

    @SuppressWarnings("unused")
    private static MemorySegment fillErrno(MemorySegment capture) throws Throwable {
        ((MemorySegment) errnoHandle.invokeExact()).reinterpret(4).set(ValueLayout.JAVA_INT, 0,
                (int) errnoGroupHandle.get(capture, 0L));
        return capture;
    }

    private static IllegalArgumentException invalidTypeForDescType(final String desc, final int offs, final Class<?> type) {
        return new IllegalArgumentException(
                "Invalid type " + type + " for native descriptor type " + desc.substring(offs, offs + charCount(desc, offs)));
    }

    private static WrongMethodTypeException wrongType(final MethodType type, final MethodType downcallType) {
        return new WrongMethodTypeException(
                "Incompatible types (expected " + type + " but downcall type is " + downcallType + ")");
    }

    private static void throwLinkError(String name) {
        throw new UnsatisfiedLinkError("Cannot link to unknown function `" + name + "`");
    }

    static Error toError(final ReflectiveOperationException ex) {
        Error error = switch (ex) {
            case IllegalAccessException __ -> new IllegalAccessError(ex.getMessage());
            case NoSuchMethodException __ -> new NoSuchMethodError(ex.getMessage());
            default -> new InternalError(ex.toString());
        };
        error.setStackTrace(ex.getStackTrace());
        if (ex.getCause() != null) {
            error.initCause(ex.getCause());
        }
        return error;
    }

    private static final MethodHandle doThrow;

    static {
        MethodHandles.Lookup lookup = lookup();
        try {
            doThrow = lookup.findStatic(Bootstraps.class, "throwLinkError", MethodType.methodType(void.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw toError(e);
        }
    }

    private static IllegalArgumentException wrongType() {
        return new IllegalArgumentException("Wrong type");
    }
}
