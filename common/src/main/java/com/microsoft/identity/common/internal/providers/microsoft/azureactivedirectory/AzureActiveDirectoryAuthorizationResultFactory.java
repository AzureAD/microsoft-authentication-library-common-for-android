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
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Sub class of {@link AuthorizationResultFactory}.
 * Encapsulates Authorization response or errors specific to Azure Active Directory in the form of
 * {@link AzureActiveDirectoryAuthorizationResult}
 */
public class AzureActiveDirectoryAuthorizationResultFactory extends AuthorizationResultFactory<AzureActiveDirectoryAuthorizationResult, AzureActiveDirectoryAuthorizationRequest> {

    private static final String TAG = AzureActiveDirectoryAuthorizationResultFactory.class.getSimpleName();

    private static final String ERROR_CODES = "error_codes";

    @Override
    public AzureActiveDirectoryAuthorizationResult createAuthorizationResult(final int resultCode, final Intent data, final AzureActiveDirectoryAuthorizationRequest request) {
        if (data == null || data.getExtras() == null) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.NULL_INTENT);
        }

        final Bundle extras = data.getExtras();
        final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);
        AzureActiveDirectoryAuthorizationResult result = null;
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.verbose(TAG, "User cancel the request in webview: " + requestId);
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = extras.getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, "");
                result = parseUrlAndCreateAuthorizationResult(url, data.getStringExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER));
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = extras.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                final String errorDescription = extras.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, error, errorDescription);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION:
                //TODO : Verify that a ClientException is serialized here after Broker Implementation
                Serializable responseAuthenticationException =
                        extras.getSerializable(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);

                if (responseAuthenticationException != null && responseAuthenticationException instanceof ClientException) {
                    ClientException exception = (ClientException) responseAuthenticationException;
                    result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                            exception.getErrorCode(), exception.getMessage());
                }
                break;

            case AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME:
                Logger.verbose(TAG, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_DEVICE_REGISTER:
                Logger.verbose(TAG, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED);
                break;

            default:
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + resultCode + "]");
                break;
        }

        if (result == null) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR,
                    MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + resultCode + "]");
        }

        return result;
    }

    private AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                               final String error,
                                                                                               final String errorDescription) {
        AzureActiveDirectoryAuthorizationErrorResponse errorResponse =
                new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    private AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                               final String error,
                                                                                               final String errorDescription,
                                                                                               final String errorCodes,
                                                                                               final String correlationId) {
        Logger.info(TAG, correlationId, "Error is returned from webview redirect");
        Logger.infoPII(TAG, correlationId, "error: " + error + " errorDescription: " + errorDescription);
        AzureActiveDirectoryAuthorizationErrorResponse errorResponse =
                new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        errorResponse.setErrorCodes(errorCodes);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    private AzureActiveDirectoryAuthorizationResult parseUrlAndCreateAuthorizationResult(final String url, final String requestStateParameter) {
        final HashMap<String, String> urlParameters = StringExtensions.getUrlParameters(url);

        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, "Invalid server response, empty query string from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }

        String correlationInResponse = urlParameters.get(AuthenticationConstants.AAD.CORRELATION_ID);
        AzureActiveDirectoryAuthorizationResult result;

        if (urlParameters.containsKey(CODE)) {
            result = validateAndCreateAuthorizationResult(urlParameters.get(CODE), urlParameters.get(STATE), requestStateParameter, correlationInResponse);
        } else if (urlParameters.containsKey(ERROR)) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, urlParameters.get(ERROR),
                    urlParameters.get(ERROR_DESCRIPTION), urlParameters.get(ERROR_CODES), correlationInResponse);
        } else {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }

        return result;
    }

    private AzureActiveDirectoryAuthorizationResult validateAndCreateAuthorizationResult(final String code,
                                                                                         final String state,
                                                                                         final String requestStateParameter,
                                                                                         final String correlationId) {
        AzureActiveDirectoryAuthorizationResult result;

        if (StringUtil.isEmpty(state)) {
            Logger.warn(TAG, correlationId, "State parameter is not returned from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED);
        } else if (StringUtil.isEmpty(requestStateParameter) || !requestStateParameter.equals(state)) {
            Logger.warn(TAG, correlationId, "State parameter returned from the redirect is not same as the one sent in request.");
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME);
        } else {
            Logger.info(TAG, correlationId, "Auth code is successfully returned from webview redirect.");
            AzureActiveDirectoryAuthorizationResponse response = new AzureActiveDirectoryAuthorizationResponse(code, state);
            response.setCorrelationId(correlationId);
            result = new AzureActiveDirectoryAuthorizationResult(AuthorizationStatus.SUCCESS, response);
        }

        return result;
    }

}