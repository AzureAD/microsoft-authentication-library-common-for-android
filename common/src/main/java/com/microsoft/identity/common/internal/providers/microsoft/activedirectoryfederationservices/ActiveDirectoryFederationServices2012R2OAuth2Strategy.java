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
package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import android.net.Uri;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Azure Active Directory Federation Services 2012 R2 OAuth Strategy
 * ADFS 2012 R2 (v3) only support oAuth and did not yet support OIDC... as result there is
 * no opportunity to get an ID Token
 * Docs on this functionality are lacking....
 * see <a href='https://msdn.microsoft.com/en-us/library/dn633593.aspx'>https://msdn.microsoft.com/en-us/library/dn633593.aspx</a>
 * see <a href='https://blogs.technet.microsoft.com/maheshu/2015/04/28/oauth-2-0-support-in-adfs-on-windows-server-2012-r2/'>https://blogs.technet.microsoft.com/maheshu/2015/04/28/oauth-2-0-support-in-adfs-on-windows-server-2012-r2/</a>
 */
public class ActiveDirectoryFederationServices2012R2OAuth2Strategy extends OAuth2Strategy {
    public ActiveDirectoryFederationServices2012R2OAuth2Strategy(OAuth2Configuration config) {
        super(config);
    }

    @Override
    public AuthorizationResponse requestAuthorization(AuthorizationRequest request, AuthorizationStrategy authorizationStrategy) {
        return super.requestAuthorization(request, authorizationStrategy);
    }

    @Override
    protected Uri createAuthorizationUri() {
        return super.createAuthorizationUri();
    }

    @Override
    public String getIssuerCacheIdentifier(AuthorizationRequest request) {
        return null;
    }

    @Override
    public AccessToken getAccessTokenFromResponse(TokenResponse response) {
        return null;
    }

    @Override
    public RefreshToken getRefreshTokenFromResponse(TokenResponse response) {
        return null;
    }

    @Override
    public Account createAccount(TokenResponse response) {
        return null;
    }

    @Override
    protected void validateAuthorizationRequest(AuthorizationRequest request) {
    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {
    }

    /**
     * This function is only used to check if the {@link HttpResponse#mResponseBody} is in the right JSON format.
     *
     * AD FS responds with an HTTP 200 with the access_token, refresh_token, and id_token in the body.
     *
     * TODO: No sure if the error description is required for ADFS 2012R2. Could not find related docs on the error response.
     *
     * @param response
     */
    @Override
    protected void validateTokenResponse(HttpResponse response) throws ServiceException {
        if (null == response) {
            throw new IllegalArgumentException("The http response is null.");
        }

        if(StringUtil.isEmpty(response.getBody())) {
            throw new IllegalArgumentException("The http response body is null.");
        }

        StringUtil.validateJsonFormat(response.getBody(), null);

        final Map<String, String> responseItems = new HashMap<>();
        StringUtil.extractJsonObjects(responseItems, response.getBody());

        if (responseItems.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)
                && responseItems.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
            StringUtil.validateJWTFormat(responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN));
        }
    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) {
        return null;
    }
}
