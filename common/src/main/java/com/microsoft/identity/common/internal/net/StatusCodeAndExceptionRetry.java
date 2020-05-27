package com.microsoft.identity.common.internal.net;

import androidx.arch.core.util.Function;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * A retry policy that implements exponential backoff based around functions that operate on the
 * HttpResponse object and any Exception that might be thrown from that method.  By default, without
 * any setup, this class will not retry at all.  Any response is acceptable, and no exceptions are
 * retryable.
 */
@AllArgsConstructor
@Builder
public class StatusCodeAndExceptionRetry implements RetryPolicy<HttpResponse> {
    @Builder.Default
    private Function<Exception, Boolean> isRetryableException = new Function<Exception, Boolean>() {
        @Override
        public Boolean apply(Exception input) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private Function<HttpResponse, Boolean> isRetryable = new Function<HttpResponse, Boolean>() {
        @Override
        public Boolean apply(HttpResponse input) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private Function<HttpResponse, Boolean> isAcceptable = new Function<HttpResponse, Boolean>() {

        public Boolean apply(HttpResponse input) {
            return Boolean.TRUE;
        }
    };
    @Builder.Default
    private int number = 1;
    @Builder.Default
    private int initialDelay = 1000;
    @Builder.Default
    private int extensionFactor = 2;
    @Override
    public HttpResponse attempt(Callable<HttpResponse> responseSupplier) throws IOException {
        int attemptNumber = number;
        int cumulativeDelay = initialDelay;
        do {
            try {
                HttpResponse response = responseSupplier.call();
                //If there are no retries left, or the response is acceptable, or it is not retryable.
                if (attemptNumber <= 0 || isAcceptable.apply(response) || !isRetryable.apply(response)) {
                    return response;
                }
            } catch (final Exception e) {
                if (attemptNumber <= 0 || !isRetryableException.apply(e)) {
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    }
                    else {
                        throw new RetryFailedException(e);
                    }
                }
            }
        } while (attemptNumber-- > 0 && waited(cumulativeDelay) && (cumulativeDelay *= extensionFactor) > 0);
        throw new IllegalStateException("This code should not be reachable");
    }

    private boolean waited(int cumulativeDelay) {
        try {
            Thread.sleep(cumulativeDelay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
