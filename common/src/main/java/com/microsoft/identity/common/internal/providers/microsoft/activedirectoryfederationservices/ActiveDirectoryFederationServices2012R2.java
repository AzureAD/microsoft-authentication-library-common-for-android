package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;


/**
 * The Active Directory Federation Services 2012 R2 Identity Provider Implementation
 */
public class ActiveDirectoryFederationServices2012R2 extends IdentityProvider {
    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2Configuration config) {

        return new ActiveDirectoryFederationServices2012R2OAuth2Strategy(config);
    }
}
