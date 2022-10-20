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

import javax.annotation.Nullable;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.NonNull;

/**
 * Assists classes associated with Certificate Based Authentication (CBA) with
 *  telemetry-related tasks.
 */
public class CertBasedAuthTelemetryHelper {

    /**
     * Creates a new Span and sets it to be the current Span.
     */
    public static void createSpanAndMakeCurrent() {
        final Span span = OTelUtility.createSpan(SpanName.CertBasedAuth.name());
        span.makeCurrent();
    }

    /**
     * Gets the current Span.
     * Should be mainly used to pass the existing current Span to a task that is
     *  running on a different thread.
     * @return current Span if it is related to CBA; null otherwise.
     */
    @Nullable
    public static Span getCurrentSpan() {
        if (isCorrectSpan()) {
            return Span.current();
        }
        return null;
    }

    /**
     * Sets attribute that indicates the ICertBasedAuthChallengeHandler handling the current CBA flow.
     * @param challengeHandlerName name of the ICertBasedAuthChallengeHandler class.
     */
    public static void setCertBasedAuthChallengeHandler(@NonNull final String challengeHandlerName) {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            span.setAttribute(
                    AttributeName.cert_based_auth_challenge_handler.name(),
                    challengeHandlerName);
        }
    }

    /**
     * Sets attribute that indicates if a PivProvider instance is already present in the
     *  Java Security static list upon adding a new instance.
     * @param present true if PivProvider instance present; false otherwise.
     */
    public static void setExistingPivProviderPresent(@NonNull final boolean present) {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            span.setAttribute(
                    AttributeName.cert_based_auth_existing_piv_provider_present.name(),
                    present);
        }
    }

    /**
     * Indicates on the Span that CBA was successful and then ends current Span.
     */
    public static void setResultSuccess() {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            span.setStatus(StatusCode.OK);
            span.end();
        }
    }

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * This method should mainly be used for cases without an exception,
     *  such as user cancellation, no certificates on smartcard, etc.
     * @param message descriptive cause of failure message.
     */
    public static void setResultFailure(@NonNull final String message) {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            //setting the error_message attribute manually since there's no exception to record.
            span.setAttribute(
                    "error_message",
                    message);
            span.setStatus(StatusCode.ERROR);
            span.end();
        }
    }

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    public static void setResultFailure(@NonNull final Exception exception) {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            span.end();
        }
    }

    /**
     * Sets attribute that indicates the user's choice for CBA (smartcard or on-device).
     * This attribute is currently being set to an empty string, but it will be utilized
     *  in future updates to the CBA dialogs.
     * @param choice string indicating user's choice for CBA.
     */
    public static void setUserChoice(@NonNull final String choice) {
        if (isCorrectSpan()) {
            final Span span = Span.current();
            span.setAttribute(
                    AttributeName.cert_based_auth_user_choice.name(),
                    choice);
        }
    }

    /**
     * Checks if the current Span is related to CBA.
     * Used to ensure CBA attributes are not being set for other unrelated Spans.
     * @return true if current Span is CBA related; false otherwise.
     */
    private static boolean isCorrectSpan() {
        return (SpanName.CertBasedAuth.name()
                .equals(OTelUtility.getCurrentSpanName()));
    }
}
