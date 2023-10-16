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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import lombok.NonNull;

/**
 * Assists classes associated with Certificate Based Authentication (CBA) with
 *  telemetry-related tasks.
 */
public class CertBasedAuthTelemetryHelper implements ICertBasedAuthTelemetryHelper{

    private final Span mSpan;

    /**
     * Creates an instance of CertBasedAuthTelemetryHelper.
     * @param spanContext current span context.
     */
    public CertBasedAuthTelemetryHelper(@Nullable final SpanContext spanContext) {
        if (spanContext != null) {
            mSpan = OTelUtility.createSpanFromParent(SpanName.CertBasedAuth.name(), spanContext);
        } else {
            mSpan = OTelUtility.createSpan(SpanName.CertBasedAuth.name());
        }
    }

    /**
     * Sets attribute that indicates the ICertBasedAuthChallengeHandler handling the current CBA flow.
     * @param challengeHandlerName name of the ICertBasedAuthChallengeHandler class.
     */
    public void setCertBasedAuthChallengeHandler(@NonNull final String challengeHandlerName) {
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
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
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
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    public void setResultFailure(@NonNull final String message) {
        mSpan.setStatus(StatusCode.ERROR, message);
        mSpan.end();
    }

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    public void setResultFailure(@NonNull final Exception exception) {
        mSpan.recordException(exception);
        mSpan.setStatus(StatusCode.ERROR);
        mSpan.end();
    }

    /**
     * Sets attribute that indicates the user's intended choice for CBA (smartcard or on-device).
     * @param choice enum indicating user's intended choice for CBA.
     */
    public void setUserChoice(@NonNull final CertBasedAuthChoice choice) {
        switch(choice) {
            case ON_DEVICE_CHOICE:
                mSpan.setAttribute(
                        AttributeName.cert_based_auth_user_choice.name(),
                        "on-device");
                break;
            case SMARTCARD_CHOICE:
                mSpan.setAttribute(
                        AttributeName.cert_based_auth_user_choice.name(),
                        "smartcard");
                break;
            default:
                mSpan.setAttribute(
                        AttributeName.cert_based_auth_user_choice.name(),
                        "N/A");
        }
    }

    /**
     * Sets attribute that indicates the selected certificate's public key algorithm type.
     * @param type algorithm name as a string.
     */
    @Override
    public void setPublicKeyAlgoType(@NonNull final String type) {
        mSpan.setAttribute(AttributeName.cert_based_auth_public_key_algo_type.name(),
                type);
    }
}