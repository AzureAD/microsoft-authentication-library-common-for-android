package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

public class AzureActiveDirectoryRefreshToken extends RefreshToken {

    boolean mIsFamilyRefreshToken;
    String mFamilyId;

    public AzureActiveDirectoryRefreshToken(AzureActiveDirectoryTokenResponse response) {
        super(response);
        this.mFamilyId = response.getFamilyId();
        this.mIsFamilyRefreshToken = !StringExtensions.isNullOrBlank(this.mFamilyId);
    }

    public boolean getIsFamilyRefreshToken() {
        return mIsFamilyRefreshToken;
    }

    public String getFamilyId() {
        return mFamilyId;
    }


}
