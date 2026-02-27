package io.smallrye.ffm.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.foreign.MemorySegment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.ffm.Errno;
import io.smallrye.ffm.ErrnoConsumer;
import io.smallrye.ffm.Link;
import io.smallrye.ffm.Out;

public final class ErrnoTests {
    public ErrnoTests() {
    }

    @Test
    public void testConstants() {
        // make sure there are native values at all
        Assertions.assertEquals(switch (OS.current()) {
            case Z -> 1;
            default -> 33;
        }, Errno.EDOM.nativeValue());
        // make sure the array is populated correctly
        for (Errno errno : Errno.values()) {
            if (errno.isPresent()) {
                if (errno == Errno.EWOULDBLOCK && Errno.EWOULDBLOCK.nativeValue() == Errno.EAGAIN.nativeValue()
                        || errno == Errno.EOPNOTSUPP && Errno.ENOTSUP.nativeValue() == Errno.EOPNOTSUPP.nativeValue()
                        || errno == Errno.EDEADLK && Errno.EDEADLOCK.nativeValue() == Errno.EDEADLK.nativeValue()) {
                    // some platforms have the same native value for these two constants
                    continue;
                }
                assertEquals(errno, Errno.ofNativeValue(errno.nativeValue()));
            }
        }
    }

    @Test
    public void testStrings() {
        String lang = System.getenv("LANG");
        assumeTrue(lang == null || lang.startsWith("en") || lang.equals("C"));
        // just make sure it's functioning
        assertEquals(switch (OS.current()) {
            default -> "Resource temporarily unavailable";
        }, Errno.EAGAIN.message());
    }

    @Test
    public void testErrorResult() {
        var errnoHolder = new ErrnoConsumer() {
            Errno errno;

            public void accept(Errno errno) {
                this.errno = errno;
            }
        };
        double result;
        result = strtod("1.18973e+4932", MemorySegment.NULL, errnoHolder);
        assertEquals(Errno.ERANGE, errnoHolder.errno);
        assertTrue(Double.isInfinite(result));
        result = strtod("1234.0", MemorySegment.NULL, errnoHolder);
        assertEquals(Errno.SUCCESS, errnoHolder.errno);
        assertEquals(1234.0, result);
    }

    @Link
    private static native double strtod(String strVal, @Out MemorySegment endPtr, ErrnoConsumer errnoConsumer);

    @Test
    public void matchErrnoValues() {
        // TODO: other OSes
        assumeTrue(OS.current() == OS.LINUX || OS.current() == OS.MAC);
        Pattern p = Pattern.compile("([^A-Z0-9_]+) (\\d+) (.*)");
        ProcessBuilder.newBuilder("errno").arguments("-l").output().consumeLinesWith(1000, line -> {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                Errno errno = Errno.valueOf(m.group(0));
                assertEquals(Integer.parseInt(m.group(1)), errno.nativeValue());
                assertEquals(m.group(2), errno.message());
            }
        }).run();
    }
}
