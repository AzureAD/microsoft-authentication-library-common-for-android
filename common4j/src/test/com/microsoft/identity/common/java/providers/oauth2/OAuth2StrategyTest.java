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
package com.microsoft.identity.common.java.providers.oauth2;

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class OAuth2StrategyTest {

    public static class TestStrategy extends OAuth2Strategy {

        @Override
        protected HttpResponse performTokenRequest(TokenRequest request) throws IOException, ClientException {
            return null;
        }

        /**
         * Constructor of OAuth2Strategy.
         *
         * @param config             generic OAuth2 configuration
         * @param strategyParameters
         */
        public TestStrategy(OAuth2Configuration config, OAuth2StrategyParameters strategyParameters) {
            super(config, strategyParameters);
        }

        @Override
        public AuthorizationResultFactory getAuthorizationResultFactory() {
            return null;
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
        public TokenRequest createTokenRequest(AuthorizationRequest request, AuthorizationResponse response, AbstractAuthenticationScheme authScheme) throws ClientException {
            return null;
        }

        @Override
        public TokenRequest createRefreshTokenRequest(AbstractAuthenticationScheme authScheme) throws ClientException {
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
        protected TokenResult getTokenResultFromHttpResponse(HttpResponse response, TokenRequest tokenRequest) throws ClientException {
            //This will return a null TokenResponse.
            return new TokenResult();
        }

        @Override
        protected void validateTokenResponse(TokenRequest request, TokenResponse response) throws ClientException {

        }
    }

    /**
     * This test only verifies that if a null token response is returned from the token result,
     * we don't create an error.
     */
    @Test
    public void testOauth2Strategy_NullTokenResponse() throws Exception {
        TestStrategy s = new TestStrategy(null, OAuth2StrategyParameters.builder().build());
        Assert.assertNotNull(s.requestToken(null));
    }
}
