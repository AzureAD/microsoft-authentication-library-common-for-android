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

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public abstract AcquireTokenResult acquireToken(AcquireTokenOperationParameters request) throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException;

    public abstract void completeAcquireToken(int requestCode, int resultCode, final Intent data);

    public abstract AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters request) throws IOException, ClientException, UiRequiredException, ArgumentException;

    protected void throwIfNetworkNotAvailable(final Context context) throws ClientException {
        final String methodName = ":throwIfNetworkNotAvailable";
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new ClientException(
                    ClientException.DEVICE_NETWORK_NOT_AVAILABLE,
                    "Device network connection is not available."
            );
        }

        Logger.info(
                TAG + methodName,
                "Network status: connected"
        );
    }

    protected TokenResult performTokenRequest(final OAuth2Strategy strategy,
                                            final AuthorizationRequest request,
                                            final AuthorizationResponse response,
                                            final AcquireTokenOperationParameters parameters)
            throws IOException, ClientException {
        throwIfNetworkNotAvailable(parameters.getAppContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        tokenResult = strategy.requestToken(tokenRequest);

        return tokenResult;
    }

    protected ICacheRecord saveTokens(final OAuth2Strategy strategy,
                                    final AuthorizationRequest request,
                                    final TokenResponse tokenResponse,
                                    final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";
        Logger.verbose(
                TAG + methodName,
                "Saving tokens..."
        );
        return tokenCache.save(strategy, request, tokenResponse);
    }

    protected boolean refreshTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    protected boolean accessTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

}
