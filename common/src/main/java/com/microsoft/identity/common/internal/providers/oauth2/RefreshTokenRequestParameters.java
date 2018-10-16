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

import java.util.List;

/**
 * Parameters used by the OAuth2Strategy when using a refresh token to acquire a new access token.
 */
public class RefreshTokenRequestParameters {

    private String mRefreshToken;

    private List<String> mScopes;

    private String mGrantType;

    /**
     * Gets the refresh token value.
     *
     * @return The refresh token value to get.
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * Sets the refresh token value.
     *
     * @param refreshToken The refresh token value to set.
     */
    public void setRefreshToken(final String refreshToken) {
        mRefreshToken = refreshToken;
    }

    /**
     * Gets the List of scopes.
     *
     * @return The List of scopes to get.
     */
    public List<String> getScopes() {
        return mScopes;
    }

    /**
     * Sets the List of scopes.
     *
     * @param scopes The List of scopes to set.
     */
    public void setScopes(final List<String> scopes) {
        mScopes = scopes;
    }

    /**
     * Gets the grant type.
     *
     * @return The grant type to get.
     */
    public String getGrantType() {
        return mGrantType;
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type to set.
     */
    public void setGrantType(final String grantType) {
        mGrantType = grantType;
    }
}
