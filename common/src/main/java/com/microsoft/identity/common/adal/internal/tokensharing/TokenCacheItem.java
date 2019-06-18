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
package com.microsoft.identity.common.adal.internal.tokensharing;

import com.microsoft.identity.common.internal.cache.ITokenCacheItem;

import java.io.Serializable;

/**
 * This class is a shallow copy of the ADAL TokenCacheItem.  The ADAL TokenCacheItem is currently what
 * the token sharing library expects to receive for "ORGID" accounts.  (AAD V1)
 * <p>
 * The following are required fields for deserialization in ADAL...
 * jsonObj.add(OAuth2.AUTHORITY, new JsonPrimitive(tokenCacheItem.getAuthority()));
 * jsonObj.add(OAuth2.REFRESH_TOKEN, new JsonPrimitive(tokenCacheItem.getRefreshToken()));
 * jsonObj.add(OAuth2.ID_TOKEN, new JsonPrimitive(tokenCacheItem.getRawIdToken()));
 * jsonObj.add(OAuth2.ADAL_CLIENT_FAMILY_ID, new JsonPrimitive(tokenCacheItem.getFamilyClientId()));
 */
public class TokenCacheItem implements ITokenCacheItem, Serializable {

    private String mClientId;
    private String mAuthority;
    private String mRefreshtoken;
    private String mResource;
    private String mRawIdToken;
    private String mFamilyClientId;

    /**
     * Default constructor for cache item.
     */
    public TokenCacheItem() {
        // Intentionally left blank
    }

    /**
     * Set the authority.
     *
     * @param authority String authority url.
     */
    public void setAuthority(String authority) {
        mAuthority = authority;
    }

    @Override
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * @param clientId
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    @Override
    public String getClientId() {
        return mClientId;
    }

    public void setResource(final String resource) {
        mResource = resource;
    }

    @Override
    public String getResource() {
        return mResource;
    }

    /**
     * Set the fresh token string.
     *
     * @param refreshToken the refresh token string.
     */
    public void setRefreshToken(String refreshToken) {
        mRefreshtoken = refreshToken;
    }

    @Override
    public String getRefreshToken() {
        return mRefreshtoken;
    }

    /**
     * Set raw ID token.
     *
     * @param rawIdToken raw ID token string.
     */
    public void setRawIdToken(String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    /**
     * Get raw ID token.
     *
     * @return raw ID token string.
     */
    public String getRawIdToken() {
        return mRawIdToken;
    }

    /**
     * Set family client identifier.
     *
     * @param familyClientId the family client ID string.
     */
    public final void setFamilyClientId(final String familyClientId) {
        mFamilyClientId = familyClientId;
    }

    /**
     * Get family client identifier.
     *
     * @return the family client ID string.
     */
    public final String getFamilyClientId() {
        return mFamilyClientId;
    }

}
