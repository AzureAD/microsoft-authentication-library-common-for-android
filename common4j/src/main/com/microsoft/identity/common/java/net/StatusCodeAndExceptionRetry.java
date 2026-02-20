// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.net;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.ported.Function;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

/**
 * A retry policy that implements exponential backoff based around functions that operate on the
 * HttpResponse object and any Exception that might be thrown from that method.  By default, without
 * any setup, this class will not retry at all - any response is acceptable, and no exceptions are
 * retryable.
 */
@AllArgsConstructor
@Builder
@ThreadSafe
@Immutable
public class StatusCodeAndExceptionRetry implements IRetryPolicy<HttpResponse> {
    @Builder.Default
    private final Function<Exception, Boolean> isRetryableException = new Function<Exception, Boolean>() {
        @Override
        public Boolean apply(Exception input) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private final BiFunction<HttpResponse, Integer, Boolean> isRetryable = new BiFunction<HttpResponse, Integer, Boolean>() {
        @Override
        public Boolean apply(HttpResponse input, Integer attemptNumber) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private final Function<HttpResponse, Boolean> isAcceptable = new Function<HttpResponse, Boolean>() {

        public Boolean apply(HttpResponse input) {
            return Boolean.TRUE;
        }
    };
    @Builder.Default
    private final int number = 1;
    @Builder.Default
    private final int initialDelay = 1000;
    @Builder.Default
    private final int extensionFactor = 2;

    @Override
    public HttpResponse attempt(Callable<HttpResponse> supplier) throws ClientException {
        int attemptNumber = number;
        int cumulativeDelay = initialDelay;
        do {
            try {
                HttpResponse response = supplier.call();
                //If there are no retries left, or the response is acceptable, or it is not retryable.
                if (attemptNumber <= 0 || isAcceptable.apply(response) || !isRetryable.apply(response, attemptNumber)) {
                    return response;
                }
            } catch (final Exception e) {
                if (attemptNumber <= 0 || !isRetryableException.apply(e)) {
                    if (e instanceof ClientException) {
                        throw (ClientException) e;
                    }
                    else {
                        throw new RetryFailedException(e);
                    }
                }
            }
        } while (attemptNumber-- > 0 && waited(cumulativeDelay) && (cumulativeDelay *= extensionFactor) > 0);
        throw new IllegalStateException("This code should not be reachable");
    }

    /**
     * Just a sleep function that allows for a return to break the loop.
     * @param cumulativeDelay How long, in milliseconds, to pause.
     * @return true if we successfully waited, false if interrupted.
     */
    private boolean waited(int cumulativeDelay) {
        try {
            Thread.sleep(cumulativeDelay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Creates a retry policy that retries on IO exceptions only.
     * <p>
     * This policy retries ONLY on {@link ClientException#IO_ERROR} exceptions
     * (excluding {@link SocketTimeoutException}).
     * <p>
     * The {@code numberOfRetries} parameter controls how many retry attempts will be made
     * after the initial attempt fails. For example:
     * <ul>
     *     <li>{@code numberOfRetries = 0}: 1 total attempt (no retries)</li>
     *     <li>{@code numberOfRetries = 1}: 2 total attempts (1 original + 1 retry)</li>
     *     <li>{@code numberOfRetries = 2}: 3 total attempts (1 original + 2 retries)</li>
     * </ul>
     * <p>
     * Use this for scenarios where you want IO-error-specific retry logic.
     * For the library's standard retry behavior, use {@link UrlConnectionHttpClient#getDefaultInstance()}.
     *
     * @param tag The logging tag for tracing retry attempts
     * @param numberOfRetries The number of retry attempts after the initial attempt (must be non-negative)
     * @return A configured {@link StatusCodeAndExceptionRetry} instance for IO errors only
     * @throws IllegalArgumentException if numberOfRetries is negative
     */
    public static StatusCodeAndExceptionRetry getIOExceptionRetryPolicy(
            @NonNull final String tag,
            final int numberOfRetries,
            @Nullable String attributeNameForRetry) {
        if (numberOfRetries < 0) {
            throw new IllegalArgumentException("numberOfRetries must be non-negative, got: " + numberOfRetries);
        }
        return StatusCodeAndExceptionRetry.builder()
                .number(numberOfRetries)
                .isRetryableException(e -> {
                    if (attributeNameForRetry != null) {
                        SpanExtension.current().setAttribute(
                                attributeNameForRetry,
                                numberOfRetries
                        );
                    }
                    if (e instanceof ClientException
                            && ((ClientException) e).getErrorCode().equals(ClientException.IO_ERROR)
                            && !(e.getCause() instanceof SocketTimeoutException)) {
                        Logger.info(tag + ":getIOExceptionRetryPolicy", "Retrying due to exception: " + e);
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                })
                .build();
    }
}
