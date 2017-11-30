package com.microsoft.identity.common.internal.cache;


import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * Abstract base class for MsalTokenCacheItems.
 */
public abstract class BaseMsalTokenCacheItem {

    @SerializedName("client_id")
    String mClientId;

    @SerializedName("client_info")
    String mRawClientInfo;

    @SerializedName("ver")
    String mVersion = "1";


    /**
     * Constructs a new BaseMsalTokenCacheItem.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
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
