package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * Create {@link Credential} instances.
 */
public interface ICredentialAdapter {

    AccessToken createAccessToken(OAuth2Strategy strategy,
                                  AuthorizationRequest request,
                                  TokenResponse response
    );

    RefreshToken createRefreshToken(OAuth2Strategy strategy,
                                    AuthorizationRequest request,
                                    TokenResponse response
    );

}
