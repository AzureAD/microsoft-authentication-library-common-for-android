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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
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
                        return new ServiceException(
                                authorizationErrorResponse.getError(),
                                authorizationErrorResponse.getError() + ";" + authorizationErrorResponse.getErrorDescription(),
                                ServiceException.DEFAULT_STATUS_CODE,
                                null
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

        final TokenResult tokenResult = result.getTokenResult();
        final TokenErrorResponse tokenErrorResponse;

        if (tokenResult != null && !tokenResult.getSuccess()) {
            tokenErrorResponse = tokenResult.getErrorResponse();

            if (tokenErrorResponse.getError().equalsIgnoreCase(UiRequiredException.INVALID_GRANT)) {
                Logger.warn(
                        TAG + methodName,
                        "Received invalid_grant"
                );
                return new UiRequiredException(
                        tokenErrorResponse.getError(),
                        tokenErrorResponse.getErrorDescription(),
                        null
                );
            }

            ServiceException outErr = null;

            if (StringUtil.isEmpty(tokenErrorResponse.getError())) {
                Logger.warn(
                        TAG + methodName,
                        "Received unknown error"
                );

                outErr = new ServiceException(
                        ServiceException.UNKNOWN_ERROR,
                        "Request failed, but no error returned back from service.",
                        null
                );
            }

            if (null == outErr) {
                outErr = new ServiceException(
                        tokenErrorResponse.getError(),
                        tokenErrorResponse.getErrorDescription(),
                        null
                );
            }

            applyHttpErrorResponseData(
                    outErr,
                    tokenErrorResponse.getStatusCode(),
                    tokenErrorResponse.getResponseHeadersJson(),
                    tokenErrorResponse.getResponseBody()
            );

            return outErr;
        }

        return null;
    }

    private static void applyHttpErrorResponseData(@NonNull final ServiceException targetException,
                                                   final int statusCode,
                                                   @Nullable String responseHeadersJson,
                                                   @Nullable String responseBody) {
        final String methodName = ":applyHttpErrorResponseData";

        if (null != responseHeadersJson && null != responseBody) {
            try {
                final HttpResponse synthesizedResponse = new HttpResponse(
                        statusCode,
                        responseBody,
                        HeaderSerializationUtil.fromJson(responseHeadersJson)
                );

                targetException.setHttpResponse(synthesizedResponse);
            } catch (JSONException | JsonSyntaxException e) {
                Logger.warn(
                        TAG + methodName,
                        "Failed to deserialize error data: status, headers, response body."
                );
            }
        }
    }

    public static BaseException baseExceptionFromException(final Exception e) {
        BaseException msalException = null;

        if (e instanceof IOException) {
            msalException = new ClientException(
                    ClientException.IO_ERROR,
                    "An IO error occurred with message: " + e.getMessage(),
                    e
            );
        }

        if (e instanceof ClientException) {
            msalException = new ClientException(
                    ((ClientException) e).getErrorCode(),
                    e.getMessage(),
                    e);
        }

        if (e instanceof ArgumentException) {
            ArgumentException argumentException = ((ArgumentException) e);
            msalException = new ArgumentException(
                    argumentException.getArgumentName(),
                    argumentException.getOperationName(),
                    argumentException.getMessage(),
                    argumentException
            );
        }

        if (e instanceof UiRequiredException) {
            UiRequiredException uiRequiredException = ((UiRequiredException) e);
            msalException = new UiRequiredException(
                    uiRequiredException.getErrorCode(),
                    uiRequiredException.getMessage()
            );
        }

        if (msalException == null) {
            msalException = new ClientException(
                    ClientException.UNKNOWN_ERROR,
                    e.getMessage(),
                    e
            );
        }

        return msalException;

    }

}
