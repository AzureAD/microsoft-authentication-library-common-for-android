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
package com.microsoft.identity.common.java.telemetry

import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse

/**
 * Bridges eSTS-emitted error codes (carried via the `x-ms-clitelem` header and parsed
 * into [MicrosoftTokenResponse] by [com.microsoft.identity.common.java.providers.microsoft.microsoftsts.AbstractMicrosoftStsTokenResponseHandler])
 * into the onboarding telemetry blob's `lastBlockingError` / `blockingErrors` fields.
 *
 * Callers (OneAuth navigation fragment, broker error handler, etc.) invoke
 * [extractBlockingError] on the [MicrosoftTokenResponse] of a failed token request
 * to obtain a string suitable for
 * [com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder.addBlockingError].
 *
 * Returns null when the response carries no error or the error is not blocking.
 *
 * Per design spec (Mobile Onboarding Telemetry §10), `errorCode` from position 2 of the
 * `x-ms-clitelem` header is the canonical attribution source for blocking errors.
 * Sub-error code (position 3) is also captured for finer-grained classification.
 */
object OnboardingBlockingErrorParser {

    /**
     * Extract a blocking-error attribution string from a [MicrosoftTokenResponse].
     *
     * Returns the most specific available identifier:
     *  1. `serverSubErrorCode` (e.g. `interaction_required`) if present and non-zero, else
     *  2. `serverErrorCode` (e.g. `65001`) if present and non-zero, else
     *  3. `null` (no blocking error to record).
     *
     * Position-2 of the `x-ms-clitelem` header is `0` when there is no error — those
     * cases are filtered out so callers don't pollute the blob with `"0"`.
     *
     * @return blocking error identifier suitable for `addBlockingError(...)`, or null
     */
    @JvmStatic
    fun extractBlockingError(tokenResponse: MicrosoftTokenResponse?): String? {
        if (tokenResponse == null) return null

        val subError = tokenResponse.cliTelemSubErrorCode
        if (!subError.isNullOrBlank() && subError != "0") {
            return subError
        }

        val error = tokenResponse.cliTelemErrorCode
        if (!error.isNullOrBlank() && error != "0") {
            return error
        }

        return null
    }

    /**
     * Convenience overload that parses a raw `x-ms-clitelem` header string directly.
     * Useful when the caller does not have a [MicrosoftTokenResponse] in hand
     * (e.g. parsing a redirect response in a WebView client).
     *
     * @return blocking error identifier suitable for `addBlockingError(...)`, or null
     */
    @JvmStatic
    fun extractBlockingError(xMsCliTelemHeader: String?): String? {
        if (xMsCliTelemHeader.isNullOrBlank()) return null

        @Suppress("DEPRECATION")
        val cliTelemInfo = CliTelemInfo.fromXMsCliTelemHeader(xMsCliTelemHeader) ?: return null

        val subError = cliTelemInfo.serverSubErrorCode
        if (!subError.isNullOrBlank() && subError != "0") {
            return subError
        }

        val error = cliTelemInfo.serverErrorCode
        if (!error.isNullOrBlank() && error != "0") {
            return error
        }

        return null
    }
}
