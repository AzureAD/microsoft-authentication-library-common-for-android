package com.microsoft.identity.common.adal.internal.providers;

/**
 * Created by shoatman on 11/15/2017.
 */

public abstract interface IdentityProvider {
    public abstract OAuth2Strategy CreateOAuth2Strategy();
}
