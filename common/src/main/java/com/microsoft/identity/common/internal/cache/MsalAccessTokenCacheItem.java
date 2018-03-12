package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.EncodingUtil;

/**
 * A lightweight representation of MSAL's cache item for access tokens.
 * <p>
 * This object knows how to create a cache key for itself.
 * <p>
 * If you call getCacheValue() on this object you get it as JSON.
 */
class MsalAccessTokenCacheItem extends BaseMsalTokenCacheItem implements ISelfSerializingCacheItem {

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

    /**
     * Constructs a new MsalAccessTokenCacheItem.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    public MsalAccessTokenCacheItem(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        super(oAuth2Strategy, request, response);
        mScope = request.getScope();
        mAccessToken = response.getAccessToken();
        mTokenType = response.getTokenType();
        mRawIdToken = response.getIdToken();

        if (request instanceof AzureActiveDirectoryAuthorizationRequest) {
            mAuthority = ((AzureActiveDirectoryAuthorizationRequest) request).getAuthority().toString();
        }

        if (response instanceof AzureActiveDirectoryTokenResponse) {
            mExpiresOn = ((AzureActiveDirectoryTokenResponse) response).getExpiresOn().getTime();
        }

        final Account account = oAuth2Strategy.createAccount(response);
        if (account instanceof AzureActiveDirectoryAccount) {
            final AzureActiveDirectoryAccount aadAcct = (AzureActiveDirectoryAccount) account;
            mUserIdentifier = aadAcct.getUniqueIdentifier();
        }
    }

    @Override
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

    @Override
    public String getCacheValue() {
        return new Gson().toJson(this);
    }

    public boolean matches(MsalAccessTokenCacheItem atCacheItem) {
        return mAuthority.equalsIgnoreCase(atCacheItem.mAuthority)
                && mClientId.equalsIgnoreCase(atCacheItem.mClientId)
                && mUserIdentifier.equals(atCacheItem.mUserIdentifier);
    }
}
