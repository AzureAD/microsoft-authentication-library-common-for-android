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

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import java.util.concurrent.Future;

/**
 * Azure Active Directory Federation Services 2016 oAuth2 Strategy.
 * For information on ADFS 2016 oAuth and OIDC support
 * see <a href='https://docs.microsoft.com/en-us/windows-server/identity/ad-fs/overview/ad-fs-scenarios-for-developers'>https://docs.microsoft.com/en-us/windows-server/identity/ad-fs/overview/ad-fs-scenarios-for-developers</a>
 */
public class ActiveDirectoryFederationServices2016OAuth2Strategy extends OAuth2Strategy<AccessToken,
        BaseAccount,
        AuthorizationRequest<?>,
        AuthorizationRequest.Builder,
        AuthorizationStrategy,
        OAuth2Configuration,
        OAuth2StrategyParameters,
        AuthorizationResponse,
        RefreshToken,
        TokenRequest,
        TokenResponse,
        TokenResult,
        AuthorizationResult> {
    /**
     * Constructor of ActiveDirectoryFederationServices2016OAuth2Strategy.
     *
     * @param config OAuth2Configuration
     */
    public ActiveDirectoryFederationServices2016OAuth2Strategy(OAuth2Configuration config,
                                                               OAuth2StrategyParameters options) {
        super(config, options);
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(AuthorizationRequest request, AuthorizationStrategy authorizationStrategy) {
        return super.requestAuthorization(request, authorizationStrategy);
    }

    @Override
    public AuthorizationResultFactory getAuthorizationResultFactory() {
        throw new UnsupportedOperationException();
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
    public BaseAccount createAccount(TokenResponse response) {
        return null;
    }

    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        return null;
    }

    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder(IAccountRecord account) {
        return null;
    }

    @Override
    public TokenRequest createTokenRequest(AuthorizationRequest request,
                                           AuthorizationResponse response,
                                           AbstractAuthenticationScheme authScheme) {
        return null;
    }

    @Override
    public TokenRequest createRefreshTokenRequest(AbstractAuthenticationScheme scheme) {
        return null;
    }

    @Override
    protected void validateAuthorizationRequest(AuthorizationRequest request) {
    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {

    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) {
        return null;
    }

    @Override
    protected void validateTokenResponse(TokenRequest request, TokenResponse response) {

    }
}
