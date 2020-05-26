package com.microsoft.identity.common.internal.net;

/**
 * An exception indicating that a retry policy has intercepted an exception that lies outside
 * the scope of exceptions that it can handle.
 */
class RetryFailedException extends RuntimeException {
    public RetryFailedException(Exception e) {
        super(e);
    }
}
