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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.content.Intent;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_ERROR_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_FINAL_URL;

public class AzureActiveDirectoryAuthorizationResult extends AuthorizationResult {

    private static final String TAG = AzureActiveDirectoryAuthorizationResult.class.getSimpleName();

    public AzureActiveDirectoryAuthorizationResult(int resultCode, final Intent data) {
        if (data == null) {
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                    (AuthorizationErrorResponse.AUTHORIZATION_FAILED, AuthorizationErrorResponse.NULL_INTENT);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
            return;
        }
        final Bundle extras = data.getExtras();
        final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);
        switch (resultCode) {

            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.verbose(TAG, null, "User cancel the request in webview: " + requestId);
                AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                        (AuthorizationErrorResponse.USER_CANCEL, AuthorizationErrorResponse.USER_CANCELLED_FLOW);
                setAuthorizationErrorResponse(AuthorizationStatus.USER_CANCEL, errorResponse);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = extras.getString(RESPONSE_FINAL_URL, "");
                parseAuthorizationResponse(url);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = extras.getString(RESPONSE_ERROR_CODE);
                final String errorDescription = extras.getString(RESPONSE_ERROR_MESSAGE);
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL, new AuthorizationErrorResponse(error, errorDescription));
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION:
                Serializable authException = extras
                        .getSerializable(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);
                //TODO : AuthenticationException is not part of Common, what needs to be done here.
                break;

            case AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME:
                Logger.verbose(TAG, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                errorResponse = new AuthorizationErrorResponse(AuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        AuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED);
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
                break;

            default:
                setAuthorizationErrorResponse(AuthorizationStatus.FAIL,
                        new AuthorizationErrorResponse(AuthorizationErrorResponse.UNKNOWN_ERROR,
                                AuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + resultCode + "}"));
        }
    }

    private void parseAuthorizationResponse(String url) {
        HashMap<String, String> urlParameters = StringExtensions.getUrlParameters(url);
        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, null, "Invalid server response, empty query string from the webview redirect.");
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse
                    (AuthorizationErrorResponse.AUTHORIZATION_FAILED, AuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
            return;
        }
        if (urlParameters.containsKey(CODE)) {
            final String state = urlParameters.get(STATE);
            //TODO : ADAL code additionally retrieves authorization URI and resource from state. Is this needed here?
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
            /* TODO  Resolve the inconsistencies
             1. The String ADAL code checks for Correlation ID is " "correlation_id" the AAD spec documents as "session_state"
             2. ADAL code looks for "error_codes" in the parameters. AAD spec doesn't document it.
             */
            String correlationInResponse = urlParameters.get(AuthenticationConstants.AAD.CORRELATION_ID);
            final String error = urlParameters.get(ERROR);
            final String errorDescription = urlParameters.get(ERROR_DESCRIPTION);
            Logger.info(TAG, correlationInResponse, "Error is returned from webview redirect");
            Logger.infoPII(TAG, correlationInResponse, "error: " + error + " errorDescription: " + errorDescription);
            AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse(error, errorDescription);
            setAuthorizationErrorResponse(AuthorizationStatus.FAIL, errorResponse);
        }
    }
}
