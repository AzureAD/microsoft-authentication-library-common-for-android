package com.microsoft.identity.common.internal.providers;

import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

public abstract class IdentityProvider {
    public abstract OAuth2Strategy createOAuth2Strategy();
}
