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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;

public class MicrosoftStsAuthorizationResult extends AuthorizationResult {

    private static final String TAG = MicrosoftStsAuthorizationResult.class.getSimpleName();

    /* TODO Determine the right place to define these constants as they are shared by calling class */
    public static final String MSTSS_AUTHORIZATION_FINAL_URL = "com.microsoft.identity.client.finalUrl";
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_DESCRIPTION = "error_description";


    public MicrosoftStsAuthorizationResult(int resultCode, final Intent data) {
        if (data == null) {
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                    (AuthorizationErrorResponse.AUTHORIZATION_FAILED, AuthorizationErrorResponse.NULL_INTENT);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
            return;
        }
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.verbose(TAG, null, "User cancel the request in webview.");
                AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                        (AuthorizationErrorResponse.USER_CANCEL, AuthorizationErrorResponse.USER_CANCELLED_FLOW);
                setAuthorizationErrorResponse(AuthorizationStatus.USER_CANCEL, errorResponse);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = data.getStringExtra(MSTSS_AUTHORIZATION_FINAL_URL);
                parseAuthorizationResponse(url);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = data.getStringExtra(ERROR_CODE);
                final String errorDescription = data.getStringExtra(ERROR_DESCRIPTION);
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL, new AuthorizationErrorResponse(error, errorDescription));
                break;

            default:
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL,
                        new AuthorizationErrorResponse(AuthorizationErrorResponse.UNKNOWN_ERROR,
                                AuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + resultCode + "}"));
        }
    }

    private void parseAuthorizationResponse(final String url) {
        HashMap<String, String> urlParameters = StringExtensions.getUrlParameters(url);
        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, null, "Invalid server response, empty query string from the webview redirect.");
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                    (AuthorizationErrorResponse.AUTHORIZATION_FAILED, AuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
            return;
        }
        if (urlParameters.containsKey(CODE)) {
            // TODO : MSAL code returns error if we do not have a state parameter, make sure that's indeed expected
            final String state = urlParameters.get(STATE);
            if (StringUtil.isEmpty(state)) {
                Logger.warn(TAG, null, "State parameter is not returned from the webview redirect.");
                AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                        (ErrorStrings.STATE_MISMATCH, AuthorizationErrorResponse.STATE_NOT_RETURNED);
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
            } else {
                Logger.info(TAG, null, "Auth code is successfully returned from webview redirect.");
                AuthorizationResponse authorizationResponse = new AuthorizationResponse();
                authorizationResponse.setCode(urlParameters.get(CODE));
                authorizationResponse.setState(state);
                setAuthorizationResponse(AuthorizationStatus.SUCCESS, authorizationResponse);
            }
        } else if (urlParameters.containsKey(ERROR)) {
            final String error = urlParameters.get(ERROR);
            final String errorDescription = urlParameters.get(ERROR_DESCRIPTION);
            Logger.info(TAG, null, "Error is returned from webview redirect");
            Logger.infoPII(TAG, null, "error: " + error + " errorDescription: " + errorDescription);
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse(error, errorDescription);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
        } else {
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                    (AuthorizationErrorResponse.AUTHORIZATION_FAILED, AuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
        }
    }
}
