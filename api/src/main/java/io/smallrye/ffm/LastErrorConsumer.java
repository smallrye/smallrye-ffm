package io.smallrye.ffm;

/**
 * A consumer for the last Windows error code.
 */
public interface LastErrorConsumer {
    /**
     * Accept the last-error code.
     *
     * @param lastError the last-error code
     */
    void accept(int lastError);
}
