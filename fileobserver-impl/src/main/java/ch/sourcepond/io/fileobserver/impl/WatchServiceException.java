package ch.sourcepond.io.fileobserver.impl;

/**
 * Created by rolandhauser on 30.01.17.
 */
final class WatchServiceException extends RuntimeException {

    public WatchServiceException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
