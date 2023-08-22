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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * Assists classes associated with Certificate Based Authentication (CBA) with
 *  telemetry-related tasks.
 */
public interface ICertBasedAuthTelemetryHelper {

    /**
     * Sets attribute that indicates the ICertBasedAuthChallengeHandler handling the current CBA flow.
     * @param challengeHandlerName name of the ICertBasedAuthChallengeHandler class.
     */
    void setCertBasedAuthChallengeHandler(@NonNull final String challengeHandlerName);

    /**
     * Sets attribute that indicates if a PivProvider instance is already present in the
     *  Java Security static list upon adding a new instance.
     * @param present true if PivProvider instance present; false otherwise.
     */
    void setExistingPivProviderPresent(final boolean present);

    /**
     * Indicates on the Span that CBA was successful and then ends current Span.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultSuccess();

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * This method should mainly be used for cases without an exception,
     *  such as user cancellation, no certificates on smartcard, etc.
     * @param message descriptive cause of failure message.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultFailure(@NonNull final String message);

    /**
     * Indicates on the Span that CBA failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultFailure(@NonNull final Exception exception);

    /**
     * Sets attribute that indicates the user's intended choice for CBA (smartcard or on-device).
     * @param choice enum indicating user's intended choice for CBA.
     */
    void setUserChoice(@NonNull final CertBasedAuthChoice choice);

    /**
     * Sets attribute that indicates the selected certificate's public key algorithm type.
     * @param type algorithm name as a string.
     */
    void setPublicKeyAlgoType(@NonNull final String type);
}
