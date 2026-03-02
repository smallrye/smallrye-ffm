package io.smallrye.ffm;

/**
 * A consumer for the last Winsock (Windows socket stack) error code.
 */
public interface WSALastErrorConsumer {
    /**
     * Accept the Winsock last-error code.
     *
     * @param lastError the last-error code
     */
    void accept(int lastError);
}
