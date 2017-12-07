package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Date;


public class AzureActiveDirectoryAccessToken extends AccessToken {

    Date mExpiresOn;
    Date mExtendedExpiresOn;

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

    public Date getExpiresOn() {
        return mExpiresOn;
    }

    public Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    //TODO: Need to add override for IsExpired() to address extended token expires on

}
