package com.microsoft.identity.common.internal.providers.ropc;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.common.internal.testutils.MockTokenResponse;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

public class MockStrategy extends MicrosoftStsOAuth2Strategy {

    private static final String TAG = ResourceOwnerPasswordCredentialsTestStrategy.class.getSimpleName();

    public final String USERNAME_EMPTY_OR_NULL = "username_empty_or_null";
    public final String PASSWORD_EMPTY_OR_NULL = "password_empty_or_null";
    public final String SCOPE_EMPTY_OR_NULL = "scope_empty_or_null";
    public final String GRANT_TYPE_EMPTY_OR_NULL = "grant_type_empty_or_null";

    /**
     * Constructor of MockStrategy.
     *
     * @param config Microsoft Sts OAuth2 configuration
     */
    public MockStrategy(MicrosoftStsOAuth2Configuration config) {
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
        FakeAuthorizationResult authorizationResult = new FakeAuthorizationResult();
        ResultFuture<AuthorizationResult> future = new ResultFuture<>();
        future.setResult(authorizationResult);
        return future;
    }

    /**
     * @param request microsoft sts token request.
     * @return TokenResult
     * @throws IOException thrown when failed or interrupted I/O operations occur.
     */
    @Override
    public TokenResult requestToken(final MicrosoftStsTokenRequest request) throws IOException, ClientException {
        final String methodName = ":requestToken";

        Logger.verbose(
                TAG + methodName,
                "Requesting token..."
        );

        if (!request.getGrantType().equals(TokenRequest.GrantTypes.REFRESH_TOKEN)) {
            request.setGrantType(TokenRequest.GrantTypes.PASSWORD);
            validateTokenRequest(request);
        }

        return MockTokenResponse.getTokenResult();
    }

    @Override
    protected void validateTokenRequest(MicrosoftStsTokenRequest request) {
        if (StringUtil.isEmpty(request.getGrantType())) {
            throw new IllegalArgumentException(GRANT_TYPE_EMPTY_OR_NULL);
        }

        if (StringUtil.isEmpty(request.getScope())) {
            throw new IllegalArgumentException(SCOPE_EMPTY_OR_NULL);
        }

        if (StringUtil.isEmpty(request.getUsername())) {
            throw new IllegalArgumentException(USERNAME_EMPTY_OR_NULL);
        }

        if (StringUtil.isEmpty(request.getPassword())) {
            throw new IllegalArgumentException(PASSWORD_EMPTY_OR_NULL);
        }
    }

    @Override
    public MicrosoftStsTokenRequest createTokenRequest(@NonNull final MicrosoftStsAuthorizationRequest request,
                                                       @NonNull final MicrosoftStsAuthorizationResponse response) {
        final String methodName = ":createTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating TokenRequest..."
        );

        String username = request.getLoginHint();
        String password = "fake-password";

        MicrosoftStsTokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setUsername(username);
        tokenRequest.setPassword(password);
        tokenRequest.setClientId(request.getClientId());
        tokenRequest.setScope(request.getScope());

        try {
            tokenRequest.setCorrelationId(
                    UUID.fromString(
                            DiagnosticContext
                                    .getRequestContext()
                                    .get(DiagnosticContext.CORRELATION_ID)
                    )
            );
        } catch (IllegalArgumentException ex) {
            //We're not setting the correlation id if we can't parse it from the diagnostic context
            Logger.error(
                    "MicrosoftSTSOAuth2Strategy",
                    "Correlation id on diagnostic context is not a UUID.",
                    ex
            );
        }

        return tokenRequest;
    }

}
