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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import lombok.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.net.URI;
import java.util.Map;

import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_DESCRIPTION;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_SUBCODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.STATE;

/**
 * Sub class of {@link AuthorizationResultFactory}.
 * Encapsulates Authorization response or errors specific to Microsoft STS in the form of
 * {@link MicrosoftStsAuthorizationResult}
 */
public class MicrosoftStsAuthorizationResultFactory
        extends AuthorizationResultFactory<MicrosoftStsAuthorizationResult, MicrosoftStsAuthorizationRequest> {

    private static final String TAG = MicrosoftStsAuthorizationResultFactory.class.getSimpleName();

    @Override
    protected MicrosoftStsAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                         @NonNull final String error,
                                                                                         @Nullable final String errorDescription) {
        final String methodTag = TAG + ":createAuthorizationResultWithErrorResponse";
        Logger.info(methodTag, "Error is returned from webview redirect");
        Logger.infoPII(methodTag, "error: " + error
                + " errorDescription: " + errorDescription);
        MicrosoftStsAuthorizationErrorResponse errorResponse
                = new MicrosoftStsAuthorizationErrorResponse(error, errorDescription);
        return new MicrosoftStsAuthorizationResult(authStatus, errorResponse);
    }

    @Override
    protected MicrosoftStsAuthorizationResult parseRedirectUriAndCreateAuthorizationResult(@NonNull final URI redirectUri,
                                                                                           @Nullable final String requestStateParameter) {
        final String methodTag = TAG + ":parseUrlAndCreateAuthorizationResponse";

        final Map<String, String> urlParameters = UrlUtil.getParameters(redirectUri);

        MicrosoftStsAuthorizationResult result;
        if (urlParameters.isEmpty()) {
            Logger.warn(methodTag, "Invalid server response, empty query string from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE
            );
        } else if (urlParameters.containsKey(CODE)) {
            result = validateAndCreateAuthorizationResult(urlParameters, requestStateParameter, null);
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

    private MicrosoftStsAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                       final String error,
                                                                                       final String errorSubcode,
                                                                                       final String errorDescription) {
        final String methodTag = TAG + ":createAuthorizationResultWithErrorResponse";
        Logger.info(methodTag, "Error is returned from webview redirect");
        Logger.infoPII(methodTag, "error: " + error
                + "error subcode:" + errorSubcode
                + " errorDescription: " + errorDescription);
        MicrosoftStsAuthorizationErrorResponse errorResponse
                = new MicrosoftStsAuthorizationErrorResponse(error, errorSubcode, errorDescription);
        return new MicrosoftStsAuthorizationResult(authStatus, errorResponse);
    }

    @Override
    protected MicrosoftStsAuthorizationResult validateAndCreateAuthorizationResult(@NonNull final Map<String, String> urlParameters,
                                                                                   @Nullable final String requestStateParameter,
                                                                                   @Nullable final String correlationId) {
        final String methodTag = TAG + ":validateAndCreateAuthorizationResult";
        MicrosoftStsAuthorizationResult result;
        final String state = urlParameters.get(STATE);
        final String code = urlParameters.get(CODE);

        if (StringUtil.isNullOrEmpty(state)) {
            Logger.warn(methodTag, "State parameter is not returned from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED
            );
        } else if (StringUtil.isNullOrEmpty(requestStateParameter) || !requestStateParameter.equals(state)) {
            Logger.warn(methodTag, "State parameter returned from the redirect is not same as the one sent in request.");
            result = createAuthorizationResultWithErrorResponse(
                    AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME
            );
        } else {
            Logger.info(methodTag, "Auth code is successfully returned from webview redirect.");
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
