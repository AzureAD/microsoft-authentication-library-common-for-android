package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class AzureActiveDirectoryRefreshToken extends RefreshToken {
    public AzureActiveDirectoryRefreshToken(TokenResponse response) {
        super(response);

        AzureActiveDirectoryTokenResponse aadResponse;
        if(response instanceof  AzureActiveDirectoryTokenResponse) {
            aadResponse = (AzureActiveDirectoryTokenResponse)response;
            this.mFamilyId = aadResponse.getFamilyId();
            this.mIsFamilyRefreshToken = !StringExtensions.isNullOrBlank(this.mFamilyId);
        }else{
            throw new RuntimeException("Expected AzureActiveDirectoryTokenResponse in AzureActiveDirectoryAccessToken constructor");
        }
    }


    boolean mIsFamilyRefreshToken;
    String mFamilyId;

    public boolean getIsFamilyRefreshToken() {
        return mIsFamilyRefreshToken;
    }

    public String getFamilyId() {
        return mFamilyId;
    }




}
