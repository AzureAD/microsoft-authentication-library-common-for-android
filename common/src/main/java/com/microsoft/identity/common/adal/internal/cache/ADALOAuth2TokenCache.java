// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.adal.internal.cache;

import android.net.Uri;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.adal.cache.CacheKey;
import com.microsoft.identity.common.java.adal.cache.DateTimeAdapter;
import com.microsoft.identity.common.java.cache.AccountDeletionRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Class responsible for saving oAuth2 Tokens for use in future requests.  Ideally this class would
 * work with any IDP; however ADAL only currently supports ADFS and AAD hence this class reflects that
 */
// Suppressing rawtype warnings due to the generic type IShareSingleSignOnState
@SuppressWarnings(WarningType.rawtype_warning)
public class ADALOAuth2TokenCache
        extends OAuth2TokenCache<AzureActiveDirectoryOAuth2Strategy, AzureActiveDirectoryAuthorizationRequest, AzureActiveDirectoryTokenResponse>
        implements IShareSingleSignOnState {
    private INameValueStorage<String> mISharedPreferencesFileManager;

    private static final String TAG = ADALOAuth2TokenCache.class.getSimpleName();
    private static final String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    private List<IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken>> mSharedSSOCaches;

    /**
     * Constructor of ADALOAuth2TokenCache.
     *
     * @param components Context
     */
    public ADALOAuth2TokenCache(final IPlatformComponents components) {
        super(components);
        Logger.verbose(TAG, "Init: " + TAG);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = new ArrayList<>();
    }

    /**
     * Constructor of ADALOAuth2TokenCache.
     *
     * @param context         Context
     * @param sharedSSOCaches List<IShareSingleSignOnState>
     */
    public ADALOAuth2TokenCache(final IPlatformComponents context,
                                final List<IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken>> sharedSSOCaches) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        //Logger.info(TAG, "Context is an Application? [" + (context instanceof Application) + "]");
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = sharedSSOCaches;
    }

    protected void initializeSharedPreferencesFileManager(final String fileName) {
        Logger.verbose(TAG, "Initializing SharedPreferencesFileManager");
        Logger.verbosePII(TAG, "Initializing with name: " + fileName);

        final IPlatformComponents components = getComponents();
        mISharedPreferencesFileManager =
                components.getEncryptedNameValueStore(
                        fileName,
                        components.getStorageEncryptionManager(),
                        String.class
                );
    }

    /**
     * Method responsible for saving tokens contained in the TokenResponse to storage.
     *
     * @param strategy
     * @param request
     * @param response
     */
    @Override
    public ICacheRecord save(
            final AzureActiveDirectoryOAuth2Strategy strategy,
            final AzureActiveDirectoryAuthorizationRequest request,
            final AzureActiveDirectoryTokenResponse response) throws ClientException {
        final String methodTag = TAG + ":save";

        final String issuerCacheIdentifier = strategy.getIssuerCacheIdentifier(request);
        final AzureActiveDirectoryAccount account = strategy.createAccount(response);
        final String msalEnvironment = Uri.parse(issuerCacheIdentifier).getAuthority();
        account.setEnvironment(msalEnvironment);
        final AzureActiveDirectoryRefreshToken refreshToken = strategy.getRefreshTokenFromResponse(response);
        refreshToken.setEnvironment(msalEnvironment);

        Logger.info(methodTag, "Constructing new ADALTokenCacheItem");
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem(strategy, request, response);
        logTokenCacheItem(cacheItem, methodTag);

        //There is more than one valid user identifier for some accounts... AAD Accounts as of this writing have 3
        Logger.info(methodTag, "Setting items to cache for user...");
        for (final String cacheIdentifier : account.getCacheIdentifiers()) {
            //Azure AD Uses Resource and Not Scope... but we didn't override... heads up
            final String scope = request.getScope();
            final String clientId = request.getClientId();

            Logger.infoPII(methodTag, "issuerCacheIdentifier: [" + issuerCacheIdentifier + "]");
            Logger.infoPII(methodTag, "scope: [" + scope + "]");
            Logger.infoPII(methodTag, "clientId: [" + clientId + "]");
            Logger.infoPII(methodTag, "cacheIdentifier: [" + cacheIdentifier + "]");

            setItemToCacheForUser(issuerCacheIdentifier, scope, clientId, cacheItem, cacheIdentifier);
        }

        //For legacy reasons creating a cache entry where the userid is null
        //ADAL supported a single user mode where it was not necessary for the developer to provide the user id
        //on calls to acquireTokenSilentAsync
        setItemToCacheForUser(issuerCacheIdentifier, request.getScope(), request.getClientId(), cacheItem, null);

        // TODO At some point, the type-safety of this call needs to get beefed-up
        Logger.info(methodTag, "Syncing SSO state to caches...");
        for (final IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken> sharedSsoCache : mSharedSSOCaches) {
            try {
                sharedSsoCache.setSingleSignOnState(account, refreshToken);
            } catch (final ClientException e) {
                Logger.errorPII(TAG,
                        "Exception setting single sign on state for account " + account.getUsername(),
                        e
                );
            } catch (final IllegalStateException e) {
                Logger.errorPII(TAG,
                        "Exception setting single sign on state for account " + account.getUsername(),
                        e
                );
                if (!AuthenticationSettings.INSTANCE.shouldIgnoreKeyLoaderNotFoundError()) {
                    throw e;
                }
            }
        }

        return null; // Returning null, since the ADAL cache's schema doesn't support this return type.
    }

    @Override
    public List<ICacheRecord> saveAndLoadAggregatedAccountData(AzureActiveDirectoryOAuth2Strategy oAuth2Strategy, AzureActiveDirectoryAuthorizationRequest request, AzureActiveDirectoryTokenResponse response) throws ClientException {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public ICacheRecord save(final AccountRecord accountRecord,
                             final IdTokenRecord idTokenRecord) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public ICacheRecord load(
            final String clientId,
            final String applicationIdentifier,
            final String target,
            final AccountRecord account,
            final AbstractAuthenticationScheme scheme) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<ICacheRecord> loadWithAggregatedAccountData(final String clientId,
                                                            final String applicationIdentifer,
                                                            final String target,
                                                            final AccountRecord account,
                                                            final AbstractAuthenticationScheme scheme) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public boolean removeCredential(Credential credential) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public AccountRecord getAccount(final String environment,
                                    final String clientId,
                                    final String homeAccountId,
                                    final String realm) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId, String homeAccountId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public AccountRecord getAccountByLocalAccountId(final String environment,
                                                    final String clientId,
                                                    final String localAccountId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(String environment, String clientId, String localAccountId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<AccountRecord> getAccounts(final String environment,
                                           final String clientId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<AccountRecord> getAllTenantAccountsForAccountByClientId(String clientId, AccountRecord accountRecord) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(final String environment,
                                                                   final String clientId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public List<IdTokenRecord> getIdTokensForAccountRecord(final String clientId,
                                                           final AccountRecord accountRecord) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public AccountDeletionRecord removeAccount(final String environment,
                                               final String clientId,
                                               final String homeAccountId,
                                               final String realm) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public AccountDeletionRecord removeAccount(final String environment,
                                               final String clientId,
                                               final String homeAccountId,
                                               final String realm,
                                               final CredentialType... typesToRemove) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public void clearAll() {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    protected Set<String> getAllClientIds() {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    @Override
    public AccountRecord getAccountByHomeAccountId(@Nullable final String environment,
                                                   @NonNull final String clientId,
                                                   @NonNull final String homeAccountId) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    private static void logTokenCacheItem(final ADALTokenCacheItem tokenCacheItem,
                                          @NonNull final String methodTag) {
        Logger.info(methodTag, "Logging TokenCacheItem");
        Logger.infoPII(methodTag, "resource: [" + tokenCacheItem.getResource() + "]");
        Logger.infoPII(methodTag, "authority: [" + tokenCacheItem.getAuthority() + "]");
        Logger.infoPII(methodTag, "clientId: [" + tokenCacheItem.getClientId() + "]");
        Logger.infoPII(methodTag, "expiresOn: [" + tokenCacheItem.getExpiresOn() + "]");
        Logger.infoPII(methodTag, "isMrrt: [" + tokenCacheItem.getIsMultiResourceRefreshToken() + "]");
        Logger.infoPII(methodTag, "tenantId: [" + tokenCacheItem.getTenantId() + "]");
        Logger.infoPII(methodTag, "foci: [" + tokenCacheItem.getFamilyClientId() + "]");
        Logger.infoPII(methodTag, "extendedExpires: [" + tokenCacheItem.getExtendedExpiresOn() + "]");
        Logger.infoPII(methodTag, "speRing: [" + tokenCacheItem.getSpeRing() + "]");
    }

    private void setItemToCacheForUser(final String issuer,
                                       final String resource,
                                       final String clientId,
                                       final ADALTokenCacheItem cacheItem,
                                       final String userId) {
        final String methodTag = TAG + ":setItemToCacheForUser";

        Logger.info(methodTag, "Setting cacheitem for RT entry.");
        setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId), cacheItem);

        if (cacheItem.getIsMultiResourceRefreshToken()) {
            Logger.info(methodTag, "CacheItem is an MRRT.");
            setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId), ADALTokenCacheItem.getAsMRRTTokenCacheItem(cacheItem));
        }

        if (!StringUtil.isNullOrEmpty(cacheItem.getFamilyClientId())) {
            Logger.info(methodTag, "CacheItem is an FRT.");
            setItem(CacheKey.createCacheKeyForFRT(issuer, cacheItem.getFamilyClientId(), userId), ADALTokenCacheItem.getAsFRTTokenCacheItem(cacheItem));
        }
    }

    private void setItem(final String key, final ADALTokenCacheItem cacheItem) {
        Logger.info(TAG + ":setItem", "Setting item to cache");
        String json = mGson.toJson(cacheItem);
        mISharedPreferencesFileManager.put(key, json);
    }

    private void validateSecretKeySetting() {
        Logger.verbose(TAG, "Validating secret key settings.");
        final byte[] secretKeyData = AuthenticationSettings.INSTANCE.getSecretKeyData();

        if (secretKeyData == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw new IllegalArgumentException("Secret key must be provided for API < 18. "
                    + "Use AuthenticationSettings.INSTANCE.setSecretKey()");
        }
    }

    @Override
    public void setSingleSignOnState(final BaseAccount account, final RefreshToken refreshToken) {
        // Unimplemented
        Logger.warn(TAG, "setSingleSignOnState was called, but is not implemented.");
    }

    @Override
    public RefreshToken getSingleSignOnState(final BaseAccount account) {
        // Unimplemented
        Logger.warn(TAG, "getSingleSignOnState was called, but is not implemented.");
        final RefreshToken refreshToken = null;
        return refreshToken;
    }

    public static String getAdalCacheFilename() {
        Logger.info(
                TAG + ":getAdalCacheFilename",
                "Getting ADAL cache file name..."
        );
        return SHARED_PREFERENCES_FILENAME;
    }
}
