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
 * into the onboarding telemetry blob's `last_blocking_error` / `blocking_errors` fields
 * (see [OnboardingTelemetryConstants.LAST_BLOCKING_ERROR] / [OnboardingTelemetryConstants.BLOCKING_ERRORS]).
 *
 * Callers (OneAuth navigation fragment, broker error handler, etc.) invoke
 * [extractBlockingError] on the [MicrosoftTokenResponse] of a failed token request
 * to obtain a string suitable for
 * [com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder.addBlockingError].
 *
 * Returns null when the response carries no error or the error is not blocking.
 *
 * Per design spec (Mobile Onboarding Telemetry §10), the `x-ms-clitelem` header
 * supplies both an `errorCode` (position 2) and a `subErrorCode` (position 3).
 * This parser prefers the sub-error code when present (it is the most specific
 * attribution signal) and falls back to the server error code; both are surfaced
 * to the onboarding blob via `addBlockingError`.
 */
object OnboardingBlockingErrorParser {

    /**
     * AADSTS error codes that look like blocking errors syntactically (5-digit
     * server error codes from eSTS) but are NOT onboarding-remediation signals.
     * These flow through the parser the same as any other server error, so we
     * filter them here at the policy boundary so callers don't have to.
     *
     *  - 50058 UserInformationNotProvided   (no SSO session — user just needs to sign in)
     *  - 50097 DeviceAuthenticationRequired (in-flow device auth challenge; if WPJ runs we
     *                                        already record DeviceRegistrationStarted as a step)
     *  - 50126 InvalidUserNameOrPassword    (wrong credentials — user error)
     */
    private val NON_ONBOARDING_AADSTS_CODES = setOf("50058", "50097", "50126")

    /**
     * Returns true if the candidate error code should be excluded from the
     * onboarding blob's `blocking_errors[]`. See [NON_ONBOARDING_AADSTS_CODES].
     */
    private fun isExcluded(candidate: String): Boolean = candidate in NON_ONBOARDING_AADSTS_CODES

    /**
     * Public policy check for callers that already hold a single, non-header error code and need to
     * decide whether it belongs in the onboarding blob's `blocking_errors[]` — e.g. the Auth UX JS
     * bridge `log_telemetry` path (AB#3688632), which receives one server error code directly rather
     * than an `x-ms-clitelem` header or a [MicrosoftTokenResponse].
     *
     * Exposing the check here keeps a single source of truth for the exclusion set (parity with iOS
     * `nonBlockingOnboardingErrorCodes`) so callers apply the SAME policy as the header/response
     * parsers above instead of duplicating [NON_ONBOARDING_AADSTS_CODES].
     *
     * @return true when [code] is present in the non-blocking exclusion set; false otherwise
     *         (including for null or blank input).
     */
    @JvmStatic
    fun isNonBlockingOnboardingErrorCode(code: String?): Boolean =
        !code.isNullOrBlank() && isExcluded(code)

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
     * Codes in [NON_ONBOARDING_AADSTS_CODES] are also filtered out as they are not
     * onboarding-remediation signals.
     *
     * @return blocking error identifier suitable for `addBlockingError(...)`, or null
     */
    @JvmStatic
    fun extractBlockingError(tokenResponse: MicrosoftTokenResponse?): String? {
        if (tokenResponse == null) return null

        val subError = tokenResponse.cliTelemSubErrorCode
        if (!subError.isNullOrBlank() && subError != "0" && !isExcluded(subError)) {
            return subError
        }

        val error = tokenResponse.cliTelemErrorCode
        if (!error.isNullOrBlank() && error != "0" && !isExcluded(error)) {
            return error
        }

        return null
    }

    /**
     * Convenience overload that parses a raw `x-ms-clitelem` header string directly.
     * Useful when the caller does not have a [MicrosoftTokenResponse] in hand
     * (e.g. parsing a redirect response in a WebView client).
     *
     * Codes in [NON_ONBOARDING_AADSTS_CODES] are filtered out the same as in the
     * [MicrosoftTokenResponse] overload.
     *
     * @return blocking error identifier suitable for `addBlockingError(...)`, or null
     */
    @JvmStatic
    fun extractBlockingError(xMsCliTelemHeader: String?): String? {
        if (xMsCliTelemHeader.isNullOrBlank()) return null

        @Suppress("DEPRECATION")
        val cliTelemInfo = CliTelemInfo.fromXMsCliTelemHeader(xMsCliTelemHeader) ?: return null

        val subError = cliTelemInfo.serverSubErrorCode
        if (!subError.isNullOrBlank() && subError != "0" && !isExcluded(subError)) {
            return subError
        }

        val error = cliTelemInfo.serverErrorCode
        if (!error.isNullOrBlank() && error != "0" && !isExcluded(error)) {
            return error
        }

        return null
    }

    /**
     * Extract blocking-error attribution codes from the OAuth `error_codes` query parameter
     * of an authorization redirect (Microsoft extension to OAuth — comma-separated AADSTS codes,
     * available on `MicrosoftStsAuthorizationErrorResponse.getErrorCodes()`).
     *
     * Unlike the single-value overloads (which return the most-specific identifier from the
     * `x-ms-clitelem` header), eSTS frequently emits multiple AADSTS codes here when one
     * authorization failure has multiple contributing causes (e.g. `"50058,53003"` =
     * "no SSO session AND CA-blocked"). Returning all qualified codes lets callers add
     * each via [com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder.addBlockingError]
     * without losing attribution detail; the schema's `blocking_errors[]` is already an array.
     *
     * Filters out:
     *  - empty entries (e.g. trailing commas)
     *  - the literal `"0"` (eSTS's "no error" sentinel)
     *  - codes in [NON_ONBOARDING_AADSTS_CODES]
     *  - duplicates (preserves first-occurrence order)
     *
     * @return ordered list of qualifying AADSTS codes; empty if none qualify
     */
    @JvmStatic
    fun extractBlockingErrorsFromAuthorizationErrorCodes(errorCodes: String?): List<String> {
        if (errorCodes.isNullOrBlank()) return emptyList()
        return errorCodes.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "0" && !isExcluded(it) }
            .distinct()
    }
}
