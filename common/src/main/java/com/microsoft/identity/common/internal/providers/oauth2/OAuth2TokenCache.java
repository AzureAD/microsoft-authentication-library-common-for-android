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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;

import com.microsoft.identity.common.exception.ClientException;

/**
 * Class for managing the tokens saved locally on a device.
 */
public abstract class OAuth2TokenCache
        <T extends OAuth2Strategy, U extends AuthorizationRequest, V extends TokenResponse> {
    private Context mContext;

    /**
     * Constructs a new OAuth2TokenCache.
     *
     * @param context The Application Context of the consuming app.
     */
    public OAuth2TokenCache(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Saves the credentials and tokens returned by the service to the cache.
     *
     * @param oAuth2Strategy The strategy used to create the token request.
     * @param request        The request used to acquire tokens and credentials.
     * @param response       The response received from the IdP/STS.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract void saveTokens(final T oAuth2Strategy,
                                    final U request,
                                    final V response) throws ClientException;

    protected Context getContext() {
        return mContext;
    }

    protected void setContext(Context context) {
        this.mContext = context;
    }
}
