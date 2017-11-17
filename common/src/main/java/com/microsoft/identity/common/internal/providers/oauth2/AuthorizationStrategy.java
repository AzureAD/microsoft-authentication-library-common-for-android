package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Abstracts the behavior associated with gathering a user authorization for an access token (oAuth)
 * and/or authentication information (OIDC)
 * Possible implementations include: EmbeddedWebViewAuthorizationStrategy, SystemWebViewAuthorizationStrategy, Device Code, etc...
 */
public abstract class AuthorizationStrategy {
    public abstract AuthorizationResult requestAuthorization(AuthorizationRequest request);
}
