package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implements the IdentityProvider base class...
 */
public class AzureActiveDirectory extends IdentityProvider {

    private static ConcurrentMap<String, AzureActiveDirectoryCloud> sAadClouds = new ConcurrentHashMap<>();

    public OAuth2Strategy createOAuth2Strategy(){
        return new AzureActiveDirectoryOAuth2Strategy();
    }

    static boolean hasCloudHost(final URL authorityUrl) {
        return sAadClouds.containsKey(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static boolean isValidCloudHost(final URL authorityUrl) {
        return hasCloudHost(authorityUrl) && getAzureActiveDirectoryCloud(authorityUrl).isValidated();
    }

    static AzureActiveDirectoryCloud getAzureActiveDirectoryCloud(final URL authorityUrl) {
        return sAadClouds.get(authorityUrl.getHost().toLowerCase(Locale.US));
    }

}
