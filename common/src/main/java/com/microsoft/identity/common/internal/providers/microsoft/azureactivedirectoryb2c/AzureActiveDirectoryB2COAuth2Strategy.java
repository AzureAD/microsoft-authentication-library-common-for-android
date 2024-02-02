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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectoryb2c;

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.oauth2.AccessToken;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import com.microsoft.identity.common.java.util.ResultFuture;

/**
 * Azure Active Directory B2C OAuth Strategy.
 * See the following for more information on the B2C OAuth Implementation:
 * see <a href='https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-oauth-code'>
 * https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-oauth-code</a>
 */
// Suppressing rawtype warnings due to the generic type OAuth2Strategy, AuthorizationRequest, AuthorizationStrategy, AuthorizationResult, AuthorizationResultFactory and Builder
@SuppressWarnings(WarningType.rawtype_warning)
public class AzureActiveDirectoryB2COAuth2Strategy extends OAuth2Strategy {
    /**
     * Constructor of AzureActiveDirectoryB2COAuth2Strategy.
     *
     * @param config OAuth2Configuration
     */
    // Suppressing unchecked warnings due to casting of OAuth2Configuration to GenericOAuth2Configuration and OAuth2StrategyParameters to GenericOAuth2StrategyParameters in the arguments of call to super class' constructor
    @SuppressWarnings(WarningType.unchecked_warning)
    public AzureActiveDirectoryB2COAuth2Strategy(OAuth2Configuration config, OAuth2StrategyParameters options) {
        super(config, options);
    }

    // Suppressing unchecked warnings due to casting of AuthorizationRequest to GenericAuthorizationRequest and AuthorizationStrategy to GenericAuthorizationStrategy in the arguments of call to super class' method requestAuthorization
    @SuppressWarnings(WarningType.unchecked_warning)
    @Override
    public ResultFuture<AuthorizationResult> requestAuthorization(AuthorizationRequest request, IAuthorizationStrategy authorizationStrategy) throws ClientException {
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
        return new AzureActiveDirectoryB2CAuthorizationRequest.Builder();
    }

    @Override
    public AuthorizationRequest.Builder createAuthorizationRequestBuilder(IAccountRecord account) {
        return createAuthorizationRequestBuilder();
    }

    @Override
    public TokenRequest createTokenRequest(AuthorizationRequest request,
                                           AuthorizationResponse response,
                                           AbstractAuthenticationScheme authScheme) {
        return null;
    }


    @Override
    public TokenRequest createRefreshTokenRequest(AbstractAuthenticationScheme authScheme) {
        return null;
    }

    @Override
    public TokenRequest createRopcTokenRequest(RopcTokenCommandParameters tokenCommandParameters) throws ClientException {
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
