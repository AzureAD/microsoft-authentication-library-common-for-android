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
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;

/**
 * Sub class of {@link AuthorizationResultFactory}.
 * Encapsulates Authorization response or errors specific to Microsoft STS in the form of
 * {@link MicrosoftStsAuthorizationResult}
 */
public class MicrosoftStsAuthorizationResultFactory extends AuthorizationResultFactory<MicrosoftStsAuthorizationResult, MicrosoftStsAuthorizationRequest> {

    private static final String TAG = MicrosoftStsAuthorizationResultFactory.class.getSimpleName();
    protected static final String ERROR_SUBCODE = "error_subcode";

    /**
     * Constant key to get authorization request final url from intent.
     */
    public static final String MSSTS_AUTHORIZATION_FINAL_URL = "com.microsoft.identity.client.final.url";

    @Override
    public MicrosoftStsAuthorizationResult createAuthorizationResult(final int resultCode,
                                                                     final Intent data,
                                                                     final MicrosoftStsAuthorizationRequest request) {
        if (data == null) {
            return createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.NULL_INTENT
            );
        }

        MicrosoftStsAuthorizationResult result;
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.info(TAG, null, "User cancel the authorization request in UI.");
                result = createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW
                );
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL:
                Logger.info(TAG, null, "SDK cancelled the authorization request.");
                result = createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.SDK_CANCEL,
                        MicrosoftAuthorizationErrorResponse.SDK_AUTH_CANCEL,
                        MicrosoftAuthorizationErrorResponse.SDK_CANCELLED_FLOW
                );
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = data.getStringExtra(AuthorizationStrategy.AUTHORIZATION_FINAL_URL);
                result = parseUrlAndCreateAuthorizationResponse(url, request.getState());
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = data.getStringExtra(
                        AuthenticationConstants.Browser.RESPONSE_ERROR_CODE
                );
                final String errorSubcode = data.getStringExtra(
                        AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE
                );
                final String errorDescription = data.getStringExtra(
                        AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE
                );
                result = createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.FAIL,
                        error,
                        errorSubcode,
                        errorDescription
                );
                break;

            case AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME:
                Logger.info(TAG, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_DEVICE_REGISTER:
                Logger.info(TAG, "Device Registration needed, need to start WPJ");
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED,
                        MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED);
                // Set username returned from the service
                result.getAuthorizationErrorResponse().setUserName(data.getStringExtra(
                                AuthenticationConstants.Broker.INSTALL_UPN_KEY)
                        );
                break;



            default:
                result = createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE
                );
        }

        return result;
    }

    private MicrosoftStsAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                       final String error,
                                                                                       final String errorSubcode,
                                                                                       final String errorDescription) {
        Logger.info(TAG, "Error is returned from webview redirect");
        Logger.infoPII(TAG, "error: " + error
                + "error subcode:" + errorSubcode
                + " errorDescription: " + errorDescription);
        MicrosoftStsAuthorizationErrorResponse errorResponse
                = new MicrosoftStsAuthorizationErrorResponse(error, errorSubcode, errorDescription);
        return new MicrosoftStsAuthorizationResult(authStatus, errorResponse);
    }

    private MicrosoftStsAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                       final String error,
                                                                                       final String errorDescription) {
        Logger.info(TAG, "Error is returned from webview redirect");
        Logger.infoPII(TAG, "error: " + error
                + " errorDescription: " + errorDescription);
        MicrosoftStsAuthorizationErrorResponse errorResponse
                = new MicrosoftStsAuthorizationErrorResponse(error, errorDescription);
        return new MicrosoftStsAuthorizationResult(authStatus, errorResponse);
    }

    private MicrosoftStsAuthorizationResult parseUrlAndCreateAuthorizationResponse(final String url, final String requestStateParameter) {
        final HashMap<String, String> urlParameters = StringUtil.isEmpty(url) ? null : StringExtensions.getUrlParameters(url);
        MicrosoftStsAuthorizationResult result;

        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, "Invalid server response, empty query string from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE
            );
        } else if (urlParameters.containsKey(CODE)) {
            result = validateAndCreateAuthorizationResult(urlParameters, requestStateParameter);
        } else if (urlParameters.containsKey(ERROR)) {
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    urlParameters.get(ERROR),
                    urlParameters.get(ERROR_SUBCODE),
                    urlParameters.get(ERROR_DESCRIPTION)
            );
        } else {
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE
            );
        }

        return result;
    }

    private MicrosoftStsAuthorizationResult validateAndCreateAuthorizationResult(final HashMap<String, String> urlParameters,
                                                                                 final String requestStateParameter) {
        MicrosoftStsAuthorizationResult result;
        final String state = urlParameters.get(STATE);
        final String code = urlParameters.get(CODE);

        if (StringUtil.isEmpty(state)) {
            Logger.warn(TAG, "State parameter is not returned from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED
            );
        } else if (StringUtil.isEmpty(requestStateParameter) || !requestStateParameter.equals(state)) {
            Logger.warn(TAG, "State parameter returned from the redirect is not same as the one sent in request.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME
            );
        } else {

            Logger.info(TAG, "Auth code is successfully returned from webview redirect.");
            MicrosoftStsAuthorizationResponse authResponse =
                    new MicrosoftStsAuthorizationResponse(code, state, urlParameters);
            result = new MicrosoftStsAuthorizationResult(
                    AuthorizationStatus.SUCCESS,
                    authResponse
            );
        }

        return result;
    }

}