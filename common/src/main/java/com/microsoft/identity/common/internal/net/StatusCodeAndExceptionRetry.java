package com.microsoft.identity.common.internal.net;

import androidx.arch.core.util.Function;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class StatusCodeAndExceptionRetry implements RetryPolicy<HttpResponse> {
    private Function<Exception, Boolean> isRetryableException;
    private Function<HttpResponse, Boolean> isRetryable;
    private Function<HttpResponse, Boolean> isAcceptable;
    private int number = 1;
    private int initialDelay = 1000;
    private int extensionFactor;
    @Override
    public HttpResponse attempt(Callable<HttpResponse> responseSupplier) throws IOException {
        int attemptNumber = number;
        int cumulativeDelay = initialDelay;
        do {
            try {
                HttpResponse response = responseSupplier.call();
                if (attemptNumber > 0 && (isAcceptable.apply(response) || !isRetryable.apply(response))) {
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
