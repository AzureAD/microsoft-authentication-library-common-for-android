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

import com.microsoft.identity.common.internal.providers.oauth2.RefreshTokenRequestParameters;

/**
 * Parameters used by the MicrosoftStsOAuth2Strategy when using a refresh token to acquire a new
 * access token.
 */
public class MicrosoftStsRefreshTokenRequestParameters extends RefreshTokenRequestParameters {

    private String mClientId;

    private String mRedirectUri;

    /**
     * Gets the clientId.
     *
     * @return The clientId to get.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the client Id.
     *
     * @param clientId The clientId to set.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Gets the redirectUri.
     *
     * @return The redirectUri to get.
     */
    public String getRedirectUri() {
        return mRedirectUri;
    }

    /**
     * Sets the redirectUri.
     *
     * @param redirectUri The redirectUri to set.
     */
    public void setRedirectUri(final String redirectUri) {
        mRedirectUri = redirectUri;
    }
}
