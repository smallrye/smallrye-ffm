package io.smallrye.ffm;

/**
 * A consumer for the last Winsock (Windows socket stack) error code.
 */
public interface WSALastErrorConsumer {
    void accept(int lastError);
}
