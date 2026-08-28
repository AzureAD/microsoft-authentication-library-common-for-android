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
package com.microsoft.identity.common.java.foci;

import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.constants.OAuth2SubErrorCode;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.Assert;
import org.junit.Test;

import java.net.HttpURLConnection;

public class FociQueryOutcomeTest {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    /**
     * Builds an unsuccessful {@link TokenResult} carrying the supplied error response fields.
     */
    private static TokenResult errorResult(final int statusCode,
                                           final String error,
                                           final String subError) {
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setStatusCode(statusCode);
        errorResponse.setError(error);
        errorResponse.setSubError(subError);
        return new TokenResult(errorResponse);
    }

    private static TokenResult invalidGrantResult(final String subError) {
        return errorResult(
                HttpURLConnection.HTTP_BAD_REQUEST,
                OAuth2ErrorCode.INVALID_GRANT,
                subError
        );
    }

    private static TokenResult unauthorizedClientResult(final String subError) {
        return errorResult(
                HttpURLConnection.HTTP_BAD_REQUEST,
                OAuth2ErrorCode.UNAUTHORIZED_CLIENT,
                subError
        );
    }

    @Test
    public void fromTokenResult_whenSuccessful_isGranted() {
        final TokenResult result = new TokenResult(new TokenResponse());

        Assert.assertEquals(FociQueryOutcome.GRANTED, FociQueryOutcome.fromTokenResult(result));
    }

    @Test
    public void fromTokenResult_whenClientMismatch_isClientMismatch() {
        Assert.assertEquals(
                FociQueryOutcome.CLIENT_MISMATCH,
                FociQueryOutcome.fromTokenResult(invalidGrantResult(OAuth2SubErrorCode.CLIENT_MISMATCH))
        );
    }

    @Test
    public void fromTokenResult_whenBadToken_isBadToken() {
        Assert.assertEquals(
                FociQueryOutcome.BAD_TOKEN,
                FociQueryOutcome.fromTokenResult(invalidGrantResult(OAuth2SubErrorCode.BAD_TOKEN))
        );
    }

    @Test
    public void fromTokenResult_whenTokenExpired_isTokenExpired() {
        Assert.assertEquals(
                FociQueryOutcome.TOKEN_EXPIRED,
                FociQueryOutcome.fromTokenResult(invalidGrantResult(OAuth2SubErrorCode.TOKEN_EXPIRED))
        );
    }

    /**
     * The service pairs {@code protection_policy_required} with {@code unauthorized_client}, not
     * {@code invalid_grant} — the same pairing {@code ExceptionAdapter.isIntunePolicyRequiredError}
     * recognises.
     */
    @Test
    public void fromTokenResult_whenProtectionPolicyRequired_isProtectionPolicyRequired() {
        Assert.assertEquals(
                FociQueryOutcome.PROTECTION_POLICY_REQUIRED,
                FociQueryOutcome.fromTokenResult(
                        unauthorizedClientResult(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED))
        );
    }

    @Test
    public void fromTokenResult_whenProtectionPolicyRequiredCasingDiffers_stillClassifies() {
        final TokenResult result = errorResult(
                HttpURLConnection.HTTP_BAD_REQUEST,
                "Unauthorized_Client",
                "Protection_Policy_Required"
        );

        Assert.assertEquals(
                FociQueryOutcome.PROTECTION_POLICY_REQUIRED,
                FociQueryOutcome.fromTokenResult(result)
        );
    }

    /**
     * The {@code unauthorized_client} branch is deliberately narrow: it exists only to reach
     * {@code protection_policy_required} and must not swallow other suberrors.
     */
    @Test
    public void fromTokenResult_whenUnauthorizedClientWithOtherSubError_isOtherError() {
        Assert.assertEquals(
                FociQueryOutcome.OTHER_ERROR,
                FociQueryOutcome.fromTokenResult(
                        unauthorizedClientResult(OAuth2SubErrorCode.CLIENT_MISMATCH))
        );
    }

    @Test
    public void fromTokenResult_whenProtectionPolicyRequiredOnUnexpectedStatus_isOtherError() {
        final TokenResult result = errorResult(
                HttpURLConnection.HTTP_FORBIDDEN,
                OAuth2ErrorCode.UNAUTHORIZED_CLIENT,
                OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED
        );

        Assert.assertEquals(FociQueryOutcome.OTHER_ERROR, FociQueryOutcome.fromTokenResult(result));
    }

    /**
     * Guards the gate in the other direction: the suberror is only honoured on the error code the
     * service actually pairs it with.
     */
    @Test
    public void fromTokenResult_whenProtectionPolicyRequiredOnInvalidGrant_isOtherInvalidGrant() {
        Assert.assertEquals(
                FociQueryOutcome.OTHER_INVALID_GRANT,
                FociQueryOutcome.fromTokenResult(
                        invalidGrantResult(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED))
        );
    }

    @Test
    public void fromTokenResult_whenConsentRequired_isConsentRequired() {
        Assert.assertEquals(
                FociQueryOutcome.CONSENT_REQUIRED,
                FociQueryOutcome.fromTokenResult(invalidGrantResult(OAuth2SubErrorCode.CONSENT_REQUIRED))
        );
    }

    /**
     * The service controls the casing of these strings, so matching must not depend on it.
     */
    @Test
    public void fromTokenResult_whenCasingDiffers_stillClassifies() {
        final TokenResult result = errorResult(
                HttpURLConnection.HTTP_BAD_REQUEST,
                "Invalid_Grant",
                "Client_Mismatch"
        );

        Assert.assertEquals(FociQueryOutcome.CLIENT_MISMATCH, FociQueryOutcome.fromTokenResult(result));
    }

    @Test
    public void fromTokenResult_whenSubErrorIsUnrecognised_isOtherInvalidGrant() {
        Assert.assertEquals(
                FociQueryOutcome.OTHER_INVALID_GRANT,
                FociQueryOutcome.fromTokenResult(invalidGrantResult("some_new_suberror"))
        );
    }

    @Test
    public void fromTokenResult_whenSubErrorIsAbsent_isOtherInvalidGrant() {
        Assert.assertEquals(
                FociQueryOutcome.OTHER_INVALID_GRANT,
                FociQueryOutcome.fromTokenResult(invalidGrantResult(null))
        );
    }

    @Test
    public void fromTokenResult_whenThrottled_isOtherError() {
        final TokenResult result = errorResult(HTTP_TOO_MANY_REQUESTS, "temporarily_unavailable", null);

        Assert.assertEquals(FociQueryOutcome.OTHER_ERROR, FociQueryOutcome.fromTokenResult(result));
    }

    /**
     * A membership suberror is only meaningful on an HTTP 400 {@code invalid_grant}; anywhere else
     * it must not be read as a statement about membership.
     */
    @Test
    public void fromTokenResult_whenClientMismatchOnUnexpectedStatus_isOtherError() {
        final TokenResult result = errorResult(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                OAuth2ErrorCode.INVALID_GRANT,
                OAuth2SubErrorCode.CLIENT_MISMATCH
        );

        Assert.assertEquals(FociQueryOutcome.OTHER_ERROR, FociQueryOutcome.fromTokenResult(result));
    }

    @Test
    public void fromTokenResult_whenErrorIsNotInvalidGrant_isOtherError() {
        final TokenResult result = errorResult(
                HttpURLConnection.HTTP_BAD_REQUEST,
                "invalid_client",
                OAuth2SubErrorCode.CLIENT_MISMATCH
        );

        Assert.assertEquals(FociQueryOutcome.OTHER_ERROR, FociQueryOutcome.fromTokenResult(result));
    }

    @Test
    public void fromTokenResult_whenNoErrorResponse_isNoErrorResponse() {
        Assert.assertEquals(
                FociQueryOutcome.NO_ERROR_RESPONSE,
                FociQueryOutcome.fromTokenResult(new TokenResult(null, null))
        );
    }
}
