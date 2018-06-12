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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

/**
 * A class to return the result of the authorization request to the calling code (ADAL or MSAL Controller classes)
 * This class should have a generic status in terms of : Cancelled, TimedOut, Error,  etc...
 * this class should also contain the AuthorizationResponse which contains the details returned from the
 * In the case of an error/exception this class should return the associated exception
 */
public abstract class AuthorizationResult {

    private static final String TAG = AuthorizationResult.class.getSimpleName();

    /* Authorization Response Constants */
    protected static final String CODE = "code";
    protected static final String STATE = "state";
    protected static final String ERROR = "error";
    protected static final String ERROR_DESCRIPTION = "error_description";

    private AuthorizationStatus mAuthorizationStatus;
    private AuthorizationResponse mAuthorizationResponse;
    private AuthorizationErrorResponse mAuthorizationErrorResponse;

    /**
     * @return The {@link AuthorizationStatus} indicating the auth status for the request sent to authorize endopoint.
     */
    public AuthorizationStatus getAuthorizationStatus() {
        return mAuthorizationStatus;
    }

    /**
     * @return The {@link AuthorizationResponse} indicating the auth response  for the request sent to authorize endopoint.
     */
    public AuthorizationResponse getAuthorizationResponse() {
        return mAuthorizationResponse;
    }

    /**
     * @return The {@link AuthorizationErrorResponse} indicating the auth error response for the request sent to authorize endopoint.
     */
    public AuthorizationErrorResponse getAuthorizationErrorResponse() {
        return mAuthorizationErrorResponse;
    }

    protected void setAuthorizationErrorResponse(AuthorizationStatus authStatus, AuthorizationErrorResponse authErrorResponse) {
        mAuthorizationStatus = authStatus;
        mAuthorizationErrorResponse = authErrorResponse;
    }

    protected void setAuthorizationResponse(AuthorizationStatus authStatus, AuthorizationResponse authResponse) {
        mAuthorizationStatus = authStatus;
        mAuthorizationResponse = authResponse;
    }


    /**
     * Enum for representing different authorization status.
     */
    protected enum AuthorizationStatus {
        /**
         * Code is successfully returned.
         */
        SUCCESS,

        /**
         * User press device back button.
         */
        USER_CANCEL,

        /**
         * Returned URI contains error.
         */
        FAIL,

        /**
         * AuthenticationActivity detects the invalid request.
         */
        INVALID_REQUEST
        //TODO:  Investigate how chrome tab returns http timeout error
    }
}
