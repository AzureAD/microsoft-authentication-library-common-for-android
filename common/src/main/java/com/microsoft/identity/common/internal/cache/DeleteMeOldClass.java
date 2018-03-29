package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.EncodingUtil;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cache representation for MSAL.
 */
// TODO delete!
public class DeleteMeOldClass extends OAuth2TokenCache implements IShareSingleSignOnState {

    // SharedPreferences used to store tokens
    private final SharedPreferencesFileManager mAccessTokenSharedPreferences;
    private final ISharedPreferencesFileManager mRefreshTokenSharedPreferences;

    // The names of the SharedPreferences files on disk
    private static final String sAccessTokenSharedPreferences = "com.microsoft.identity.client.token";
    private static final String sRefreshTokenSharedPreferences = "com.microsoft.identity.client.refreshToken";

    private List<IShareSingleSignOnState> mSharedSSOCaches;

    /**
     * Constructs a new DeleteMeOldClass.
     *
     * @param context The Application consuming this library.
     */
    public DeleteMeOldClass(Context context, List<IShareSingleSignOnState> sharedSSOCaches) {
        super(context);
        mAccessTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sAccessTokenSharedPreferences);
        mRefreshTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sRefreshTokenSharedPreferences);
        mSharedSSOCaches = sharedSSOCaches;
    }

    /**
     * Constructs a new DeleteMeOldClass.
     *
     * @param context The Application consuming this library.
     */
    public DeleteMeOldClass(Context context) {
        super(context);
        mAccessTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sAccessTokenSharedPreferences);
        mRefreshTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sRefreshTokenSharedPreferences);
        mSharedSSOCaches = new ArrayList<>();
    }

    @Override
    public void setSingleSignOnState(Account account, RefreshToken refreshToken) {
        // TODO implement
    }

    @Override
    public RefreshToken getSingleSignOnState(Account account) {
        // TODO implement
        return null;
    }

    @Override
    public void saveTokens(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        saveAccessToken(oAuth2Strategy, request, response);
        saveRefreshToken(oAuth2Strategy, request, response);
    }

    /**
     * Saves the accessToken to the cache.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    private void saveAccessToken(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        final MsalAccessTokenCacheItem cacheItem = new MsalAccessTokenCacheItem(oAuth2Strategy, request, response);
        deleteAccessTokensWithIntersectingScopes(cacheItem);
        final String atCacheKey = cacheItem.getCacheKey();
        final String atCacheValue = cacheItem.getCacheValue();
        mAccessTokenSharedPreferences.putString(atCacheKey, atCacheValue);
    }

    private void deleteAccessTokensWithIntersectingScopes(final MsalAccessTokenCacheItem cacheItem) {
        List<MsalAccessTokenCacheItem> atCacheItems = getAllAccessTokensForClientId(cacheItem.mClientId);
        for (final MsalAccessTokenCacheItem atCacheItem : atCacheItems) {
            if (cacheItem.matches(atCacheItem) && scopesIntersect(cacheItem, atCacheItem)) {
                deleteAccessToken(atCacheItem);
            }
        }
    }

    private void deleteAccessToken(MsalAccessTokenCacheItem atCacheItem) {
        mAccessTokenSharedPreferences.remove(atCacheItem.getCacheKey());
    }

    private boolean scopesIntersect(
            final MsalAccessTokenCacheItem cacheItem1,
            final MsalAccessTokenCacheItem cacheItem2) {
        final Set<String> item1Scopes = scopesAsSet(cacheItem1);
        final Set<String> item2Scopes = scopesAsSet(cacheItem2);
        for (final String scope : item2Scopes) {
            if (item1Scopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> scopesAsSet(final MsalAccessTokenCacheItem cacheItem) {
        final Set<String> scopeSet = new HashSet<>();
        final String scopeString = cacheItem.mScope;
        final String[] scopeArray = scopeString.split("\\s+");
        scopeSet.addAll(Arrays.asList(scopeArray));
        return scopeSet;
    }

    private List<MsalAccessTokenCacheItem> getAllAccessTokensForClientId(final String clientId) {
        final List<MsalAccessTokenCacheItem> allATs = getAllAccessTokens();
        final List<MsalAccessTokenCacheItem> allATsForClientId = new ArrayList<>();
        for (final MsalAccessTokenCacheItem atItem : allATs) {
            if (clientId.equalsIgnoreCase(atItem.mClientId)) {
                allATsForClientId.add(atItem);
            }
        }
        return Collections.unmodifiableList(allATsForClientId);
    }

    private List<MsalAccessTokenCacheItem> getAllAccessTokens() {
        final Map<String, ?> atMap = mAccessTokenSharedPreferences.getAll();
        final Collection<?> accessTokensAsUnknowns = atMap.values();
        final List<MsalAccessTokenCacheItem> atCacheItems = new ArrayList<>();

        for (final Object atUnknown : accessTokensAsUnknowns) {
            if (atUnknown instanceof String) {
                final MsalAccessTokenCacheItem atCacheItem = new Gson().fromJson((String) atUnknown, MsalAccessTokenCacheItem.class);
                initUserIdentifier(atCacheItem);
                atCacheItems.add(atCacheItem);
            }
        }

        return atCacheItems;
    }

    private void initUserIdentifier(MsalAccessTokenCacheItem atCacheItem) {
        // set up the uniqueIdentifier here...
        final String rawClientInfo = atCacheItem.mRawClientInfo;
        try {
            final Map<String, String> clientInfoItems = parseRawClientInfo(rawClientInfo);
            String uid = clientInfoItems.get(ClientInfo.UNIQUE_IDENTIFIER);
            String utid = clientInfoItems.get(ClientInfo.UNIQUE_TENANT_IDENTIFIER);
            atCacheItem.mUserIdentifier =
                    EncodingUtil.base64UrlEncodeToString(uid)
                            + "."
                            + EncodingUtil.base64UrlEncodeToString(utid);
        } catch (final JSONException e) {
            throw new RuntimeException("Failed to deserialize user identifier from cache.");
        }
    }

    private Map<String, String> parseRawClientInfo(final String rawClientInfo) throws JSONException {
        final String decodedClientInfo = new String(Base64.decode(rawClientInfo, Base64.URL_SAFE), Charset.forName(StringExtensions.ENCODING_UTF8));
        return JsonExtensions.extractJsonObjectIntoMap(decodedClientInfo);
    }

    /**
     * Saves the refreshToken to the cache.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    private void saveRefreshToken(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        final ISelfSerializingCacheItem cacheItem = new MsalRefreshTokenCacheItem(oAuth2Strategy, request, response);
        final String rtCacheKey = cacheItem.getCacheKey();
        final String rtCacheValue = cacheItem.getCacheValue();
        if (!StringExtensions.isNullOrBlank(response.getRefreshToken())) {
            mRefreshTokenSharedPreferences.putString(rtCacheKey, rtCacheValue);
        }
    }
}
