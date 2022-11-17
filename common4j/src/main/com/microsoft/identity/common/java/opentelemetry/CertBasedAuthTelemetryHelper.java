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

    private final Span mSpan;

    public CertBasedAuthTelemetryHelper() {
        mSpan = OTelUtility.createSpan(SpanName.CertBasedAuth.name());
    }

    /**
     * Sets attribute that indicates the ICertBasedAuthChallengeHandler handling the current CBA flow.
     * @param challengeHandlerName name of the ICertBasedAuthChallengeHandler class.
     */
    public void setCertBasedAuthChallengeHandler(@Nullable final String challengeHandlerName) {
        mSpan.setAttribute(
                AttributeName.cert_based_auth_challenge_handler.name(),
                challengeHandlerName);
    }

    /**
     * Sets attribute that indicates if a PivProvider instance is already present in the
     *  Java Security static list upon adding a new instance.
     * @param present true if PivProvider instance present; false otherwise.
     */
    public void setExistingPivProviderPresent(final boolean present) {
        mSpan.setAttribute(
                AttributeName.cert_based_auth_existing_piv_provider_present.name(),
                present);
    }

    /**
     * Indicates on the Span that CBA was successful and then ends current Span.
     */
    public void setResultSuccess() {
        mSpan.setStatus(StatusCode.OK);
        mSpan.end();
    }

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * This method should mainly be used for cases without an exception,
     *  such as user cancellation, no certificates on smartcard, etc.
     * @param message descriptive cause of failure message.
     */
    public void setResultFailure(@NonNull final String message) {
        mSpan.setStatus(StatusCode.ERROR, message);
        mSpan.end();
    }

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    public void setResultFailure(@NonNull final Exception exception) {
        mSpan.recordException(exception);
        mSpan.setStatus(StatusCode.ERROR);
        mSpan.end();
    }

    /**
     * Sets attribute that indicates the user's intended choice for CBA (smartcard or on-device).
     * @param choice string indicating user's intended choice for CBA.
     */
    public void setUserChoice(@Nullable final String choice) {
        mSpan.setAttribute(
                AttributeName.cert_based_auth_user_choice.name(),
                choice);
    }
}