package io.smallrye.ffm;

/**
 * A consumer for the last Windows error code.
 */
public interface LastErrorConsumer {
    void accept(int lastError);
}
