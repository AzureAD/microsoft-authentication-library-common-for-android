package com.microsoft.identity.common.internal.cache;


import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public abstract class BaseMsalTokenCacheItem {

    @SerializedName("client_id")
    String mClientId;

    @SerializedName("client_info")
    String mRawClientInfo;

    @SerializedName("ver")
    String mVersion = "1";


    public BaseMsalTokenCacheItem(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        mClientId = request.getClientId();
        if (response instanceof AzureActiveDirectoryTokenResponse) {
            mRawClientInfo = ((AzureActiveDirectoryTokenResponse) response).getClientInfo();
        }
    }
}
