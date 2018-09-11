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
package com.microsoft.identity.common.internal.providers.li;

import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;

/**
 * Sub class of {@link AuthorizationResultFactory}.
 * Encapsulates Authorization response or errors specific to Microsoft STS in the form of
  */
public class LiAuthorizationResultFactory extends AuthorizationResultFactory<LiAuthorizationResult, LiAuthorizationRequest> {


    /**
     * Constant key to get authorization request final url from intent.
     */
    public static final String MSSTS_AUTHORIZATION_FINAL_URL = "com.microsoft.identity.client.final.url";


    @Override
    public LiAuthorizationResult createAuthorizationResult(final int resultCode, final Intent data, final LiAuthorizationRequest request) {
        if (data == null) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, MicrosoftAuthorizationErrorResponse.NULL_INTENT);
        }

        LiAuthorizationResult result;
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL, MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW);
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = data.getStringExtra(MSSTS_AUTHORIZATION_FINAL_URL);
                result = parseUrlAndCreateAuthorizationResponse(url, request.getState());
                break;

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = data.getStringExtra(ERROR_CODE);
                final String errorDescription = data.getStringExtra(ERROR_DESCRIPTION);
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, error, errorDescription);
                break;

            default:
                result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE);
        }

        return result;
    }

    private LiAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                       final String error,
                                                                                       final String errorDescription) {
        AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse(error, errorDescription);
        return new LiAuthorizationResult(authStatus, errorResponse);
    }

    private LiAuthorizationResult parseUrlAndCreateAuthorizationResponse(final String url, final String requestStateParameter) {
        final HashMap<String, String> urlParameters = StringUtil.isEmpty(url) ? null : StringExtensions.getUrlParameters(url);
        LiAuthorizationResult result;

        if (urlParameters == null || urlParameters.isEmpty()) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        } else if (urlParameters.containsKey(CODE)) {
            result = validateAndCreateAuthorizationResult(urlParameters.get(CODE), urlParameters.get(STATE), requestStateParameter);
        } else if (urlParameters.containsKey(ERROR)) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    urlParameters.get(ERROR), urlParameters.get(ERROR_DESCRIPTION));
        } else {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }

        return result;
    }

    private LiAuthorizationResult validateAndCreateAuthorizationResult(final String code,
                                                                                 final String state,
                                                                                 final String requestStateParameter) {
        LiAuthorizationResult result;

        if (StringUtil.isEmpty(state)) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED);
        } else if (StringUtil.isEmpty(requestStateParameter) || !requestStateParameter.equals(state)) {
            result = createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME);
        } else {
            AuthorizationResponse authResponse = new AuthorizationResponse(code, state);
            result = new LiAuthorizationResult(AuthorizationStatus.SUCCESS, authResponse);
        }

        return result;
    }

 }