package com.microsoft.identity.internal.testutils.strategies;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.internal.testutils.mocks.MockSuccessAuthorizationResultMockedTests;

import java.util.concurrent.Future;

public class MockStrategyWithMockedHttpResponse extends ResourceOwnerPasswordCredentialsTestStrategy {

    /**
     * Constructor of ResourceOwnerPasswordCredentialsTestStrategy.
     *
     * @param config Microsoft Sts OAuth2 configuration
     */
    public MockStrategyWithMockedHttpResponse(MicrosoftStsOAuth2Configuration config) {
        super(config);
    }

    /**
     * Template method for executing an OAuth2 authorization request.
     *
     * @param request               microsoft sts authorization request.
     * @param authorizationStrategy authorization strategy.
     * @return GenericAuthorizationResponse
     */
    @Override
    public Future<AuthorizationResult> requestAuthorization(
            final MicrosoftStsAuthorizationRequest request,
            final AuthorizationStrategy authorizationStrategy) {
        final MockSuccessAuthorizationResultMockedTests authorizationResult = new MockSuccessAuthorizationResultMockedTests();
        final ResultFuture<AuthorizationResult> future = new ResultFuture<>();
        future.setResult(authorizationResult);
        return future;
    }

    @Override
    String getPasswordForUser(String username) {
        return "fake-password";
    }

}
