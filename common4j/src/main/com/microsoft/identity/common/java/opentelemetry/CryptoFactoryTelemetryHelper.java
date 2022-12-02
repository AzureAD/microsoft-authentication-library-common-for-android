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

import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_controller;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_exception_stack_trace;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.crypto_operation;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.error_code;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.error_type;
import static com.microsoft.identity.common.java.opentelemetry.AttributeName.parent_span_name;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.microsoft.identity.common.java.crypto.ICryptoFactory;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.IErrorInformation;
import com.microsoft.identity.common.java.util.ThrowableUtil;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import lombok.NonNull;

public class CryptoFactoryTelemetryHelper {

    private static final LongCounter sFailedCryptoOperationCount = OTelUtility.createLongCounter(
            "failed_crypto_operation_count",
            "Number of failed crypto operations"
    );

    /**
     * A helper class that consolidate all the telemetry emitting work
     * for crypto operation in one place.
     *
     * @param cryptoOperation name of the crypto operation.
     * @param algorithmName   name of the algorithm.
     * @param cryptoFactory   an {@link ICryptoFactory} object.
     * @param cryptoOperation a callback that wraps around the crypto operation to be performed.
     * @return result of the crypto operation.
     */
    public static <T> T performCryptoOperationAndUploadTelemetry(@NonNull final CryptoObjectName operationName,
                                                                 @NonNull final String algorithmName,
                                                                 @NonNull final ICryptoFactory cryptoFactory,
                                                                 @NonNull final ICryptoOperation<T> cryptoOperation)
            throws ClientException {
        try {
            return cryptoOperation.perform();
        } catch (final Exception e) {
            final String parentSpanName = OTelUtility.getCurrentSpanName();
            final Attributes attributes = Attributes.of(
                    stringKey(crypto_controller.name()), cryptoFactory.getTelemetryClassName().name(),
                    stringKey(crypto_operation.name()), getCryptoOperationEventName(operationName, algorithmName),
                    stringKey(parent_span_name.name()), parentSpanName == null ? "N/A" : parentSpanName,
                    stringKey(error_type.name()), e.getClass().getSimpleName(),
                    stringKey(error_code.name()), e instanceof IErrorInformation ? ((IErrorInformation) e).getErrorCode() : "N/A",
                    stringKey(crypto_exception_stack_trace.name()), ThrowableUtil.getStackTraceAsString(e)
            );
            sFailedCryptoOperationCount.add(1, attributes);
            throw e;
        }
    }

    /**
     * Constructs the telemetry name for {@link AttributeName#crypto_operation}
     */
    private static String getCryptoOperationEventName(@NonNull final CryptoObjectName operationName,
                                                      @NonNull final String algorithm) {
        return operationName.name() + "_" + algorithm;
    }
}

