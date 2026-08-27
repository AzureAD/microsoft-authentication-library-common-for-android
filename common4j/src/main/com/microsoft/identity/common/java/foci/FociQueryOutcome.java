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
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import java.net.HttpURLConnection;

import lombok.NonNull;

/**
 * Classified result of a family-of-client-ids (FoCI) membership query, as performed by
 * {@code FociQueryUtilities.queryFociMembership}.
 *
 * <p>The query asks the service whether a given client id may redeem a family refresh token. A
 * boolean answer conflates two very different situations: the service stating that the client id is
 * <em>not</em> a family member, and the query failing for a reason that says nothing about
 * membership — for example the refresh token it was attempted with having been revoked or expired.
 * Both arrive as HTTP 400 {@code invalid_grant} and are distinguishable only by the suberror, so
 * callers that need to tell them apart require this classification rather than a boolean.
 *
 * <p>The value set is deliberately closed. The suberror is a service-controlled string, so it is
 * mapped onto a bounded set rather than surfaced verbatim, keeping it safe to use as a telemetry
 * dimension.
 */
public enum FociQueryOutcome {

    /**
     * The service issued a token: the client id is a member of the family.
     */
    GRANTED,

    /**
     * {@link OAuth2SubErrorCode#CLIENT_MISMATCH} — the service states the client id is not a member
     * of the family the refresh token belongs to. The only value that is a definitive statement
     * about membership.
     */
    CLIENT_MISMATCH,

    /**
     * {@link OAuth2SubErrorCode#BAD_TOKEN} — the refresh token the query was attempted with was
     * rejected. Says nothing about the client id's membership.
     */
    BAD_TOKEN,

    /**
     * {@link OAuth2SubErrorCode#TOKEN_EXPIRED} — the refresh token the query was attempted with had
     * expired. Says nothing about the client id's membership.
     */
    TOKEN_EXPIRED,

    /**
     * {@link OAuth2SubErrorCode#PROTECTION_POLICY_REQUIRED} — a policy blocked the exchange. Says
     * nothing about the client id's membership.
     */
    PROTECTION_POLICY_REQUIRED,

    /**
     * {@link OAuth2SubErrorCode#CONSENT_REQUIRED} — consent is needed before the exchange can
     * complete. Says nothing about the client id's membership.
     */
    CONSENT_REQUIRED,

    /**
     * An {@code invalid_grant} carrying a suberror this enum does not model, or no suberror at all.
     */
    OTHER_INVALID_GRANT,

    /**
     * An error response that was not an HTTP 400 {@code invalid_grant} — for example a throttling
     * or service-side failure.
     */
    OTHER_ERROR,

    /**
     * The result was unsuccessful but carried no error response to classify.
     */
    NO_ERROR_RESPONSE;

    /**
     * Classifies a token result produced by a FoCI membership query.
     *
     * <p>Only an HTTP 400 {@code invalid_grant} is examined for a suberror. A suberror arriving on
     * any other status or error code is not treated as a membership answer, so such responses
     * classify as {@link #OTHER_ERROR}.
     *
     * @param tokenResult the result of the membership query.
     * @return the classified outcome; {@link #GRANTED} if and only if
     * {@link TokenResult#getSuccess()} is {@code true}.
     */
    @NonNull
    public static FociQueryOutcome fromTokenResult(@NonNull final TokenResult tokenResult) {
        if (tokenResult.getSuccess()) {
            return GRANTED;
        }

        final TokenErrorResponse errorResponse = tokenResult.getErrorResponse();
        if (errorResponse == null) {
            return NO_ERROR_RESPONSE;
        }

        final boolean isInvalidGrant =
                HttpURLConnection.HTTP_BAD_REQUEST == errorResponse.getStatusCode()
                        && OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorResponse.getError());
        if (!isInvalidGrant) {
            return OTHER_ERROR;
        }

        final String subError = errorResponse.getSubError();
        if (OAuth2SubErrorCode.CLIENT_MISMATCH.equalsIgnoreCase(subError)) {
            return CLIENT_MISMATCH;
        }
        if (OAuth2SubErrorCode.BAD_TOKEN.equalsIgnoreCase(subError)) {
            return BAD_TOKEN;
        }
        if (OAuth2SubErrorCode.TOKEN_EXPIRED.equalsIgnoreCase(subError)) {
            return TOKEN_EXPIRED;
        }
        if (OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED.equalsIgnoreCase(subError)) {
            return PROTECTION_POLICY_REQUIRED;
        }
        if (OAuth2SubErrorCode.CONSENT_REQUIRED.equalsIgnoreCase(subError)) {
            return CONSENT_REQUIRED;
        }
        return OTHER_INVALID_GRANT;
    }
}
