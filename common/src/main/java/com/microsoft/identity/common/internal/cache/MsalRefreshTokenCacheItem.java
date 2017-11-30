package com.microsoft.identity.common.internal.cache;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.EncodingUtil;

import java.net.URL;

/**
 * A lightweight representation of MSAL's cache item for refresh tokens.
 * <p>
 * This object knows how to create a cache key for itself.
 * <p>
 * If you call getCacheValue() on this object you get it as JSON.
 */
class MsalRefreshTokenCacheItem extends BaseMsalTokenCacheItem implements ISelfSerializingCacheItem {

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

    /**
     * Constructs a new MsalRefreshTokenCacheItem.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    public MsalRefreshTokenCacheItem(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        super(oAuth2Strategy, request, response);
        final Account account = oAuth2Strategy.createAccount(response);
        mRefreshToken = response.getRefreshToken();

        if (request instanceof AzureActiveDirectoryAuthorizationRequest) {
            final URL authority = ((AzureActiveDirectoryAuthorizationRequest) request).getAuthority();
            final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);
            mEnvironment = cloudEnv.getPreferredNetworkHostName();
        }

        if (account instanceof AzureActiveDirectoryAccount) {
            final AzureActiveDirectoryAccount aadAcct = (AzureActiveDirectoryAccount) account;
            mDisplayableId = aadAcct.getDisplayableId();
            mName = aadAcct.getName();
            mIdentityProvider = aadAcct.getIdentityProvider();
            mUserIdentifier = account.getUniqueIdentifier();
        }
    }

    @Override
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

    @Override
    public String getCacheValue() {
        return new Gson().toJson(this);
    }
}
