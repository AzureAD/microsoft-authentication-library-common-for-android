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
package com.microsoft.identity.common.java.providers.oauth2;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.UPN_TO_WPJ_KEY;

/**
 * Abstract Factory class which can be extended to construct provider specific {@link AuthorizationResult}.
 */

// Suppressing rawtype warnings due to the generic types AuthorizationResult and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class AuthorizationResultFactory
        <GenericAuthorizationResult extends AuthorizationResult,
                GenericAuthorizationRequest extends AuthorizationRequest> {

    private static final String TAG = AuthorizationResultFactory.class.getSimpleName();

    /**
     * Factory method which can implemented to construct provider specific {@link AuthorizationResult}.
     *
     * @return {@link AuthorizationResult}
     */
    public GenericAuthorizationResult createAuthorizationResult(@NonNull final RawAuthorizationResult data,
                                                                @NonNull final GenericAuthorizationRequest request) {

        final String methodTag = TAG + ":createAuthorizationResult";
        final URI url = data.getAuthorizationFinalUri();
        switch (data.getResultCode()) {
            case CANCELLED:
                Logger.info(methodTag, null, "The authorization request was intentionally cancelled.");
                return createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW
                );

            case SDK_CANCELLED:
                Logger.info(methodTag, null, "SDK cancelled the authorization request.");
                return createAuthorizationResultWithErrorResponse(
                        AuthorizationStatus.SDK_CANCEL,
                        MicrosoftAuthorizationErrorResponse.SDK_AUTH_CANCEL,
                        MicrosoftAuthorizationErrorResponse.SDK_CANCELLED_FLOW
                );

            case COMPLETED:
                if (url == null) {
                    Logger.warn(methodTag, null, "returned URL is null or empty.");
                    return createAuthorizationResultWithErrorResponse(
                            AuthorizationStatus.FAIL,
                            MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                            MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
                }
                return parseRedirectUriAndCreateAuthorizationResult(url, request.getState());

            case NON_OAUTH_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final BaseException exception = data.getException();
                if (exception != null) {
                    return createAuthorizationResultWithErrorResponse(
                            AuthorizationStatus.FAIL,
                            exception.getErrorCode(),
                            exception.getMessage());
                }

            case BROKER_INSTALLATION_TRIGGERED: {
                Logger.info(methodTag, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                final GenericAuthorizationResult result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED_ERROR_DESCRIPTION);

                // Set username returned from the service. We're not currently using it though, but the server returns it.
                final Map<String, String> urlParameters = UrlUtil.getParameters(url);
                result.getAuthorizationErrorResponse().setUpnToWpj(urlParameters.get(UPN_TO_WPJ_KEY));
                return result;
            }

            case DEVICE_REGISTRATION_REQUIRED: {
                Logger.info(methodTag, "Device Registration needed, need to start WPJ");
                final GenericAuthorizationResult result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED,
                        MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED_ERROR_DESCRIPTION);

                // Set username returned from the service
                final Map<String, String> urlParameters = UrlUtil.getParameters(url);
                result.getAuthorizationErrorResponse().setUpnToWpj(urlParameters.get(UPN_TO_WPJ_KEY));
                return result;
            }

            case MDM_FLOW:
                Logger.info(methodTag, "MDM required. Launching Intune MDM link on browser.");
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.DEVICE_NEEDS_TO_BE_MANAGED,
                        MicrosoftAuthorizationErrorResponse.DEVICE_NEEDS_TO_BE_MANAGED_ERROR_DESCRIPTION);
        }

        return createAuthorizationResultWithErrorResponse(
                AuthorizationStatus.FAIL,
                MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR,
                MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + data.getResultCode() + "]");
    }

    protected abstract GenericAuthorizationResult parseRedirectUriAndCreateAuthorizationResult(final @NonNull URI redirectUri,
                                                                                               @Nullable final String requestStateParameter);

    protected abstract GenericAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                             @NonNull final String error,
                                                                                             @Nullable final String errorDescription);

    protected abstract GenericAuthorizationResult validateAndCreateAuthorizationResult(@NonNull final Map<String, String> urlParameters,
                                                                                       @Nullable final String requestStateParameter,
                                                                                       @Nullable final String correlationId);
}
