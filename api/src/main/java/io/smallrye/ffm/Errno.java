package io.smallrye.ffm;

import static io.smallrye.ffm.AsType.*;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import io.smallrye.common.cpu.CPU;
import io.smallrye.common.os.OS;

/**
 * Errno values.
 */
// TODO: MIPS Linux has a whole separate set of errno values!
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
    /**
     * The {@code errno} constant for {@code E2BIG}.
     */
    E2BIG(OS.Z, 145, 7),
    /**
     * The {@code errno} constant for {@code EACCES}.
     */
    EACCES(OS.Z, 111, 13),
    /**
     * The {@code errno} constant for {@code EADDRINUSE}.
     */
    EADDRINUSE(67, 125, 48, 98, 100, 1115),
    /**
     * The {@code errno} constant for {@code EADDRNOTAVAIL}.
     */
    EADDRNOTAVAIL(68, 126, 49, 99, 101, 1116),
    /**
     * The {@code errno} constant for {@code EADV}.
     */
    EADV(-1, -1, -1, 68, -1, 1145),
    /**
     * The {@code errno} constant for {@code EAFNOSUPPORT}.
     */
    EAFNOSUPPORT(66, 124, 47, 97, 102, 1114),
    /**
     * The {@code errno} constant for {@code EAGAIN}.
     * This is the same as {@link #EWOULDBLOCK} on some platforms.
     */
    EAGAIN(11, 11, 35, 11, 11, 112),
    /**
     * The {@code errno} constant for {@code EALREADY}.
     */
    EALREADY(56, 42, 37, 114, 103, 1104),
    /**
     * The {@code errno} constant for {@code EAUTH}.
     */
    EAUTH(OS.MAC, 80),
    /**
     * The {@code errno} constant for {@code EBADARCH}.
     */
    EBADARCH(OS.MAC, 86),
    /**
     * The {@code errno} constant for {@code EBADE}.
     */
    EBADE(OS.LINUX, 52),
    /**
     * The {@code errno} constant for {@code EBADEXEC}.
     */
    EBADEXEC(OS.MAC, 85),
    /**
     * The {@code errno} constant for {@code EBADF}.
     */
    EBADF(OS.Z, 113, 9),
    /**
     * The {@code errno} constant for {@code EBADFD}.
     */
    EBADFD(OS.LINUX, 77),
    /**
     * The {@code errno} constant for {@code EBADMACHO}.
     */
    EBADMACHO(OS.MAC, 88),
    /**
     * The {@code errno} constant for {@code EBADMSG}.
     */
    EBADMSG(120, 77, 94, 74, 104, 1140),
    /**
     * The {@code errno} constant for {@code EBADR}.
     */
    EBADR(OS.LINUX, 53),
    /**
     * The {@code errno} constant for {@code EBADRPC}.
     */
    EBADRPC(OS.MAC, 72),
    /**
     * The {@code errno} constant for {@code EBADRQC}.
     */
    EBADRQC(OS.LINUX, 56),
    /**
     * The {@code errno} constant for {@code EBADSLT}.
     */
    EBADSLT(OS.LINUX, 57),
    /**
     * The {@code errno} constant for {@code EBFONT}.
     */
    EBFONT(OS.LINUX, 59),
    /**
     * The {@code errno} constant for {@code EBUSY}.
     */
    EBUSY(OS.Z, 114, 16),
    /**
     * The {@code errno} constant for {@code ECANCELED}.
     */
    ECANCELED(117, 47, 89, 125, 105, 1152),
    /**
     * The {@code errno} constant for {@code ECHILD}.
     */
    ECHILD(OS.Z, 115, 10),
    /**
     * The {@code errno} constant for {@code ECHRNG}.
     */
    ECHRNG(OS.LINUX, 44),
    /**
     * The {@code errno} constant for {@code ECOMM}.
     */
    ECOMM(-1, -1, -1, 70, -1, 1147),
    /**
     * The {@code errno} constant for {@code ECONNABORTED}.
     */
    ECONNABORTED(72, 130, 53, 103, 106, 1120),
    /**
     * The {@code errno} constant for {@code ECONNREFUSED}.
     */
    ECONNREFUSED(79, 146, 61, 111, 107, 1128),
    /**
     * The {@code errno} constant for {@code ECONNRESET}.
     */
    ECONNRESET(73, 131, 54, 104, 108, 1121),
    /**
     * The {@code errno} constant for {@code EDEADLK}.
     */
    EDEADLK(45, 45, 11, 35, 36, 116),
    /**
     * The {@code errno} constant for {@code EDEADLOCK}.
     */
    EDEADLOCK(45, 45, 11, switch (CPU.host()) {
        case ppc, ppc32 -> 58;
        default -> 35;
    }, 36, 116),
    /**
     * The {@code errno} constant for {@code EDESTADDRREQ}.
     */
    EDESTADDRREQ(58, 96, 39, 89, 109, 1106),
    /**
     * The {@code errno} constant for {@code EDEVERR}.
     */
    EDEVERR(OS.MAC, 83),
    /**
     * The {@code errno} constant for {@code EDOM}.
     */
    EDOM(OS.Z, 1, 33),
    /**
     * The {@code errno} constant for {@code EDOTDOT}.
     */
    EDOTDOT(-1, -1, -1, 73, -1, 1150),
    /**
     * The {@code errno} constant for {@code EDQUOT}.
     */
    EDQUOT(-1, -1, -1, 122, -1, 1133),
    /**
     * The {@code errno} constant for {@code EEXIST}.
     */
    EEXIST(OS.Z, 117, 17),
    /**
     * The {@code errno} constant for {@code EFAULT}.
     */
    EFAULT(OS.Z, 118, 14),
    /**
     * The {@code errno} constant for {@code EFBIG}.
     */
    EFBIG(OS.Z, 119, 27),
    /**
     * The {@code errno} constant for {@code EFTYPE}.
     */
    EFTYPE(OS.MAC, 79),
    /**
     * The {@code errno} constant for {@code EHOSTDOWN}.
     */
    EHOSTDOWN(-1, -1, 64, 112, -1, 1129),
    /**
     * The {@code errno} constant for {@code EHOSTUNREACH}.
     */
    EHOSTUNREACH(81, 148, 65, 113, 110, 1130),
    /**
     * The {@code errno} constant for {@code EHWPOISON}.
     */
    EHWPOISON(OS.LINUX, 133),
    /**
     * The {@code errno} constant for {@code EIBMBADTCPNAME}.
     */
    EIBMBADTCPNAME(OS.Z, 1011),
    /**
     * The {@code errno} constant for {@code EIBMCANCELLED}.
     */
    EIBMCANCELLED(OS.Z, 1009),
    /**
     * The {@code errno} constant for {@code EIBMCONFLICT}.
     */
    EIBMCONFLICT(OS.Z, 1008),
    /**
     * The {@code errno} constant for {@code EIBMSOCKINUSE}.
     */
    EIBMSOCKINUSE(OS.Z, 1003),
    /**
     * The {@code errno} constant for {@code EIBMSOCKOUTOFRANGE}.
     */
    EIBMSOCKOUTOFRANGE(OS.Z, 1002),
    /**
     * The {@code errno} constant for {@code EIDRM}.
     */
    EIDRM(36, 36, 90, 43, 111, 1141),
    /**
     * The {@code errno} constant for {@code EILSEQ}.
     */
    EILSEQ(116, 88, 92, 84, 42, 147),
    /**
     * The {@code errno} constant for {@code EINPROGRESS}.
     */
    EINPROGRESS(55, 150, 36, 115, 112, 1103),
    /**
     * The {@code errno} constant for {@code EINTR}.
     */
    EINTR(OS.Z, 120, 4),
    /**
     * The {@code errno} constant for {@code EINTRNODATA}.
     */
    EINTRNODATA(OS.Z, 1159),
    /**
     * The {@code errno} constant for {@code EINVAL}.
     */
    EINVAL(OS.Z, 121, 22),
    /**
     * The {@code errno} constant for {@code EIO}.
     */
    EIO(OS.Z, 122, 5),
    /**
     * The {@code errno} constant for {@code EISCONN}.
     */
    EISCONN(75, 133, 56, 106, 113, 1123),
    /**
     * The {@code errno} constant for {@code EISDIR}.
     */
    EISDIR(OS.Z, 123, 21),
    /**
     * The {@code errno} constant for {@code EISNAM}.
     */
    EISNAM(OS.LINUX, 120),
    /**
     * The {@code errno} constant for {@code EKEYEXPIRED}.
     */
    EKEYEXPIRED(OS.LINUX, 127),
    /**
     * The {@code errno} constant for {@code EKEYREJECTED}.
     */
    EKEYREJECTED(OS.LINUX, 129),
    /**
     * The {@code errno} constant for {@code EKEYREVOKED}.
     */
    EKEYREVOKED(OS.LINUX, 128),
    /**
     * The {@code errno} constant for {@code EL2HLT}.
     */
    EL2HLT(OS.LINUX, 51),
    /**
     * The {@code errno} constant for {@code EL2NSYNC}.
     */
    EL2NSYNC(OS.LINUX, 45),
    /**
     * The {@code errno} constant for {@code EL3HLT}.
     */
    EL3HLT(OS.LINUX, 46),
    /**
     * The {@code errno} constant for {@code EL3RST}.
     */
    EL3RST(OS.LINUX, 47),
    /**
     * The {@code errno} constant for {@code ELIBACC}.
     */
    ELIBACC(OS.LINUX, 79),
    /**
     * The {@code errno} constant for {@code ELIBBAD}.
     */
    ELIBBAD(OS.LINUX, 80),
    /**
     * The {@code errno} constant for {@code ELIBEXEC}.
     */
    ELIBEXEC(OS.LINUX, 83),
    /**
     * The {@code errno} constant for {@code ELIBMAX}.
     */
    ELIBMAX(OS.LINUX, 82),
    /**
     * The {@code errno} constant for {@code ELIBSCN}.
     */
    ELIBSCN(OS.LINUX, 81),
    /**
     * The {@code errno} constant for {@code ELNRNG}.
     */
    ELNRNG(OS.LINUX, 48),
    /**
     * The {@code errno} constant for {@code ELOOP}.
     */
    ELOOP(85, 90, 62, 40, 114, 146),
    /**
     * The {@code errno} constant for {@code EMEDIUMTYPE}.
     */
    EMEDIUMTYPE(OS.LINUX, 124),
    /**
     * The {@code errno} constant for {@code EMFILE}.
     */
    EMFILE(OS.Z, 124, 24),
    /**
     * The {@code errno} constant for {@code EMLINK}.
     */
    EMLINK(OS.Z, 125, 31),
    /**
     * The {@code errno} constant for {@code EMSGSIZE}.
     */
    EMSGSIZE(59, 97, 40, 90, 115, 1107),
    /**
     * The {@code errno} constant for {@code EMULTIHOP}.
     */
    EMULTIHOP(-1, -1, 95, 72, -1, 1149),
    /**
     * The {@code errno} constant for {@code EMVSARMERROR}.
     */
    EMVSARMERROR(OS.Z, 172),
    /**
     * The {@code errno} constant for {@code EMVSCATLG}.
     */
    EMVSCATLG(OS.Z, 153),
    /**
     * The {@code errno} constant for {@code EMVSCPLERROR}.
     */
    EMVSCPLERROR(OS.Z, 171),
    /**
     * The {@code errno} constant for {@code EMVSCVAF}.
     */
    EMVSCVAF(OS.Z, 152),
    /**
     * The {@code errno} constant for {@code EMVSDYNALC}.
     */
    EMVSDYNALC(OS.Z, 151),
    /**
     * The {@code errno} constant for {@code EMVSERR}.
     */
    EMVSERR(OS.Z, 157),
    /**
     * The {@code errno} constant for {@code EMVSEXPIRE}.
     */
    EMVSEXPIRE(OS.Z, 168),
    /**
     * The {@code errno} constant for {@code EMVSINITIAL}.
     */
    EMVSINITIAL(OS.Z, 156),
    /**
     * The {@code errno} constant for {@code EMVSNORTL}.
     */
    EMVSNORTL(OS.Z, 167),
    /**
     * The {@code errno} constant for {@code EMVSNOTUP}.
     */
    EMVSNOTUP(OS.Z, 150),
    /**
     * The {@code errno} constant for {@code EMVSPARM}.
     */
    EMVSPARM(OS.Z, 158),
    /**
     * The {@code errno} constant for {@code EMVSPASSWORD}.
     */
    EMVSPASSWORD(OS.Z, 169),
    /**
     * The {@code errno} constant for {@code EMVSPFSFILE}.
     */
    EMVSPFSFILE(OS.Z, 159),
    /**
     * The {@code errno} constant for {@code EMVSPFSPERM}.
     */
    EMVSPFSPERM(OS.Z, 162),
    /**
     * The {@code errno} constant for {@code EMVSSAF2ERR}.
     */
    EMVSSAF2ERR(OS.Z, 164),
    /**
     * The {@code errno} constant for {@code EMVSSAFEXTRERR}.
     */
    EMVSSAFEXTRERR(OS.Z, 163),
    /**
     * The {@code errno} constant for {@code EMVSWLMERROR}.
     */
    EMVSWLMERROR(OS.Z, 170),
    /**
     * The {@code errno} constant for {@code ENAMETOOLONG}.
     */
    ENAMETOOLONG(86, 78, 63, 36, 38, 126),
    /**
     * The {@code errno} constant for {@code ENAVAIL}.
     */
    ENAVAIL(OS.LINUX, 119),
    /**
     * The {@code errno} constant for {@code ENEEDAUTH}.
     */
    ENEEDAUTH(OS.MAC, 81),
    /**
     * The {@code errno} constant for {@code ENETDOWN}.
     */
    ENETDOWN(69, 127, 50, 100, 116, 1117),
    /**
     * The {@code errno} constant for {@code ENETRESET}.
     */
    ENETRESET(71, 129, 52, 102, 117, 1119),
    /**
     * The {@code errno} constant for {@code ENETUNREACH}.
     */
    ENETUNREACH(70, 128, 51, 101, 118, 1118),
    /**
     * The {@code errno} constant for {@code ENFILE}.
     */
    ENFILE(OS.Z, 127, 23),
    /**
     * The {@code errno} constant for {@code ENOANO}.
     */
    ENOANO(OS.LINUX, 55),
    /**
     * The {@code errno} constant for {@code ENOATTR}.
     */
    ENOATTR(OS.MAC, 93),
    /**
     * The {@code errno} constant for {@code ENOBUFS}.
     */
    ENOBUFS(74, 132, 55, 105, 119, 1122),
    /**
     * The {@code errno} constant for {@code ENOCSI}.
     */
    ENOCSI(OS.LINUX, 50),
    /**
     * The {@code errno} constant for {@code ENODATA}.
     */
    ENODATA(122, 61, 96, 61, 120),
    /**
     * The {@code errno} constant for {@code ENODEV}.
     */
    ENODEV(OS.Z, 128, 19),
    /**
     * The {@code errno} constant for {@code ENOENT}.
     */
    ENOENT(OS.Z, 129, 2),
    /**
     * The {@code errno} constant for {@code ENOEXEC}.
     */
    ENOEXEC(OS.Z, 130, 8),
    /**
     * The {@code errno} constant for {@code ENOKEY}.
     */
    ENOKEY(OS.LINUX, 126),
    /**
     * The {@code errno} constant for {@code ENOLCK}.
     */
    ENOLCK(49, 46, 77, 37, 39, 131),
    /**
     * The {@code errno} constant for {@code ENOLINK}.
     */
    ENOLINK(126, 67, 97, 67, 121, 1144),
    /**
     * The {@code errno} constant for {@code ENOMEDIUM}.
     */
    ENOMEDIUM(OS.LINUX, 123),
    /**
     * The {@code errno} constant for {@code ENOMEM}.
     */
    ENOMEM(OS.Z, 132, 12),
    /**
     * The {@code errno} constant for {@code ENOMOVE}.
     */
    ENOMOVE(OS.Z, 1161),
    /**
     * The {@code errno} constant for {@code ENOMSG}.
     */
    ENOMSG(35, 35, 91, 42, 122, 1139),
    /**
     * The {@code errno} constant for {@code ENONET}.
     */
    ENONET(-1, -1, -1, 64, -1, 1142),
    /**
     * The {@code errno} constant for {@code ENOPKG}.
     */
    ENOPKG(OS.LINUX, 65),
    /**
     * The {@code errno} constant for {@code ENOPOLICY}.
     */
    ENOPOLICY(OS.MAC, 103),
    /**
     * The {@code errno} constant for {@code ENOPROTOOPT}.
     */
    ENOPROTOOPT(61, 99, 42, 92, 123, 1109),
    /**
     * The {@code errno} constant for {@code ENOREUSE}.
     */
    ENOREUSE(OS.Z, 1160),
    /**
     * The {@code errno} constant for {@code ENOSPC}.
     */
    ENOSPC(OS.Z, 133, 28),
    /**
     * The {@code errno} constant for {@code ENOSR}.
     */
    ENOSR(118, 63, 98, 63, 124, 1138),
    /**
     * The {@code errno} constant for {@code ENOSTR}.
     */
    ENOSTR(123, 60, 99, 60, 125, 1136),
    /**
     * The {@code errno} constant for {@code ENOSYS}.
     */
    ENOSYS(109, 89, 78, 38, 40, 134),
    /**
     * The {@code errno} constant for {@code ENOTBLK}.
     */
    ENOTBLK(OS.Z, 1100, 15),
    /**
     * The {@code errno} constant for {@code ENOTCONN}.
     */
    ENOTCONN(76, 134, 57, 107, 126, 1124),
    /**
     * The {@code errno} constant for {@code ENOTDIR}.
     */
    ENOTDIR(OS.Z, 135, 20),
    /**
     * The {@code errno} constant for {@code ENOTEMPTY}.
     */
    ENOTEMPTY(87, 93, 66, 39, 41, 136),
    /**
     * The {@code errno} constant for {@code ENOTNAM}.
     */
    ENOTNAM(OS.LINUX, 118),
    /**
     * The {@code errno} constant for {@code ENOTRECOVERABLE}.
     */
    ENOTRECOVERABLE(-1, 59, 104, 131, 127),
    /**
     * The {@code errno} constant for {@code ENOTSOCK}.
     */
    ENOTSOCK(57, 95, 38, 88, 128, 1105),
    /**
     * The {@code errno} constant for {@code ENOTSUP}.
     */
    ENOTSUP(124, 48, 45, 95, 129, 247),
    /**
     * The {@code errno} constant for {@code ENOTTY}.
     */
    ENOTTY(OS.Z, 137, 25),
    /**
     * The {@code errno} constant for {@code ENOTUNIQ}.
     */
    ENOTUNIQ(OS.LINUX, 76),
    /**
     * The {@code errno} constant for {@code ENXIO}.
     */
    ENXIO(OS.Z, 138, 6),
    /**
     * The {@code errno} constant for {@code EOFFLOADboxDOWN}.
     */
    EOFFLOADboxDOWN(OS.Z, 1007),
    /**
     * The {@code errno} constant for {@code EOFFLOADboxERROR}.
     */
    EOFFLOADboxERROR(OS.Z, 1005),
    /**
     * The {@code errno} constant for {@code EOFFLOADboxRESTART}.
     */
    EOFFLOADboxRESTART(OS.Z, 1006),
    /**
     * The {@code errno} constant for {@code EOPNOTSUPP}.
     */
    EOPNOTSUPP(64, 122, 102, 95, 130, 1112),
    /**
     * The {@code errno} constant for {@code EOTHER}.
     */
    EOTHER(OS.WINDOWS, 131),
    /**
     * The {@code errno} constant for {@code EOVERFLOW}.
     */
    EOVERFLOW(127, 79, 84, 75, 132, 149),
    /**
     * The {@code errno} constant for {@code EOWNERDEAD}.
     */
    EOWNERDEAD(-1, 58, 105, 130, 133),
    /**
     * The {@code errno} constant for {@code EPERM}.
     */
    EPERM(OS.Z, 139, 1),
    /**
     * The {@code errno} constant for {@code EPFNOSUPPORT}.
     */
    EPFNOSUPPORT(-1, -1, 46, 96, -1, 1113),
    /**
     * The {@code errno} constant for {@code EPIPE}.
     */
    EPIPE(OS.Z, 140, 32),
    /**
     * The {@code errno} constant for {@code EPROCLIM}.
     */
    EPROCLIM(-1, -1, 67, -1, -1, 1131),
    /**
     * The {@code errno} constant for {@code EPROCUNAVAIL}.
     */
    EPROCUNAVAIL(OS.MAC, 76),
    /**
     * The {@code errno} constant for {@code EPROGMISMATCH}.
     */
    EPROGMISMATCH(OS.MAC, 75),
    /**
     * The {@code errno} constant for {@code EPROGUNAVAIL}.
     */
    EPROGUNAVAIL(OS.MAC, 74),
    /**
     * The {@code errno} constant for {@code EPROTO}.
     */
    EPROTO(121, 71, 100, 71, 134, 1148),
    /**
     * The {@code errno} constant for {@code EPROTONOSUPPORT}.
     */
    EPROTONOSUPPORT(62, 120, 43, 93, 135, 1110),
    /**
     * The {@code errno} constant for {@code EPROTOTYPE}.
     */
    EPROTOTYPE(69, 98, 41, 91, 136, 1108),
    /**
     * The {@code errno} constant for {@code EPWROFF}.
     */
    EPWROFF(OS.MAC, 82),
    /**
     * The {@code errno} constant for {@code EQFULL}.
     */
    EQFULL(OS.MAC, 106),
    /**
     * The {@code errno} constant for {@code ERANGE}.
     */
    ERANGE(OS.Z, 2, 34),
    /**
     * The {@code errno} constant for {@code EREMCHG}.
     */
    EREMCHG(-1, -1, -1, 78, -1, 1151),
    /**
     * The {@code errno} constant for {@code EREMOTE}.
     */
    EREMOTE(-1, -1, 71, 66, -1, 1135),
    /**
     * The {@code errno} constant for {@code EREMOTEIO}.
     */
    EREMOTEIO(OS.LINUX, 121),
    /**
     * The {@code errno} constant for {@code ERESTART}.
     */
    ERESTART(OS.LINUX, 85),
    /**
     * The {@code errno} constant for {@code ERFKILL}.
     */
    ERFKILL(OS.LINUX, 132),
    /**
     * The {@code errno} constant for {@code EROFS}.
     */
    EROFS(OS.Z, 141, 30),
    /**
     * The {@code errno} constant for {@code ERPCMISMATCH}.
     */
    ERPCMISMATCH(OS.MAC, 73),
    /**
     * The {@code errno} constant for {@code ERREMOTE}.
     */
    ERREMOTE(OS.Z, 1143),
    /**
     * The {@code errno} constant for {@code ESHLIBVERS}.
     */
    ESHLIBVERS(OS.MAC, 87),
    /**
     * The {@code errno} constant for {@code ESHUTDOWN}.
     */
    ESHUTDOWN(-1, -1, 58, 108, -1, 1125),
    /**
     * The {@code errno} constant for {@code ESOCKTNOSUPPORT}.
     */
    ESOCKTNOSUPPORT(-1, -1, 44, 94, -1, 1111),
    /**
     * The {@code errno} constant for {@code ESPIPE}.
     */
    ESPIPE(OS.Z, 142, 29),
    /**
     * The {@code errno} constant for {@code ESRCH}.
     */
    ESRCH(OS.Z, 143, 3),
    /**
     * The {@code errno} constant for {@code ESRMNT}.
     */
    ESRMNT(-1, -1, -1, 69, -1, 1146),
    /**
     * The {@code errno} constant for {@code ESTALE}.
     */
    ESTALE(52, 151, 70, 116, -1, 1134),
    /**
     * The {@code errno} constant for {@code ESTRPIPE}.
     */
    ESTRPIPE(OS.LINUX, 86),
    /**
     * The {@code errno} constant for {@code ETIME}.
     */
    ETIME(119, 62, 101, 62, 137, 1137),
    /**
     * The {@code errno} constant for {@code ETIMEDOUT}.
     */
    ETIMEDOUT(78, 145, 60, 110, 138, 1127),
    /**
     * The {@code errno} constant for {@code ETOOMANYREFS}.
     */
    ETOOMANYREFS(-1, -1, 59, 109, -1, 1126),
    /**
     * The {@code errno} constant for {@code ETXTBSY}.
     */
    ETXTBSY(26, 26, 26, 26, 139, 1101),
    /**
     * The {@code errno} constant for {@code ETcpBadObj}.
     */
    ETcpBadObj(OS.Z, 1155),
    /**
     * The {@code errno} constant for {@code ETcpClosed}.
     */
    ETcpClosed(OS.Z, 1156),
    /**
     * The {@code errno} constant for {@code ETcpErr}.
     */
    ETcpErr(OS.Z, 1158),
    /**
     * The {@code errno} constant for {@code ETcpLinked}.
     */
    ETcpLinked(OS.Z, 1157),
    /**
     * The {@code errno} constant for {@code ETcpOutOfState}.
     */
    ETcpOutOfState(OS.Z, 1153),
    /**
     * The {@code errno} constant for {@code ETcpUnattach}.
     */
    ETcpUnattach(OS.Z, 1154),
    /**
     * The {@code errno} constant for {@code EUCLEAN}.
     */
    EUCLEAN(OS.LINUX, 117),
    /**
     * The {@code errno} constant for {@code EUNATCH}.
     */
    EUNATCH(-1, -1, -1, 49, -1, 3448),
    /**
     * The {@code errno} constant for {@code EUSERS}.
     */
    EUSERS(-1, -1, 68, 87, -1, 1132),
    /**
     * The {@code errno} constant for {@code EWOULDBLOCK}.
     * This is the same as {@link #EAGAIN} on some platforms.
     */
    EWOULDBLOCK(54, 11, 35, 11, 140, 1102),
    /**
     * The {@code errno} constant for {@code EXDEV}.
     */
    EXDEV(OS.Z, 144, 18),
    /**
     * The {@code errno} constant for {@code EXFULL}.
     */
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
