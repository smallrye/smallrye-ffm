package io.smallrye.ffm;

/**
 * A consumer for {@code errno} which is optionally returned by a method call.
 */
public interface ErrnoConsumer {
    /**
     * Accept the current value of {@code errno}.
     *
     * @param errno the error value (not {@code null})
     */
    void accept(Errno errno);
}
