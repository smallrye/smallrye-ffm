# SmallRye FFM

## Overview

SmallRye FFM is a utility to simplify usage of the Java FFM API.
It is used by way of an API module plus a build system plugin.

Using the SmallRye FFM API, native functions can be modelled using the `native` method feature of the Java language.
The build system plugin replaces these methods with methods which use the special `invokedynamic` JVM instruction to lazily link to native functions.

## Usage

### Maven

To use SmallRye FFM, the API artifact dependency and the build system plugin must be added to build.

To add the API dependency using Maven, add the following to the `dependencies` section of your project `pom.xml`:

```xml
<dependency>
    <groupId>io.smallrye.ffm</groupId>
    <artifactId>smallrye-ffm</artifactId>
    <version>LATEST</version>
</dependency>
```

You should replace `LATEST` with the latest release version in order to ensure a stable build.

To add the plugin using Maven, add the following to the `plugins` sub-element of the `build` section of your project `pom.xml`:

```xml
<plugin>
    <groupId>io.smallrye.ffm</groupId>
    <artifactId>smallrye-ffm-maven-plugin</artifactId>
    <version>LATEST</version>
    <executions>
        <execution>
            <id>ffm</id>
            <goals>
                <goal>transform</goal>
            </goals>
        </execution>
        <execution>
            <id>ffm-test</id>
            <goals>
                <goal>transform-test</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Again, you should replace `LATEST` with the latest release version in order to ensure a stable build.

### Native function methods

To declare a native function method, create a `native` method in your class
(it can be a `static` method or an instance method)
and annotate it with the `@Link` annotation.

```java
// simple example: call the libc `cosf` function
@Link
private static native float cosf(float value);
```

The first invocation of this method will link the call site before invoking the function.
Subsequent invocations will directly invoke the function through the linked downcall method handle.
If the function cannot be linked, calling the native method will result in an `UnsatisfiedLinkError` being thrown.

### Types and `@As`

The C ABI and calling conventions allow a high degree of variability between platforms.
Basic C types, such as `int` or `long`, may have different sizes between platforms.
Additionally, the signedness of `char` is highly platform-dependent.
The ABI for certain OSes and/or CPUs may also specify special rules for handling certain value types.

Therefore, to ensure correct operation, it is usually necessary to declare the C type of each
function argument and the function return value.
This allows SmallRye FFM to apply the correct integer conversion at link time to ensure correct operation.
This can be accomplished using the `@As` annotation.

The `@As` annotation takes a single value which corresponds to the native C type to use for a given argument
or the method return value.

The following example specifies the type of both the argument and return type of a function
to correspond to the C `long int` type, whose size is platform-dependent.

```java
// example of specifying explicit types
@Link
@As(stdc_long)
private static native long labs(@As(stdc_long) long value);
```

### Supported types

The full set of supported types is found and documented in the `AsType` enumeration.
Some of the more frequently useful types are:

* `stdc_char` - the C `char` type
* `stdc_unsigned_char` - the C `unsigned char` type
* `stdc_int` - the C signed `int` type, also useful for `enum` type arguments
* `stdc_unsigned_int` - the C `unsigned int` type
* `f32` - a 32-bit float type (usable for the C type `float`)
* `f64` - a 64-bit float type (usable for the C type `double`)
* `uintptr` - an unsigned pointer-sized integer (usable for the standard C type `size_t` among other things)
* `ptr` - a pointer value (use this in conjunction with `MemorySegment`, `String`, or primitive arrays)

By default, this is the mapping of Java types to `AsType` constants:

* `byte` - `AsType.s8`
* `short` - `AsType.s16`
* `char` - `AsType.u16`
* `int` - `AsType.s32`
* `long` - `AsType.s64`
* `boolean` - `AsType.stdc_bool`
* `float` - `AsType.f32`
* `double` - `AsType.f64`
* `MemorySegment`, primitive arrays, and `String` - `AsType.ptr`

### `Errno` support

SmallRye FFM includes an enumeration called `Errno` which can be used as an integer argument or return type.
Because it is a Java `enum`, `Errno` values can be used in `switch` statements.

Since most standard C or POSIX functions do not return `Errno` directly, there is also an `ErrnoConsumer`
interface which can provided as an argument to the `native` method.
The consumer will be populated with the `Errno` constant after the method is called.
Note that many APIs do not specify whether the `Errno` value is sensible if the operation succeeded,
so care should be taken to check the result of the function before using the error value
if the function has a separate success indicator.

```java
// example of handling errno
@Link
private static native double log(double value, ErrnoConsumer handler);

public static double log(double value) throws ArithmeticException {
    var errnoHolder = new ErrnoConsumer() {
        Errno errno;

        public void accept(Errno errno) {
            this.errno = errno;
        }
    };
    double result = log(value, errnoHolder);
    if (errnoHolder.errno != Errno.SUCCESS) {
        throw new ArithmeticException(errnoHolder.errno.message());
    }
    return result;
}
```

