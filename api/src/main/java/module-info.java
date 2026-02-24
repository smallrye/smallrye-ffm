import io.smallrye.common.annotation.NativeAccess;

@NativeAccess
module io.smallrye.ffm {
    requires static io.smallrye.common.annotation;
    requires io.smallrye.common.constraint;
    requires io.smallrye.common.cpu;
    requires io.smallrye.common.os;

    exports io.smallrye.ffm;
}
