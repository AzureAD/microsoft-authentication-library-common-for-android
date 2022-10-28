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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;
import com.microsoft.identity.common.java.logging.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.CORRELATION_ID;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_CODES;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_DESCRIPTION;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.STATE;

/**
 * Sub class of {@link AuthorizationResultFactory}.
 * Encapsulates Authorization response or errors specific to Azure Active Directory in the form of
 * {@link AzureActiveDirectoryAuthorizationResult}
 */
public class AzureActiveDirectoryAuthorizationResultFactory
        extends AuthorizationResultFactory<AzureActiveDirectoryAuthorizationResult, AzureActiveDirectoryAuthorizationRequest> {

    private static final String TAG = AzureActiveDirectoryAuthorizationResultFactory.class.getSimpleName();

    @Override
    protected AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                                 @NonNull final String error,
                                                                                                 @Nullable final String errorDescription) {
        final AzureActiveDirectoryAuthorizationErrorResponse errorResponse =
                new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    @Override
    protected AzureActiveDirectoryAuthorizationResult parseRedirectUriAndCreateAuthorizationResult(@NonNull final URI redirectUri,
                                                                                                   @Nullable final String requestStateParameter) {
        final Map<String, String> urlParameters = UrlUtil.getParameters(redirectUri);
        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, "Invalid server response, empty query string from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }

        final String correlationInResponse = urlParameters.get(CORRELATION_ID);
        final AzureActiveDirectoryAuthorizationResult result;

        if (urlParameters.containsKey(CODE)) {
            result = validateAndCreateAuthorizationResult(urlParameters, requestStateParameter, correlationInResponse);
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

    private AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(@NonNull final AuthorizationStatus authStatus,
                                                                                               @Nullable final String error,
                                                                                               @Nullable final String errorDescription,
                                                                                               @Nullable final String errorCodes,
                                                                                               @Nullable final String correlationId) {
        final String methodTag = TAG + ":createAuthorizationResultWithErrorResponse";
        Logger.info(methodTag, correlationId, "Error is returned from webview redirect");
        Logger.infoPII(methodTag, correlationId, "error: " + error + " errorDescription: " + errorDescription);
        final AzureActiveDirectoryAuthorizationErrorResponse errorResponse =
                new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        errorResponse.setErrorCodes(errorCodes);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    @Override
    protected AzureActiveDirectoryAuthorizationResult validateAndCreateAuthorizationResult(@NonNull final Map<String, String> urlParameters,
                                                                                           @Nullable final String requestStateParameter,
                                                                                           @Nullable final String correlationId) {
        final String methodTag = TAG + ":validateAndCreateAuthorizationResult";
        AzureActiveDirectoryAuthorizationResult result;
        final String state = urlParameters.get(STATE);
        final String code = urlParameters.get(CODE);

        if (StringUtil.isNullOrEmpty(state)) {
            Logger.warn(methodTag, correlationId, "State parameter is not returned from the webview redirect.");
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED);
        } else if (StringUtil.isNullOrEmpty(requestStateParameter) || !requestStateParameter.equals(state)) {
            Logger.warn(methodTag, correlationId, "State parameter returned from the redirect is not same as the one sent in request.");
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME);
        } else {
            Logger.info(methodTag, correlationId, "Auth code is successfully returned from webview redirect.");
            AzureActiveDirectoryAuthorizationResponse response = new AzureActiveDirectoryAuthorizationResponse(code, state);
            response.setCorrelationId(correlationId);
            result = new AzureActiveDirectoryAuthorizationResult(AuthorizationStatus.SUCCESS, response);
        }

        return result;
    }
}
