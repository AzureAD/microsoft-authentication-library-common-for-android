package com.microsoft.identity.common.internal.providers.oauth2;


/**
 * A class for pulling the OpenIDConfiguratino document from the OpenID Provider server
 */
public class OpenIDProviderConfigurationClient {

    public OpenIDProviderConfiguration getOpenIDProviderConfiguration() {
        OpenIDProviderConfiguration config = new OpenIDProviderConfiguration();
        return config;
    }

}
