package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.EncodingUtil;

class MsalRefreshTokenCacheItem extends BaseMsalTokenCacheItem {

    @SerializedName("refresh_token")
    private String mRefreshToken;

    @SerializedName("environment")
    private String mEnvironment;

    // meta data used to construct user object from refresh token cache item.
    @SerializedName("displayable_id")
    String mDisplayableId;

    @SerializedName("name")
    String mName;

    @SerializedName("identity_provider")
    String mIdentityProvider;

    transient String mUserIdentifier;

    public MsalRefreshTokenCacheItem(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) {
        super(oAuth2Strategy, request, response);
        final Account account = oAuth2Strategy.createAccount(response);
        mRefreshToken = response.getRefreshToken();
        mEnvironment = ""; // TODO where can I get this?
        if (account instanceof AzureActiveDirectoryAccount) {
            final AzureActiveDirectoryAccount aadAcct = (AzureActiveDirectoryAccount) account;
            mDisplayableId = aadAcct.getDisplayableId();
            mName = aadAcct.getName();
            mIdentityProvider = aadAcct.getIdentityProvider();
            mUserIdentifier = ""; // TODO which one of these do I use?
        }
    }

    public String getCacheKey() {
        final String tokenCacheDelimiter = "$";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EncodingUtil.base64UrlEncodeToString(mEnvironment));
        stringBuilder.append(tokenCacheDelimiter);
        stringBuilder.append(EncodingUtil.base64UrlEncodeToString(mClientId));
        stringBuilder.append(tokenCacheDelimiter);
        stringBuilder.append(mUserIdentifier);

        return stringBuilder.toString();
    }

    public String getCacheValue() {
        return new Gson().toJson(this);
    }
}
