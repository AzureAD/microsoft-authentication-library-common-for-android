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
package com.microsoft.identity.common.java.opentelemetry;

import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_algorithm;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_controller;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_exception_stack_trace;

import com.microsoft.identity.common.java.crypto.ICryptoFactory;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.StringUtil;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

public class CryptoFactoryTelemetryHelper {

    private static final String TAG = CryptoFactoryTelemetryHelper.class.getSimpleName();

    private static final Tracer sTracer = GlobalOpenTelemetry.getTracer(TAG);

    private static final String SPAN_PREFIX = "CryptoFactory_";

    /**
     * A helper class that consolidate all the telemetry emitting work
     * for crypto operation in one place.
     *
     * @param cryptoOperation name of the crypto operation.
     * @param algorithmName name of the algorithm.
     * @param cryptoFactory an {@link ICryptoFactory} object.
     * @param cryptoOperation a callback that wraps around the crypto operation to be performed.
     * @return result of the crypto operation.
     * */
    public static <T> T performCryptoTaskAndUploadTelemetry(@NonNull final CryptoFactoryOperationName operationName,
                                                            @NonNull final String algorithmName,
                                                            @NonNull final ICryptoFactory cryptoFactory,
                                                            @NonNull final ICryptoOperationCallback<T> cryptoOperation)
            throws ClientException {
        final Span span = sTracer.spanBuilder(getSpanName(operationName)).startSpan();
        try (final Scope scope = span.makeCurrent()) {
            span.setAttribute(crypto_controller.name(), cryptoFactory.getClass().getSimpleName());
            span.setAttribute(crypto_algorithm.name(), algorithmName);
            span.setStatus(StatusCode.OK);
            return cryptoOperation.perform();
        } catch (final Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            span.setAttribute(crypto_exception_stack_trace.name(),
                    StringUtil.getStackTraceAsString(e));
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Constructs the span name from {@link CryptoFactoryOperationName}.
     */
    private static String getSpanName(@NonNull final CryptoFactoryOperationName operationName){
        return SPAN_PREFIX + operationName.name();
    }
}

