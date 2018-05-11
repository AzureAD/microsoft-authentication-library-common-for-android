package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;

/**
 * The Active Directory Federations Services 2016 Identity Provider Implementation
 */
public class ActiveDirectoryFederationServices2016
        extends IdentityProvider<ActiveDirectoryFederationServices2016OAuth2Strategy, OAuth2Configuration> {

    @Override
    public ActiveDirectoryFederationServices2016OAuth2Strategy createOAuth2Strategy(OAuth2Configuration config) {
        return new ActiveDirectoryFederationServices2016OAuth2Strategy(config);
    }

}
