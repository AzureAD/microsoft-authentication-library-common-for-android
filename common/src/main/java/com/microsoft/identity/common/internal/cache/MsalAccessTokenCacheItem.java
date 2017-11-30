package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.EncodingUtil;

class MsalAccessTokenCacheItem extends BaseMsalTokenCacheItem {

    @SerializedName("authority")
    String mAuthority;

    @SerializedName("access_token")
    String mAccessToken;

    @SerializedName("expires_on")
    long mExpiresOn;

    @SerializedName("scope")
    String mScope;

    @SerializedName("token_type")
    String mTokenType;

    @SerializedName("id_token")
    String mRawIdToken;

    transient String mUserIdentifier;

    public MsalAccessTokenCacheItem(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) {
        super(oAuth2Strategy, request, response);
        mAuthority = ""; // TODO where can I get this?
        mScope = request.getScope();
        mAccessToken = response.getAccessToken();
        if (response instanceof AzureActiveDirectoryTokenResponse) {
            mExpiresOn = ((AzureActiveDirectoryTokenResponse) response).getExpiresOn().getTime();
        }
        mTokenType = response.getTokenType();
        mRawIdToken = response.getIdToken();
        final Account account = oAuth2Strategy.createAccount(response);
        if (account instanceof AzureActiveDirectoryAccount) {
            final AzureActiveDirectoryAccount aadAcct = (AzureActiveDirectoryAccount) account;
            mUserIdentifier = ""; // TODO which unique identifier should I use?
        }
    }

    public String getCacheKey() {
        final String tokenCacheDelimiter = "$";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EncodingUtil.base64UrlEncodeToString(mAuthority));
        stringBuilder.append(tokenCacheDelimiter);
        stringBuilder.append(EncodingUtil.base64UrlEncodeToString(mClientId));
        stringBuilder.append(tokenCacheDelimiter);
        stringBuilder.append(EncodingUtil.base64UrlEncodeToString(mScope));
        stringBuilder.append(tokenCacheDelimiter);
        stringBuilder.append(mUserIdentifier);

        return stringBuilder.toString();
    }

    public String getCacheValue() {
        return new Gson().toJson(this);
    }
}
