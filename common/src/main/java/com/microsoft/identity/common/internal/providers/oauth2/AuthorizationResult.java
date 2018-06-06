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
    AuthorizationStatus mAuthorizationStatus;
    AuthorizationResponse mAuthorizationResponse;
    AuthorizationErrorResponse mAuthorizationErrorResponse;

    public static AuthorizationResult create(int resultCode, final Intent data) {
        //Step 1: validate data
        // return the authorization exception/result with AUTHORIZATION_FAILED error.

        if(resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
            // User cancel the request in webview.
            // return the Authentication result with user cancel error.
        } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE) {
            // extract the url from data parse url
            // and encapsulate the auth code into the AuthorizationResponse
            // thus AuthorizationResponse should be part of Authentication Result
        } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {
            // webview returns AuthorizationErrorResponse with error and error description
        } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION) {
            // Serializable exception returned, need to deserialize it and throw it.
        } else if (resultCode == AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME) {
            //Device needs to have broker installed, we expect the apps to call us back when the broker is installed
        } else {
            // throw exception for unknown result code.
        }

        return null;
    }

    /**
     * Enum for representing different authorization status.
     */
    enum AuthorizationStatus {
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
