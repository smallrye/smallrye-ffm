package io.smallrye.ffm;

import static io.smallrye.ffm.AsType.*;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import io.smallrye.common.os.OS;

/**
 * Errno values.
 */
@SuppressWarnings("SpellCheckingInspection")
public enum Errno implements NativeIntValued {
    /**
     * An error value that is unknown.
     */
    UNKNOWN,
    /**
     * Value indicating no registered error.
     */
    SUCCESS(0),
    // actual codes follow; keep alphabetical
    E2BIG(OS.Z, 145, 7),
    EACCES(OS.Z, 111, 13),
    EADDRINUSE(67, 125, 48, 98, 100, 1115),
    EADDRNOTAVAIL(68, 126, 49, 99, 101, 1116),
    EADV(-1, -1, -1, 68, -1, 1145),
    EAFNOSUPPORT(66, 124, 47, 97, 102, 1114),
    EAGAIN(11, 11, 35, 11, 11, 112),
    EALREADY(56, 42, 37, 114, 103, 1104),
    EAUTH(OS.MAC, 80),
    EBADARCH(OS.MAC, 86),
    EBADE(OS.LINUX, 52),
    EBADEXEC(OS.MAC, 85),
    EBADF(OS.Z, 113, 9),
    EBADFD(OS.LINUX, 77),
    EBADMACHO(OS.MAC, 88),
    EBADMSG(120, 77, 94, 74, 104, 1140),
    EBADR(OS.LINUX, 53),
    EBADRPC(OS.MAC, 72),
    EBADRQC(OS.LINUX, 56),
    EBADSLT(OS.LINUX, 57),
    EBFONT(OS.LINUX, 59),
    EBUSY(OS.Z, 114, 16),
    ECANCELED(117, 47, 89, 125, 105, 1152),
    ECHILD(OS.Z, 115, 10),
    ECHRNG(OS.LINUX, 44),
    ECOMM(-1, -1, -1, 70, -1, 1147),
    ECONNABORTED(72, 130, 53, 103, 106, 1120),
    ECONNREFUSED(79, 146, 61, 111, 107, 1128),
    ECONNRESET(73, 131, 54, 104, 108, 1121),
    EDEADLK(45, 45, 11, 35, 36, 116),
    EDEADLOCK(OS.LINUX, 35),
    EDESTADDRREQ(58, 96, 39, 89, 109, 1106),
    EDEVERR(OS.MAC, 83),
    EDOM(OS.Z, 1, 33),
    EDOTDOT(-1, -1, -1, 73, -1, 1150),
    EDQUOT(-1, -1, -1, 122, -1, 1133),
    EEXIST(OS.Z, 117, 17),
    EFAULT(OS.Z, 118, 14),
    EFBIG(OS.Z, 119, 27),
    EFTYPE(OS.MAC, 79),
    EHOSTDOWN(-1, -1, 64, 112, -1, 1129),
    EHOSTUNREACH(81, 148, 65, 113, 110, 1130),
    EHWPOISON(OS.LINUX, 133),
    EIBMBADTCPNAME(OS.Z, 1011),
    EIBMCANCELLED(OS.Z, 1009),
    EIBMCONFLICT(OS.Z, 1008),
    EIBMSOCKINUSE(OS.Z, 1003),
    EIBMSOCKOUTOFRANGE(OS.Z, 1002),
    EIDRM(36, 36, 90, 43, 111, 1141),
    EILSEQ(116, 88, 92, 84, 42, 147),
    EINPROGRESS(55, 150, 36, 115, 112, 1103),
    EINTR(OS.Z, 120, 4),
    EINTRNODATA(OS.Z, 1159),
    EINVAL(OS.Z, 121, 22),
    EIO(OS.Z, 122, 5),
    EISCONN(75, 133, 56, 106, 113, 1123),
    EISDIR(OS.Z, 123, 21),
    EISNAM(OS.LINUX, 120),
    EKEYEXPIRED(OS.LINUX, 127),
    EKEYREJECTED(OS.LINUX, 129),
    EKEYREVOKED(OS.LINUX, 128),
    EL2HLT(OS.LINUX, 51),
    EL2NSYNC(OS.LINUX, 45),
    EL3HLT(OS.LINUX, 46),
    EL3RST(OS.LINUX, 47),
    ELIBACC(OS.LINUX, 79),
    ELIBBAD(OS.LINUX, 80),
    ELIBEXEC(OS.LINUX, 83),
    ELIBMAX(OS.LINUX, 82),
    ELIBSCN(OS.LINUX, 81),
    ELNRNG(OS.LINUX, 48),
    ELOOP(85, 90, 62, 40, 114, 146),
    EMEDIUMTYPE(OS.LINUX, 124),
    EMFILE(OS.Z, 124, 24),
    EMLINK(OS.Z, 125, 31),
    EMSGSIZE(59, 97, 40, 90, 115, 1107),
    EMULTIHOP(-1, -1, 95, 72, -1, 1149),
    EMVSARMERROR(OS.Z, 172),
    EMVSCATLG(OS.Z, 153),
    EMVSCPLERROR(OS.Z, 171),
    EMVSCVAF(OS.Z, 152),
    EMVSDYNALC(OS.Z, 151),
    EMVSERR(OS.Z, 157),
    EMVSEXPIRE(OS.Z, 168),
    EMVSINITIAL(OS.Z, 156),
    EMVSNORTL(OS.Z, 167),
    EMVSNOTUP(OS.Z, 150),
    EMVSPARM(OS.Z, 158),
    EMVSPASSWORD(OS.Z, 169),
    EMVSPFSFILE(OS.Z, 159),
    EMVSPFSPERM(OS.Z, 162),
    EMVSSAF2ERR(OS.Z, 164),
    EMVSSAFEXTRERR(OS.Z, 163),
    EMVSWLMERROR(OS.Z, 170),
    ENAMETOOLONG(86, 78, 63, 36, 38, 126),
    ENAVAIL(OS.LINUX, 119),
    ENEEDAUTH(OS.MAC, 81),
    ENETDOWN(69, 127, 50, 100, 116, 1117),
    ENETRESET(71, 129, 52, 102, 117, 1119),
    ENETUNREACH(70, 128, 51, 101, 118, 1118),
    ENFILE(OS.Z, 127, 23),
    ENOANO(OS.LINUX, 55),
    ENOATTR(OS.MAC, 93),
    ENOBUFS(74, 132, 55, 105, 119, 1122),
    ENOCSI(OS.LINUX, 50),
    ENODATA(122, 61, 96, 61, 120),
    ENODEV(OS.Z, 128, 19),
    ENOENT(OS.Z, 129, 2),
    ENOEXEC(OS.Z, 130, 8),
    ENOKEY(OS.LINUX, 126),
    ENOLCK(49, 46, 77, 37, 39, 131),
    ENOLINK(126, 67, 97, 67, 121, 1144),
    ENOMEDIUM(OS.LINUX, 123),
    ENOMEM(OS.Z, 132, 12),
    ENOMOVE(OS.Z, 1161),
    ENOMSG(35, 35, 91, 42, 122, 1139),
    ENONET(-1, -1, -1, 64, -1, 1142),
    ENOPKG(OS.LINUX, 65),
    ENOPOLICY(OS.MAC, 103),
    ENOPROTOOPT(61, 99, 42, 92, 123, 1109),
    ENOREUSE(OS.Z, 1160),
    ENOSPC(OS.Z, 133, 28),
    ENOSR(118, 63, 98, 63, 124, 1138),
    ENOSTR(123, 60, 99, 60, 125, 1136),
    ENOSYS(109, 89, 78, 38, 40, 134),
    ENOTBLK(OS.Z, 1100, 15),
    ENOTCONN(76, 134, 57, 107, 126, 1124),
    ENOTDIR(OS.Z, 135, 20),
    ENOTEMPTY(87, 93, 66, 39, 41, 136),
    ENOTNAM(OS.LINUX, 118),
    ENOTRECOVERABLE(-1, 59, 104, 131, 127),
    ENOTSOCK(57, 95, 38, 88, 128, 1105),
    ENOTSUP(124, 48, 45, 95, 129, 247),
    ENOTTY(OS.Z, 137, 25),
    ENOTUNIQ(OS.LINUX, 76),
    ENXIO(OS.Z, 138, 6),
    EOFFLOADboxDOWN(OS.Z, 1007),
    EOFFLOADboxERROR(OS.Z, 1005),
    EOFFLOADboxRESTART(OS.Z, 1006),
    EOPNOTSUPP(64, 122, 102, 95, 130, 1112),
    EOTHER(OS.WINDOWS, 131),
    EOVERFLOW(127, 79, 84, 75, 132, 149),
    EOWNERDEAD(-1, 58, 105, 130, 133),
    EPERM(OS.Z, 139, 1),
    EPFNOSUPPORT(-1, -1, 46, 96, -1, 1113),
    EPIPE(OS.Z, 140, 32),
    EPROCLIM(-1, -1, 67, -1, -1, 1131),
    EPROCUNAVAIL(OS.MAC, 76),
    EPROGMISMATCH(OS.MAC, 75),
    EPROGUNAVAIL(OS.MAC, 74),
    EPROTO(121, 71, 100, 71, 134, 1148),
    EPROTONOSUPPORT(62, 120, 43, 93, 135, 1110),
    EPROTOTYPE(69, 98, 41, 91, 136, 1108),
    EPWROFF(OS.MAC, 82),
    EQFULL(OS.MAC, 106),
    ERANGE(OS.Z, 2, 34),
    EREMCHG(-1, -1, -1, 78, -1, 1151),
    EREMOTE(-1, -1, 71, 66, -1, 1135),
    EREMOTEIO(OS.LINUX, 121),
    ERESTART(OS.LINUX, 85),
    ERFKILL(OS.LINUX, 132),
    EROFS(OS.Z, 141, 30),
    ERPCMISMATCH(OS.MAC, 73),
    ERREMOTE(OS.Z, 1143),
    ESHLIBVERS(OS.MAC, 87),
    ESHUTDOWN(-1, -1, 58, 108, -1, 1125),
    ESOCKTNOSUPPORT(-1, -1, 44, 94, -1, 1111),
    ESPIPE(OS.Z, 142, 29),
    ESRCH(OS.Z, 143, 3),
    ESRMNT(-1, -1, -1, 69, -1, 1146),
    ESTALE(52, 151, 70, 116, -1, 1134),
    ESTRPIPE(OS.LINUX, 86),
    ETIME(119, 62, 101, 62, 137, 1137),
    ETIMEDOUT(78, 145, 60, 110, 138, 1127),
    ETOOMANYREFS(-1, -1, 59, 109, -1, 1126),
    ETXTBSY(26, 26, 26, 26, 139, 1101),
    ETcpBadObj(OS.Z, 1155),
    ETcpClosed(OS.Z, 1156),
    ETcpErr(OS.Z, 1158),
    ETcpLinked(OS.Z, 1157),
    ETcpOutOfState(OS.Z, 1153),
    ETcpUnattach(OS.Z, 1154),
    EUCLEAN(OS.LINUX, 117),
    EUNATCH(-1, -1, -1, 49, -1, 3448),
    EUSERS(-1, -1, 68, 87, -1, 1132),
    EWOULDBLOCK(54, 11, 35, 11, 140, 1102),
    EXDEV(OS.Z, 144, 18),
    EXFULL(OS.LINUX, 54),
    ;

    private final int nativeValue;
    private String message;

    Errno() {
        nativeValue = -1;
        message = "Unknown error";
    }

    /**
     * Construct a new instance with a native value that is the same across all OSes.
     *
     * @param value the native value
     */
    Errno(int value) {
        this.nativeValue = value;
    }

    /**
     * Construct a new instance with a native value that only exists on one OS.
     *
     * @param os the OS (must not be {@code null})
     * @param value the native value for that OS
     */
    Errno(OS os, int value) {
        this(os, value, -1);
    }

    /**
     * Construct a new instance with a native value that only exists on one OS and is otherwise the same across
     * all OSes.
     *
     * @param os the OS (must not be {@code null})
     * @param value the native value for that OS
     * @param defaultValue the native value for other OSes
     */
    Errno(OS os, int value, int defaultValue) {
        this(os == OS.current() ? value : defaultValue);
    }

    /**
     * Construct a new instance.
     *
     * @param aix the value for AIX
     * @param solaris the value for Solaris
     * @param mac the value for macOS
     * @param linux the value for Linux
     * @param windows the value for Windows
     */
    Errno(int aix, int solaris, int mac, int linux, int windows) {
        this(switch (OS.current()) {
            case AIX -> aix;
            case SOLARIS -> solaris;
            case MAC -> mac;
            case LINUX -> linux;
            case WINDOWS -> windows;
            default -> -1;
        });
    }

    /**
     * Construct a new instance.
     *
     * @param aix the value for AIX
     * @param solaris the value for Solaris
     * @param mac the value for macOS
     * @param linux the value for Linux
     * @param windows the value for Windows
     * @param z the value for z/OS
     */
    Errno(int aix, int solaris, int mac, int linux, int windows, int z) {
        this(switch (OS.current()) {
            case AIX -> aix;
            case SOLARIS -> solaris;
            case MAC -> mac;
            case LINUX -> linux;
            case WINDOWS -> windows;
            case Z -> z;
            default -> -1;
        });
    }

    /**
     * An immutable list of all values in ordinal order.
     */
    public static final List<Errno> values = List.of(values());

    private static final Errno[] fromInt;

    static {
        Errno[] array = new Errno[values.stream().filter(Errno::isPresent).mapToInt(Errno::nativeValue).max().orElse(0) + 1];
        Arrays.fill(array, Errno.UNKNOWN);
        for (Errno value : values) {
            if (value.isPresent()) {
                int nv = value.nativeValue();
                if (array[nv] != EAGAIN) {
                    // EAGAIN is preferred over EWOULDBLOCK
                    array[nv] = value;
                }
            }
        }
        fromInt = array;
    }

    /**
     * {@return the {@code Errno} constant for the given native value, or {@link #UNKNOWN} if there is no constant for this
     * value}
     *
     * @param value the native value
     */
    public static Errno ofNativeValue(int value) {
        if (value < 0 || value >= fromInt.length) {
            return UNKNOWN;
        } else {
            return fromInt[value];
        }
    }

    private static final VarHandle errnoHandle = Bootstraps.callStateHandle(MethodHandles.lookup(), "errno", VarHandle.class);

    /**
     * {@return the {@code errno} value from the given captured call state buffer}
     *
     * @param segment the captured call state buffer (must not be {@code null})
     */
    public static Errno fromCallState(MemorySegment segment) {
        return ofNativeValue((int) errnoHandle.get(segment));
    }

    /**
     * {@return the native integer value for this constant, or {@code -1} if there is none}
     */
    @Override
    public int nativeValue() {
        if (!isPresent()) {
            throw new NoSuchElementException();
        }
        return nativeValue;
    }

    /**
     * {@return {@code true} if this constant has a native value on the current operating system, or {@code false} if it does
     * not}
     */
    @Override
    public boolean isPresent() {
        return nativeValue != -1;
    }

    /**
     * {@return the native string message for this error number (not {@code null})}
     */
    public String message() {
        String message = this.message;
        if (message == null) {
            byte[] buf = new byte[256];
            if (switch (OS.current()) {
                case WINDOWS -> strerror_s(buf, buf.length, nativeValue);
                case LINUX -> __xpg_strerror_r(nativeValue, buf, buf.length);
                default -> strerror_r(nativeValue, buf, buf.length);
            } == SUCCESS) {
                for (int idx = 0; idx < buf.length; idx++) {
                    if (buf[idx] == 0) {
                        message = this.message = idx == 0 ? UNKNOWN.message() : new String(buf, 0, idx, StandardCharsets.UTF_8);
                        break;
                    }
                }
                if (message == null) {
                    message = this.message = new String(buf, StandardCharsets.UTF_8);
                }
            } else {
                message = this.message = UNKNOWN.message();
            }
        }
        return message;
    }

    @Link
    @Critical(heap = true)
    private static native Errno strerror_s(byte[] buf, @As(uintptr) long buflen, @As(stdc_int) int errNum);

    @Link
    @Critical(heap = true)
    private static native Errno strerror_r(@As(stdc_int) int errNum, byte[] buf, @As(uintptr) long buflen);

    @Link
    @Critical(heap = true)
    private static native Errno __xpg_strerror_r(@As(stdc_int) int errNum, byte[] buf,
            @As(uintptr) long buflen);
}
