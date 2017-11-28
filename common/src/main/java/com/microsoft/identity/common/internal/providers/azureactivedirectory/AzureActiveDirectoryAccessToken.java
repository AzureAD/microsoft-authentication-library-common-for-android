package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;


public class AzureActiveDirectoryAccessToken extends AccessToken {

    String mExpiresOn;
    String mExtendedExpiresOn;

    public AzureActiveDirectoryAccessToken(TokenResponse response) {
        super(response);

        AzureActiveDirectoryTokenResponse aadResponse;
        if (response instanceof AzureActiveDirectoryTokenResponse) {
            aadResponse = (AzureActiveDirectoryTokenResponse) response;
            this.mExpiresOn = aadResponse.getExpiresOn();
            this.mExtendedExpiresOn = aadResponse.getExtExpiresOn();
        } else {
            throw new RuntimeException("Expected AzureActiveDirectoryTokenResponse in AzureActiveDirectoryAccessToken constructor");
        }
    }

    public String getExpiresOn() {
        return mExpiresOn;
    }

    //TODO: Need to add override for IsExpired() to address extended token expires on

}
