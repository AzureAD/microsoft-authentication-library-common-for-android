package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.net.HttpResponse;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.when;

public class OAuth2StrategyTest {
    public class TestStrategy extends OAuth2Strategy {

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
        protected void validateAuthorizationRequest(AuthorizationRequest request) {

        }

        @Override
        protected void validateTokenRequest(TokenRequest request) {

        }

        @Override
        protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) throws ClientException {
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
        OAuth2StrategyParameters params = Mockito.mock(OAuth2StrategyParameters.class);
        when(params.getContext()).thenReturn(null);
        TestStrategy s = new TestStrategy(null, params);
        s.requestToken(null);
    }
}
