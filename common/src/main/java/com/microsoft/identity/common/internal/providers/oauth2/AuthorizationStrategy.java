package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Created by shoatman on 11/15/2017.
 */

public abstract class AuthorizationStrategy {
    public abstract AuthorizationResult requestAuthorization(AuthorizationRequest request);
}
