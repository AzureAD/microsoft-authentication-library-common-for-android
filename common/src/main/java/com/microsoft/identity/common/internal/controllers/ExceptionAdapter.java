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
package com.microsoft.identity.common.internal.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.DeviceRegistrationRequiredException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.json.JSONException;

import java.io.IOException;

public class ExceptionAdapter {

    private static final String TAG = ExceptionAdapter.class.getSimpleName();

    @Nullable
    public static BaseException exceptionFromAcquireTokenResult(final AcquireTokenResult result) {
        final String methodName = ":exceptionFromAcquireTokenResult";
        final AuthorizationResult authorizationResult = result.getAuthorizationResult();

        if (null != authorizationResult) {
            final AuthorizationErrorResponse authorizationErrorResponse = authorizationResult.getAuthorizationErrorResponse();
            if (!authorizationResult.getSuccess()) {
                //THERE ARE CURRENTLY NO USAGES of INVALID_REQUEST
                switch (result.getAuthorizationResult().getAuthorizationStatus()) {
                    case FAIL:
                        // Check if the error is to register device and throw DEVICE_REGISTRATION_NEEDED exception
                        if (authorizationErrorResponse instanceof MicrosoftAuthorizationErrorResponse) {
                            MicrosoftAuthorizationErrorResponse microsoftAuthorizationErrorResponse =
                                    (MicrosoftAuthorizationErrorResponse) authorizationErrorResponse;

                            if (microsoftAuthorizationErrorResponse.getError().equals(
                                    MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED)) {

                                return new DeviceRegistrationRequiredException(
                                        microsoftAuthorizationErrorResponse.getError(),
                                        microsoftAuthorizationErrorResponse.getErrorDescription(),
                                        microsoftAuthorizationErrorResponse.getUserName()
                                );
                            }
                        }

                        return new ServiceException(
                                authorizationErrorResponse.getError(),
                                authorizationErrorResponse.getError() + ";" + authorizationErrorResponse.getErrorDescription(),
                                ServiceException.DEFAULT_STATUS_CODE,
                                null
                        );

                    case SDK_CANCEL:
                        return new ClientException(
                                authorizationErrorResponse.getError(),
                                authorizationErrorResponse.getErrorDescription()
                        );

                    case USER_CANCEL:
                        return new UserCancelException();

                }
            }
        } else {
            Logger.warn(
                    TAG + methodName,
                    "AuthorizationResult was null -- expected for ATS cases."
            );
        }

        return exceptionFromTokenResult(result.getTokenResult());
    }

    /**
     * Get an exception out of a TokenResult object.
     *
     * @param tokenResult
     * @return ServiceException, UiRequiredException
     * */
    public static ServiceException exceptionFromTokenResult(final TokenResult tokenResult) {
        final String methodName = ":exceptionFromTokenResult";

        ServiceException outErr;

        if (tokenResult != null &&
                !tokenResult.getSuccess() &&
                tokenResult.getErrorResponse() != null &&
                !StringUtil.isEmpty(tokenResult.getErrorResponse().getError())) {

            outErr = getExceptionFromTokenErrorResponse(tokenResult.getErrorResponse());
            applyCliTelemInfo(tokenResult.getCliTelemInfo(), outErr);
        }else {
            Logger.warn(
                    TAG + methodName,
                    "Unknown error, Token result is null [" + (tokenResult == null) + "]"
            );
            outErr = new ServiceException(
                    ServiceException.UNKNOWN_ERROR,
                    "Request failed, but no error returned back from service.",
                    null
            );
        }

        return outErr;
    }

    /**
     * Determine if an exception owning the given error codes should be converted into UiRequiredException.
     *
     * @param oAuthError
     * @return boolean
     * */
    private static boolean shouldBeConvertedToUiRequiredException(final String oAuthError){
        // Invalid_grant doesn't necessarily requires UI protocol-wise.
        // We simplify our logic because this layer is also used by MSAL.

        return oAuthError.equalsIgnoreCase(AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT) ||
                oAuthError.equalsIgnoreCase(AuthenticationConstants.OAuth2ErrorCode.INTERACTION_REQUIRED);

    }


    /**
     * Get an exception object from the given oAuth values.
     *
     * @param errorResponse
     * @return ServiceException, UiRequiredException
     * */
    public static ServiceException getExceptionFromTokenErrorResponse(@NonNull final TokenErrorResponse errorResponse) {
        final String methodName = ":getExceptionFromTokenErrorResponse";

        final ServiceException outErr;

        if (shouldBeConvertedToUiRequiredException(errorResponse.getError())) {

            outErr = new UiRequiredException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription());
        } else {
            outErr = new ServiceException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription(),
                    null);
        }

        outErr.setOauthSubErrorCode(errorResponse.getSubError());

        try {
            outErr.setHttpResponse(
                    synthesizeHttpResponse(
                            errorResponse.getStatusCode(),
                            errorResponse.getResponseHeadersJson(),
                            errorResponse.getResponseBody()
                    )
            );
        }
        catch (JSONException e) {
            Logger.warn(
                    TAG + methodName,
                    "Failed to deserialize error data: status, headers, response body."
            );
        }

        return outErr;
    }

    public static void applyCliTelemInfo(@Nullable final CliTelemInfo cliTelemInfo,
                                         @NonNull final BaseException outErr) {
        if (null != cliTelemInfo) {
            outErr.setSpeRing(cliTelemInfo.getSpeRing());
            outErr.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
            outErr.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
            outErr.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());
        }
    }

    private static HttpResponse synthesizeHttpResponse(final int statusCode,
                                                       @Nullable String responseHeadersJson,
                                                       @Nullable String responseBody) {
        final String methodName = ":applyHttpErrorResponseData";

        if (null != responseHeadersJson && null != responseBody) {
            try {
                return new HttpResponse(
                        statusCode,
                        responseBody,
                        HeaderSerializationUtil.fromJson(responseHeadersJson)
                );
            } catch (JsonSyntaxException e) {
                Logger.warn(
                        TAG + methodName,
                        "Failed to deserialize error data: status, headers, response body."
                );
            }
        }

        return null;
    }

    public static BaseException baseExceptionFromException(final Exception e) {
        if (e instanceof IOException) {
            return new ClientException(
                    ClientException.IO_ERROR,
                    "An IO error occurred with message: " + e.getMessage(),
                    e
            );
        }

        if (e instanceof BaseException) {
            return (BaseException) e;
        }

        return new ClientException(
                ClientException.UNKNOWN_ERROR,
                e.getMessage(),
                e);
    }
}
