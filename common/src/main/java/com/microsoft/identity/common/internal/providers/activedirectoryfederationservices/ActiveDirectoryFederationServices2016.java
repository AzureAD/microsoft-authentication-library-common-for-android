package com.microsoft.identity.common.internal.providers.activedirectoryfederationservices;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

/**
 * The Active Directory Federations Services 2016 Identity Provider Implementation
 */
public class ActiveDirectoryFederationServices2016 extends IdentityProvider {
    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2Configuration config) {
        return new ActiveDirectoryFederationServices2016OAuth2Strategy(config);
    }
}
