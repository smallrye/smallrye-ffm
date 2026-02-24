package io.smallrye.ffm;

/**
 * The various supported native types.
 */
public enum AsType {
    /**
     * The type of C's {@code char} type on this platform.
     * It may be signed or unsigned, depending on the OS and CPU.
     */
    stdc_char(Bootstraps.stdc_char_is_signed ? Signedness.signed : Signedness.unsigned),
    /**
     * The type of C's {@code signed char} type on this platform.
     */
    stdc_signed_char(Signedness.signed),
    /**
     * The type of C's {@code unsigned char} type on this platform.
     */
    stdc_unsigned_char(Signedness.unsigned),
    /**
     * The type of C's {@code short} (a.k.a. {@code short int}) type on this platform.
     * This type is always signed.
     */
    stdc_short(Signedness.signed),
    /**
     * The type of C's {@code unsigned short} (a.k.a. {@code unsigned short int}) type on this platform.
     */
    stdc_unsigned_short(Signedness.unsigned),
    /**
     * The type of C's {@code int} type on this platform.
     * This type is always signed.
     * This is the default conversion for values of type {@link Errno}.
     */
    stdc_int(Signedness.signed),
    /**
     * The type of C's {@code unsigned int} (a.k.a. {@code unsigned}) type on this platform.
     */
    stdc_unsigned_int(Signedness.unsigned),
    /**
     * The type of C's {@code long} (a.k.a. {@code long int}) type on this platform.
     * This type is always signed.
     */
    stdc_long(Signedness.signed),
    /**
     * The type of C's {@code unsigned long} (a.k.a. {@code unsigned long int}) type on this platform.
     */
    stdc_unsigned_long(Signedness.unsigned),
    /**
     * The type of C's {@code long long} (a.k.a. {@code long long int}) type on this platform.
     * This type is always signed.
     */
    stdc_long_long(Signedness.signed),
    /**
     * The type of C's {@code unsigned long long} (a.k.a. {@code unsigned long long int}) type on this platform.
     */
    stdc_unsigned_long_long(Signedness.unsigned),

    /**
     * A boolean value.
     * This is the default conversion for {@code boolean}-typed values.
     * This type is also suitable for C types such as {@code bool} or {@code _Bool}.
     */
    stdc_bool,

    /**
     * The type of C's {@code wchar_t} type on this platform.
     * This type may be 16 or 32 bits so care should be taken when creating or interpreting values of this type.
     */
    stdc_wchar_t(Bootstraps.stdc_wchar_is_signed ? Signedness.signed : Signedness.unsigned),

    /**
     * A signed 8-bit integer.
     * This is the default conversion for {@code byte}-typed values.
     * This type is also suitable for C types such as {@code int8_t}.
     */
    s8(Signedness.signed),
    /**
     * A signed 16-bit integer.
     * This is the default conversion for {@code short}-typed values.
     * This type is also suitable for C types such as {@code int16_t}.
     */
    s16(Signedness.signed),
    /**
     * A signed 32-bit integer.
     * This is the default conversion for {@code int}-typed values.
     * This type is also suitable for C types such as {@code int32_t}.
     */
    s32(Signedness.signed),
    /**
     * A signed 64-bit integer.
     * This is the default conversion for {@code long}-typed values.
     * This type is also suitable for C types such as {@code int64_t}.
     */
    s64(Signedness.signed),

    /**
     * An unsigned 8-bit integer.
     * This type is also suitable for C types such as {@code char8_t} or {@code uint8_t}.
     */
    u8(Signedness.unsigned),
    /**
     * An unsigned 16-bit integer.
     * This is the default conversion for {@code char}-typed values.
     * This type is also suitable for C types such as {@code char16_t} or {@code uint16_t}.
     */
    u16(Signedness.unsigned),
    /**
     * An unsigned 32-bit integer.
     * This type is also suitable for C types such as {@code char32_t} or {@code uint32_t}.
     */
    u32(Signedness.unsigned),
    /**
     * An unsigned 64-bit integer.
     * This type is also suitable for C types such as {@code uint64_t}.
     */
    u64(Signedness.unsigned),

    /**
     * A 32-bit floating-point number.
     * This is the default conversion for {@code float}-typed values.
     */
    f32,
    /**
     * A 64-bit floating-point number.
     * This is the default conversion for {@code float}-typed values.
     */
    f64,

    /**
     * A pointer-sized signed integer. Usable for {@code ptrdiff_t} or {@code ssize_t} typed arguments.
     */
    intptr(Signedness.signed),
    /**
     * A pointer-sized unsigned integer. Usable for {@code uintptr_t} or {@code size_t} typed arguments.
     */
    uintptr(Signedness.unsigned),

    /**
     * A pointer.
     * This is the default conversion for {@code String}-, {@code MemorySegment}-, and array-typed values.
     */
    ptr,
    /**
     * The void type.
     * This is the default conversion for {@code void} return types.
     */
    void_,
    ;

    private final Signedness signedness;

    AsType(final Signedness signedness) {
        this.signedness = signedness;
    }

    AsType() {
        this(Signedness.none);
    }

    /**
     * {@return {@code true} if this type is signed, or {@code false} if it is not signed, including the case where this type
     * does not have signedness}
     */
    public boolean isSigned() {
        return signedness == Signedness.signed;
    }

    /**
     * {@return {@code true} if this type is unsigned, or {@code false} if it is not unsigned, including the case where this
     * type does not have signedness}
     */
    public boolean isUnsigned() {
        return signedness == Signedness.unsigned;
    }

    enum Signedness {
        none,
        signed,
        unsigned,
    }
}
