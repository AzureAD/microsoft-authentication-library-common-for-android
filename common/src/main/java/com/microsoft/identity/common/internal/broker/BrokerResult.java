//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult implements Serializable {

    private class SerializedNames {
        static final String ACCeSS_TOKEN = "broker.accessToken";
        static final String ID_TOKEN = "broker.idToken";
        static final String HOMEACCOUNT_ID = "broker.homeAccountId";
        static final String LCCAL_ACCOUNT_ID = "broker.localAccountId";
        static final String USERNAME = "broker.username";
        static final String TOKENTYPE = "broker.tokenType";
        static final String CLIENINFO = "broker.clientInfo";
        static final String AUTHORITY = "broker.authority";
        static final String ENVIRONMENT = "broker.environment";
        static final String EXPIRES_ON = "broker.expiresOn";
        static final String EXTENDED_EXPIRES_ON = "broker.extendedExpiresOn";
        static final String CACHED_AT = "broker.cachedAt";
        static final String SPE_RING = "broker.speRing";
        static final String REFRESH_TOKEN_AGE = "broker.refreshTokenAge";
        static final String ERROR_CODE = "broker.errorCode";
        static final String ERROR_MESSAGE = "broker.errorMessage";
        static final String CORRELATION_ID = "broker.correlationId";
        static final String SUB_ERROR_CODE = "broker.subErrorCode";
        static final String HTTP_STATUS_CODE = "broker.httpStatusCode";
        static final String HTTP_RESPONSE_HEADERS = "broker.httpResponseHeaders";
        static final String HTTP_RESPONSE_BODY = "broker.httpResponseBody";
        static final String CLI_TELEM_ERRORCODE = "broker.cliTelemErrorCode";
        static final String CLI_TELEM_SUB_ERROR_CODe = "broker.cliTelemSubErrorCode";
    }

    // Success parameters
    /**
     * Access token from the response
     */
    @Nullable
    private String mAccessToken;

    /**
     * ID token from the response
     */
    @Nullable
    private String mIdToken;

    /**
     * Home account id of the user.
     */
    @Nullable
    private String mHomeAccountId;

    /**
     * Local account id or user id of the User
     */
    @Nullable
    private String mLocalAccountId;

    /**
     * Username of the User.
     */
    @Nullable
    private String mUserName;

    /**
     * Token type from the response
     */
    @Nullable
    private String mTokenType;

    /**
     * Client Info from the response
     */
    @Nullable
    private String mClientInfo;

    /**
     * Authority from the response.
     */
    @Nullable
    private String mAuthority;

    /**
     * Environment used to cache token.
     */
    @Nullable
    private String mEnvironment;

    /**
     * Expires on value for the token.
     */
    @Nullable
    private long mExpiresOn;

    /**
     * Extended expires on value for the token
     */
    @Nullable
    private long mExtendedExpiresOn;

    /**
     * Access token cache at time in millis
     */
    @Nullable
    private long mCachedAt;

    /**
     * Client telemetry SPE ring
     */
    @Nullable
    private String mSpeRing;

    /**
     * Refresh token age from client telemetry
     */
    @Nullable
    private String mRefreshTokenAge;

    // Exception parameters

    /**
     * Error code corresponding to any of the {@link com.microsoft.identity.common.exception.ErrorStrings}
     */
    @Nullable
    private String mErrorCode;

    /**
     * Error message
     */
    @Nullable
    private String mErrorMessage;

    /**
     * Correlation id of the request
     */
    @Nullable
    private String mCorrelationId;

    /**
     * Sub error code from the error response
     */
    @Nullable
    private String mSubErrorCode;

    /**
     * Http Status code of the error response
     */
    @Nullable
    private String mHttpStatusCode;

    /**
     * Response headers or the error response in json format
     */
    @Nullable
    private String mHttpResponseHeaders;

    /**
     * Response body of the error response
     */
    @Nullable
    private String mHttpResponseBody;

    /**
     * Client telemetry error code
     */
    @Nullable
    private String mCliTelemErrorCode;

    /**
     * Client telemetry sub error code
     */
    @Nullable
    private String mCliTelemSubErrorCode;

}
