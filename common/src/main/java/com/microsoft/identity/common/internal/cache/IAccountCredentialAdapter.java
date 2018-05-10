package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * Provides Adapters to the MsalOAuth2TokenCache.
 */
public interface IAccountCredentialAdapter
        <T extends OAuth2Strategy,
                U extends AuthorizationRequest,
                V extends TokenResponse,
                W extends com.microsoft.identity.common.Account,
                X extends com.microsoft.identity.common.internal.providers.oauth2.RefreshToken> {

    /**
     * Constructs an Account.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived Account.
     */
    Account createAccount(T strategy, U request, V response);

    /**
     * Constructs an AccessToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived AccessToken.
     */
    AccessToken createAccessToken(T strategy, U request, V response);

    /**
     * Constructs a RefreshToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived RefreshToken.
     */
    RefreshToken createRefreshToken(T strategy, U request, V response);

    /**
     * Constructs an IdToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived IdToken.
     */
    IdToken createIdToken(T strategy, U request, V response);

    /**
     * Adapter method to turn
     * {@link com.microsoft.identity.common.internal.providers.oauth2.RefreshToken} instances into
     * {@link RefreshToken}.
     *
     * @param refreshToken The RefreshToken to adapt.
     * @return The adapted RefreshToken.
     */
    RefreshToken asRefreshToken(X refreshToken);

    /**
     * Adapter method to turn {@link com.microsoft.identity.common.Account} instances into
     * {@link Account} instances.
     *
     * @param account The Account to adapt.
     * @return The adapted Account.
     */
    Account asAccount(W account);

    /**
     * Constructs IdToken instances from {@link com.microsoft.identity.common.Account} and
     * {@link com.microsoft.identity.common.internal.providers.oauth2.RefreshToken} instances.
     *
     * @param account      The Account to read.
     * @param refreshToken The RefreshToken to read.
     * @return The newly constructed IdToken.
     */
    IdToken asIdToken(W account, X refreshToken
    );
}
