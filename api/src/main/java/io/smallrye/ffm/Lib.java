package io.smallrye.ffm;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;

/**
 * Declare that the annotated type or method requires the named library or libraries to be loaded
 * and searched in the given order.
 * A name is mapped to a library file name with {@link System#mapLibraryName(String)} (for example, {@code z} is
 * {@code libz.so} on Linux), unless it is an absolute path or already a library file name
 * (such as {@code libz.so.1}, {@code libz.1.dylib} or {@code zlib1.dll}).
 * A few special names are recognized:
 * <ul>
 * <li>{@code <<SYSTEM>>} - use the system default library lookup as returned by {@link Linker#defaultLookup()}</li>
 * <li>{@code <<LOADER>>} - use the class loader's default library lookup as returned by {@link SymbolLookup#loaderLookup()}
 * (this would include any symbols loaded via {@link System#loadLibrary} for example)</li>
 * </ul>
 * If this annotation is present on both a method and its enclosing type, the annotation on the method takes precedence.
 * This annotation is not inherited to nested types nor is it inherited through the type hierarchy.
 * If this annotation is not present, methods will be linked as if annotated with {@code @Lib("<<LOADER>>") @Lib("<<SYSTEM>>")}.
 */
@Repeatable(Lib.List.class)
@Target({ TYPE, METHOD })
@Retention(CLASS)
public @interface Lib {
    /**
     * {@return the library name}
     */
    String value();

    /**
     * The list annotation holder for {@code @Lib}.
     */
    @Target({ TYPE, METHOD })
    @Retention(CLASS)
    @interface List {
        /**
         * {@return the list value}
         */
        Lib[] value();
    }
}
