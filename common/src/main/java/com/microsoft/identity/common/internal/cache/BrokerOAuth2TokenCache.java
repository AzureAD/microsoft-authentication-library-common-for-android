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
package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.ADALOAuth2TokenCache.ERR_UNSUPPORTED_OPERATION;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

/**
 * "Combined" cache implementation to cache tokens inside of the broker.
 * <p>
 * This cache is really a container for other caches. It contains:
 * 1 Family of Client ID cache (FOCI)
 * <p>
 * 0 or more app-specific caches for use when an application is not a member of the family.
 * <p>
 * Operations performed on the BrokerOAuth2TokenCache are designed to route the caller to the
 * proper data store: a good example of this is when calling save(). Save() will inspect the contents
 * of the response and determine if the payload contains a family id. If it does, the Account and
 * any associated credentials are written to the FOCI cache and nowhere else.
 * <p>
 * The reverse is true for non-family apps: if the response does not contain a family id, then the
 * account and credentials are written to the app specific cache.
 * <p>
 * Some operations will be performed on multiple caches; a good example of this is the
 * removeAccountFromDevice() API. This call affects multiple caches by iterating over the family
 * and app-specific caches to locate occurrences of an Account: if it is found, the account
 * and corresponding credential entries are removed.
 *
 * @param <GenericOAuth2Strategy>       The strategy type to use.
 * @param <GenericAuthorizationRequest> The AuthorizationRequest type to use.
 * @param <GenericTokenResponse>        The TokenResponse type to use.
 * @param <GenericAccount>              The Account type to use.
 * @param <GenericRefreshToken>         The RefreshToken type to use.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class BrokerOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends MicrosoftTokenResponse,
                GenericAccount extends MicrosoftAccount,
                GenericRefreshToken extends MicrosoftRefreshToken>
        extends OAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse> {

    private static final String TAG = BrokerOAuth2TokenCache.class.getSimpleName();

    private static final String UNCHECKED = "unchecked";

    private final IBrokerApplicationMetadataCache mApplicationMetadataCache;
    private final MicrosoftFamilyOAuth2TokenCache mFociCache;
    private final int mCallingProcessUid;
    private ProcessUidCacheFactory mDelegate = null;

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context                  The current application context.
     * @param callingProcessUid        The UID of the current broker-calling app.
     * @param applicationMetadataCache The metadata cache to use.
     */
    public BrokerOAuth2TokenCache(@NonNull final Context context,
                                  int callingProcessUid,
                                  @NonNull IBrokerApplicationMetadataCache applicationMetadataCache) {
        super(context);

        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );

        mCallingProcessUid = callingProcessUid;
        mFociCache = initializeFociCache(context);
        mApplicationMetadataCache = applicationMetadataCache;
    }

    /**
     * Interface used to inject process-uid based caches into the broker.
     */
    @VisibleForTesting
    public interface ProcessUidCacheFactory {

        /**
         * Returns an instance of the {@link MsalOAuth2TokenCache} for the supplied params.
         *
         * @param context           The application context to use.
         * @param bindingProcessUid The process UID of the current binding-app.
         * @return
         */
        MsalOAuth2TokenCache getTokenCache(final Context context, final int bindingProcessUid);

    }

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context   The current application context.
     * @param fociCache The FOCI cache implementation to use.
     */
    @VisibleForTesting
    public BrokerOAuth2TokenCache(@NonNull Context context,
                                  final int callingProcessUid,
                                  @NonNull IBrokerApplicationMetadataCache applicationMetadataCache,
                                  @NonNull ProcessUidCacheFactory delegate,
                                  @NonNull final MicrosoftFamilyOAuth2TokenCache fociCache) {
        super(context);

        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );

        mDelegate = delegate;
        mApplicationMetadataCache = applicationMetadataCache;
        mCallingProcessUid = callingProcessUid;
        mFociCache = fociCache;
    }

    /**
     * Broker-only API to persist WPJ's Accounts & their associated credentials.
     *
     * @param accountRecord     The {@link AccountRecord} to store.
     * @param idTokenRecord     The {@link IdTokenRecord} to store.
     * @param accessTokenRecord The {@link AccessTokenRecord} to store.
     * @param familyId          The family_id or null, if not applicable.
     * @return The {@link ICacheRecord} result of this save action.
     * @throws ClientException If the supplied Accounts or Credentials are schema invalid.
     */
    public ICacheRecord save(@NonNull AccountRecord accountRecord,
                             @NonNull IdTokenRecord idTokenRecord,
                             @NonNull AccessTokenRecord accessTokenRecord,
                             @Nullable String familyId) throws ClientException {
        final String methodName = ":save";

        final ICacheRecord result;

        final boolean isFoci = !StringExtensions.isNullOrBlank(familyId);

        Logger.info(
                TAG + methodName,
                "Saving to FOCI cache? ["
                        + isFoci
                        + "]"
        );

        if (isFoci) {
            // Save to the foci cache....
            result = mFociCache.save(
                    accountRecord,
                    idTokenRecord,
                    accessTokenRecord
            );
        } else {
            // Save to the processUid cache... or create a new one
            MsalOAuth2TokenCache targetCache = getTokenCacheForClient(
                    idTokenRecord.getClientId(),
                    idTokenRecord.getEnvironment(),
                    mCallingProcessUid
            );

            if (null == targetCache) {
                Logger.warn(
                        TAG + methodName,
                        "Existing cache not found. A new one will be created."
                );

                targetCache = initializeProcessUidCache(
                        getContext(),
                        mCallingProcessUid
                );
            }

            result = targetCache.save(
                    accountRecord,
                    idTokenRecord,
                    accessTokenRecord
            );
        }

        updateApplicationMetadataCache(
                result.getAccessToken().getClientId(),
                result.getAccessToken().getEnvironment(),
                familyId,
                mCallingProcessUid
        );

        return result;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<ICacheRecord> saveAndLoadAggregatedAccountData(
            @NonNull final AccountRecord accountRecord,
            @NonNull final IdTokenRecord idTokenRecord,
            @NonNull final AccessTokenRecord accessTokenRecord,
            @Nullable final String familyId,
            @NonNull final AbstractAuthenticationScheme authScheme) throws ClientException {
        synchronized (this) {
            final ICacheRecord cacheRecord = save(
                    accountRecord,
                    idTokenRecord,
                    accessTokenRecord,
                    familyId
            );

            final String clientId = cacheRecord.getAccessToken().getClientId();
            final String target = cacheRecord.getAccessToken().getTarget();
            final String environment = cacheRecord.getAccessToken().getEnvironment();

            // Now get the cache we just saved to....
            final MsalOAuth2TokenCache cache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            return (List<ICacheRecord>) cache.loadWithAggregatedAccountData(
                    clientId,
                    target,
                    cacheRecord.getAccount(),
                    authScheme
            );
        }
    }

    @Override
    public ICacheRecord save(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                             @NonNull final GenericAuthorizationRequest request,
                             @NonNull final GenericTokenResponse response) throws ClientException {
        final String methodName = ":save";

        final boolean isFoci = !StringExtensions.isNullOrBlank(response.getFamilyId());

        if (isFoci) {
            Logger.verbose(
                    TAG + methodName,
                    "Received FOCI value: ["
                            + response.getFamilyId()
                            + "]"
            );
        }

        Logger.info(
                TAG + methodName,
                "Saving to FOCI cache? ["
                        + isFoci
                        + "]"
        );

        OAuth2TokenCache targetCache;

        if (isFoci) {
            targetCache = mFociCache;
        } else {
            // Try to find an existing cache for this application
            targetCache = getTokenCacheForClient(
                    request.getClientId(),
                    oAuth2Strategy.getIssuerCacheIdentifier(request),
                    mCallingProcessUid
            );

            if (null == targetCache) {// No existing cache could be found... Make a new one!
                Logger.warn(
                        TAG + methodName,
                        "Existing cache not found. A new one will be created."
                );
                targetCache = initializeProcessUidCache(
                        getContext(),
                        mCallingProcessUid
                );
            }
        }

        final ICacheRecord result = targetCache.save(
                oAuth2Strategy,
                request,
                response
        );

        updateApplicationMetadataCache(
                result.getRefreshToken().getClientId(),
                result.getRefreshToken().getEnvironment(),
                result.getRefreshToken().getFamilyId(),
                mCallingProcessUid
        );

        return result;
    }

    @Override
    @SuppressWarnings(UNCHECKED)
    public List<ICacheRecord> saveAndLoadAggregatedAccountData(
            @NonNull final GenericOAuth2Strategy oAuth2Strategy,
            @NonNull final GenericAuthorizationRequest request,
            @NonNull final GenericTokenResponse response) throws ClientException {
        synchronized (this) {
            final String methodName = ":saveAndLoadAggregatedAccountData";

            final boolean isFoci = !StringExtensions.isNullOrBlank(response.getFamilyId());

            OAuth2TokenCache targetCache;

            Logger.info(
                    TAG + methodName,
                    "Saving to FOCI cache? ["
                            + isFoci
                            + "]"
            );

            if (isFoci) {
                targetCache = mFociCache;
            } else {
                targetCache = getTokenCacheForClient(
                        request.getClientId(),
                        oAuth2Strategy.getIssuerCacheIdentifier(request),
                        mCallingProcessUid
                );

                if (null == targetCache) {
                    Logger.warn(
                            TAG + methodName,
                            "Existing cache not found. A new one will be created."
                    );
                    targetCache = initializeProcessUidCache(
                            getContext(),
                            mCallingProcessUid
                    );
                }
            }

            final List<ICacheRecord> result = targetCache.saveAndLoadAggregatedAccountData(
                    oAuth2Strategy,
                    request,
                    response
            );

            // The 0th element contains the record we *just* saved. Other records are corollary data.
            final ICacheRecord justSavedRecord = result.get(0);

            updateApplicationMetadataCache(
                    justSavedRecord.getRefreshToken().getClientId(),
                    justSavedRecord.getRefreshToken().getEnvironment(),
                    justSavedRecord.getRefreshToken().getFamilyId(),
                    mCallingProcessUid
            );

            return result;
        }
    }

    private void updateApplicationMetadataCache(@NonNull final String clientId,
                                                @NonNull final String environment,
                                                @Nullable final String familyId,
                                                int callingProcessUid) {
        final String methodName = ":updateApplicationMetadataCache";

        final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
        applicationMetadata.setClientId(clientId);
        applicationMetadata.setEnvironment(environment);
        applicationMetadata.setFoci(familyId);
        applicationMetadata.setUid(callingProcessUid);

        Logger.verbose(
                TAG + methodName,
                "Adding cache entry for clientId: ["
                        + clientId
                        + "]"
        );

        final boolean success = mApplicationMetadataCache.insert(applicationMetadata);

        Logger.info(
                TAG + methodName,
                "Cache updated successfully? ["
                        + success
                        + "]"
        );
    }

    @Override
    public ICacheRecord save(@NonNull final AccountRecord accountRecord,
                             @NonNull final IdTokenRecord idTokenRecord) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * The caller of this function should inspect the result carefully.
     * <p>
     * If the result contains an AccountRecord, IdTokenRecord, AccessTokenRecord, and
     * RefreshTokenRecord then the result is OK to use. The caller should still check the expiry of
     * the AccessTokenRecord before returning the result to the caller, refreshing as necessary...
     * <p>
     * If the result contains only an AccountRecord then we had no tokens in the cache and the
     * library should do some equivalent of AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED
     * <p>
     * If the result contains only an AccountRecord and RefreshTokenRecord then the caller should attempt to refresh
     * the access token. If it works, call BrokerOAuth2TokenCache#save() with the result. If it
     * fails, throw some equivalent of AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED
     *
     * @param clientId The ClientId of the current app.
     * @param target   The 'target' (scopes) the requested token should contain.
     * @param account  The Account whose Credentials should be loaded.
     * @return
     */
    @Override
    public ICacheRecord load(@NonNull final String clientId,
                             @Nullable final String target,
                             @NonNull final AccountRecord account,
                             @NonNull final AbstractAuthenticationScheme authScheme) {
        final String methodName = ":load";

        Logger.verbose(
                TAG + methodName,
                "Performing lookup in app-specific cache."
        );

        final BrokerApplicationMetadata appMetadata = mApplicationMetadataCache.getMetadata(
                clientId,
                account.getEnvironment(),
                mCallingProcessUid
        );

        boolean isKnownFoci = false;

        if (null != appMetadata) {
            isKnownFoci = null != appMetadata.getFoci();

            Logger.info(
                    TAG + methodName,
                    "App is known foci? " + isKnownFoci
            );
        }

        final OAuth2TokenCache targetCache = getTokenCacheForClient(
                clientId,
                account.getEnvironment(),
                mCallingProcessUid
        );

        final boolean shouldUseFociCache = null == targetCache || isKnownFoci;

        Logger.info(
                TAG + methodName,
                "Loading from FOCI cache? ["
                        + shouldUseFociCache
                        + "]"
        );

        ICacheRecord resultRecord;

        if (shouldUseFociCache) {
            resultRecord = mFociCache.loadByFamilyId(
                    clientId,
                    target,
                    account,
                    authScheme
            );
        } else {
            resultRecord = targetCache.load(
                    clientId,
                    target,
                    account,
                    authScheme
            );
        }

        final boolean resultFound = null != resultRecord.getRefreshToken();

        Logger.verbose(
                TAG + methodName,
                "Result found? ["
                        + resultFound
                        + "]"
        );

        return resultRecord;
    }

    /**
     * The caller of this method should inspect the result carefully.
     * <p>
     * If the result contains >1 element: tokens were found for the provided filter criteria and
     * additionally, tokens were found for this Account relative to a guest tenant.
     * <p>
     * If the result contains exactly 1 element, you may receive 1 of a few different
     * response payloads, depending on cache state...
     * <p>
     * If the result contains an AccountRecord, IdTokenRecord, AccessTokenRecord, and
     * RefreshTokenRecord then the result is OK to use. The caller should still check the expiry of
     * the AccessTokenRecord before returning the result to the caller, refreshing as necessary...
     * <p>
     * If the result contains only an AccountRecord then we had no tokens in the cache and the
     * library should do some equivalent of AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED
     * <p>
     * If the result contains only an AccountRecord and RefreshTokenRecord then the caller should attempt to refresh
     * the access token. If it works, call BrokerOAuth2TokenCache#save() with the result. If it
     * fails, throw some equivalent of AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED
     *
     * @param clientId The ClientId of the current app.
     * @param target   The 'target' (scopes) the requested token should contain.
     * @param account  The Account whose Credentials should be loaded.
     * @return A List of ICacheRecords for the supplied filter criteria.
     */
    @SuppressWarnings(UNCHECKED)
    @Override
    public List<ICacheRecord> loadWithAggregatedAccountData(@NonNull final String clientId,
                                                            @Nullable final String target,
                                                            @NonNull final AccountRecord account,
                                                            @NonNull final AbstractAuthenticationScheme authScheme) {
        synchronized (this) {
            final String methodName = ":loadWithAggregatedAccountData";

            final BrokerApplicationMetadata appMetadata = mApplicationMetadataCache.getMetadata(
                    clientId,
                    account.getEnvironment(),
                    mCallingProcessUid
            );

            boolean isKnownFoci = false;

            if (null != appMetadata) {
                isKnownFoci = null != appMetadata.getFoci();

                Logger.info(
                        TAG + methodName,
                        "App is known foci? " + isKnownFoci
                );
            }

            final OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    account.getEnvironment(),
                    mCallingProcessUid
            );

            final boolean appIsUnknownUseFociAsFallback = null == targetCache;

            final List<ICacheRecord> resultRecords;

            Logger.info(
                    TAG + methodName,
                    "Loading from FOCI cache? ["
                            + (isKnownFoci || appIsUnknownUseFociAsFallback)
                            + "]"
            );

            if (appIsUnknownUseFociAsFallback) {
                // We do not have a cache for this app or it is not yet known to be a member of the family
                // use the foci cache....

                // Load a sparse-record (if available) containing only the desired account and a
                // refresh token if available...
                resultRecords = new ArrayList<>();
                resultRecords.add(
                        mFociCache.loadByFamilyId(
                                clientId,
                                target,
                                account,
                                authScheme
                        )
                );
            } else if (isKnownFoci) {
                resultRecords =
                        mFociCache.loadByFamilyIdWithAggregatedAccountData(
                                clientId,
                                target,
                                account,
                                authScheme
                        );
            } else {
                resultRecords = targetCache.loadWithAggregatedAccountData(
                        clientId,
                        target,
                        account,
                        authScheme
                );
            }

            final boolean resultFound = !resultRecords.isEmpty()
                    && null != resultRecords.get(0).getRefreshToken();

            Logger.verbose(
                    TAG + methodName,
                    "Result found? ["
                            + resultFound
                            + "]"
            );

            return resultRecords;
        }
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credential) {
        final String methodName = ":removeCredential";

        final OAuth2TokenCache targetCache = getTokenCacheForClient(
                credential.getClientId(),
                credential.getEnvironment(),
                mCallingProcessUid
        );

        boolean removed = false;

        if (null != targetCache) {
            removed = targetCache.removeCredential(credential);
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Could not remove credential. Cache not found."
            );
        }

        Logger.verbose(
                TAG + methodName,
                "Credential removed? ["
                        + removed
                        + "]"
        );

        return removed;
    }

    @Override
    @Nullable
    public AccountRecord getAccount(@Nullable final String environment,
                                    @NonNull final String clientId,
                                    @NonNull final String homeAccountId,
                                    @Nullable final String realm) {
        final String methodName = ":getAccount";

        OAuth2TokenCache targetCache = null;

        AccountRecord result = null;

        if (null != environment) {
            targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            if (null == targetCache) {
                Logger.verbose(
                        TAG + methodName,
                        "Target cache was null. Using FOCI cache."
                );

                targetCache = mFociCache;
            }

            result = targetCache.getAccount(
                    environment,
                    clientId,
                    homeAccountId,
                    realm
            );
        } else {
            // We need to check all of the caches that match the supplied client id
            // If none match, return null...
            final List<OAuth2TokenCache> clientIdTokenCaches = getTokenCachesForClientId(
                    clientId
            );

            final Iterator<OAuth2TokenCache> cacheIterator = clientIdTokenCaches.iterator();

            while (null == result && cacheIterator.hasNext()) {
                result = cacheIterator
                        .next()
                        .getAccount(
                                environment,
                                clientId,
                                homeAccountId,
                                realm
                        );
            }
        }

        return result;
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(
            @Nullable final String environment,
            @NonNull final String clientId,
            @NonNull final String homeAccountId) {
        final String methodName = ":getAccountsWithAggregatedAccountData";

        final List<ICacheRecord> result;
        OAuth2TokenCache targetCache;

        if (null != environment) {
            targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            if (null == targetCache) {
                Logger.verbose(
                        TAG + methodName,
                        "Falling back to FoCI cache..."
                );

                targetCache = mFociCache;
            }

            result = targetCache.getAccountsWithAggregatedAccountData(
                    environment,
                    clientId,
                    homeAccountId
            );
        } else {
            // If no environment was specified, return all of the accounts across all of the envs...
            // Callers should really specify an environment...
            final List<OAuth2TokenCache> caches = getTokenCachesForClientId(clientId);

            // Declare a new List to which we will add all of our results...
            result = new ArrayList<>();

            for (final OAuth2TokenCache cache : caches) {
                result.addAll(
                        cache.getAccountsWithAggregatedAccountData(
                                environment,
                                clientId,
                                homeAccountId
                        )
                );
            }
        }

        return result;
    }

    private List<OAuth2TokenCache> getTokenCachesForClientId(@NonNull final String clientId) {
        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        final List<OAuth2TokenCache> result = new ArrayList<>();
        boolean containsFoci = false;

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (clientId.equals(metadata.getClientId())) {
                if (null != metadata.getFoci() && !containsFoci) {
                    // Add the foci cache, but only once...
                    result.add(mFociCache);
                    containsFoci = true;
                } else {
                    // App is not foci, see if we can find its real cache...
                    final OAuth2TokenCache candidateCache = getTokenCacheForClient(
                            metadata.getClientId(),
                            metadata.getEnvironment(),
                            mCallingProcessUid
                    );

                    if (null != candidateCache) {
                        result.add(candidateCache);
                    }
                }
            }
        }

        return result;
    }

    @Override
    @Nullable
    public AccountRecord getAccountByLocalAccountId(@Nullable final String environment,
                                                    @NonNull final String clientId,
                                                    @NonNull final String localAccountId) {
        final String methodName = ":getAccountByLocalAccountId";

        Logger.verbose(
                TAG + methodName,
                "Loading account by local account id."
        );

        if (null != environment) {
            OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            Logger.info(
                    TAG + methodName,
                    "Loading from FOCI cache? ["
                            + (targetCache == null)
                            + "]"
            );

            if (null != targetCache) {
                return targetCache.getAccountByLocalAccountId(
                        environment,
                        clientId,
                        localAccountId
                );
            } else {
                return mFociCache.getAccountByLocalAccountId(
                        environment,
                        clientId,
                        localAccountId
                );
            }
        } else {
            AccountRecord result = null;

            final List<OAuth2TokenCache> cachesToInspect = getTokenCachesForClientId(clientId);
            final Iterator<OAuth2TokenCache> cacheIterator = cachesToInspect.iterator();

            while (null == result && cacheIterator.hasNext()) {
                result = cacheIterator
                        .next()
                        .getAccountByLocalAccountId(
                                environment,
                                clientId,
                                localAccountId
                        );
            }

            return result;
        }
    }

    @Override
    @Nullable
    public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(
            @Nullable final String environment,
            @NonNull final String clientId,
            @NonNull final String localAccountId) {
        final String methodName = ":getAccountWithAggregatedAccountDataByLocalAccountId";
        if (null != environment) {
            OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            Logger.info(
                    TAG + methodName,
                    "Loading from FOCI cache? ["
                            + (targetCache == null)
                            + "]"
            );

            if (null != targetCache) {
                return targetCache.getAccountWithAggregatedAccountDataByLocalAccountId(
                        environment,
                        clientId,
                        localAccountId
                );
            } else {
                return mFociCache.getAccountWithAggregatedAccountDataByLocalAccountId(
                        environment,
                        clientId,
                        localAccountId
                );
            }
        } else {
            ICacheRecord result = null;

            final List<OAuth2TokenCache> cachesToInspect = getTokenCachesForClientId(clientId);
            final Iterator<OAuth2TokenCache> cacheIterator = cachesToInspect.iterator();

            while (null == result && cacheIterator.hasNext()) {
                result = cacheIterator
                        .next()
                        .getAccountWithAggregatedAccountDataByLocalAccountId(
                                environment,
                                clientId,
                                localAccountId
                        );
            }

            return result;
        }
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public List<AccountRecord> getAccounts(@Nullable final String environment,
                                           @NonNull final String clientId) {
        final String methodName = ":getAccounts (2 param)";
        final List<AccountRecord> result = new ArrayList<>();

        if (null != environment) {
            OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            if (null != targetCache) {
                result.addAll(targetCache.getAccounts(environment, clientId));
            } else {
                Logger.warn(
                        TAG + methodName,
                        "No caches to inspect."
                );
            }
        } else {
            final List<OAuth2TokenCache> cachesToInspect = getTokenCachesForClientId(clientId);

            for (final OAuth2TokenCache cache : cachesToInspect) {
                result.addAll(
                        cache.getAccounts(
                                environment,
                                clientId
                        )
                );
            }

            Logger.verbose(
                    TAG + methodName,
                    "Found ["
                            + result.size()
                            + "] accounts."
            );
        }

        return result;
    }

    @Override
    public List<AccountRecord> getAllTenantAccountsForAccountByClientId(@NonNull final String clientId,
                                                                        @NonNull final AccountRecord accountRecord) {
        final OAuth2TokenCache cache = getTokenCacheForClient(
                clientId,
                accountRecord.getEnvironment(),
                mCallingProcessUid
        );

        return cache.getAllTenantAccountsForAccountByClientId(
                clientId,
                accountRecord
        );
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(@Nullable String environment,
                                                                   @NonNull String clientId) {
        final String methodName = ":getAccountsWithAggregatedAccountData";

        final List<ICacheRecord> result;
        OAuth2TokenCache targetCache;

        if (null != environment) {
            targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            if (null == targetCache) {
                Logger.verbose(
                        TAG + methodName,
                        "Falling back to FoCI cache..."
                );

                targetCache = mFociCache;
            }

            result = targetCache.getAccountsWithAggregatedAccountData(environment, clientId);
        } else {
            // If no environment was specified, return all of the accounts across all of the envs...
            // Callers should really specify an environment...
            final List<OAuth2TokenCache> caches = getTokenCachesForClientId(clientId);

            // Declare a new List to which we will add all of our results...
            result = new ArrayList<>();

            for (final OAuth2TokenCache cache : caches) {
                result.addAll(cache.getAccountsWithAggregatedAccountData(environment, clientId));
            }
        }

        return result;
    }

    @Override
    public List<IdTokenRecord> getIdTokensForAccountRecord(@NonNull final String clientId,
                                                           @NonNull final AccountRecord accountRecord) {
        final List<IdTokenRecord> result;
        final String accountEnv = accountRecord.getEnvironment();

        if (null == clientId) {
            // If the client id was null... then presumably we want to aggregate the IdTokens across
            // apps... why would you want that? For now, throw an Exception and see if anyone requests
            // this feature...
            throw new UnsupportedOperationException(
                    "Aggregating IdTokens across ClientIds is not supported - do you have a feature request?"
            );
        } else {
            final OAuth2TokenCache cache = getTokenCacheForClient(
                    clientId,
                    accountEnv,
                    mCallingProcessUid
            );

            result = cache.getIdTokensForAccountRecord(
                    clientId,
                    accountRecord
            );
        }

        return result;
    }

    /**
     * Broker-only API. Fetches AccountRecords from all provided caches - makes NO GUARANTEES
     * as to whether or not an AT/RT pair exists for these Accounts.
     *
     * @return A List of AccountRecords, may be empty but is never null.
     */
    public List<AccountRecord> getAccounts() {
        final String methodName = ":getAccounts";

        final Set<AccountRecord> allAccounts = new HashSet<>();

        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            final OAuth2TokenCache candidateCache = getTokenCacheForClient(
                    metadata.getClientId(),
                    metadata.getEnvironment(),
                    metadata.getUid() // Supports v1 broker back-compat which yields all accounts
            );

            if (null != candidateCache) {
                allAccounts.addAll(
                        ((MsalOAuth2TokenCache) candidateCache)
                                .getAccountCredentialCache()
                                .getAccounts()
                );
            }
        }

        // Hit the FOCI cache
        allAccounts.addAll(mFociCache.getAccountCredentialCache().getAccounts());

        final List<AccountRecord> allAccountsResult = new ArrayList<>(allAccounts);

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allAccountsResult.size()
                        + "] accounts."
        );

        return allAccountsResult;
    }

    /**
     * Removes the provided {@link AccountRecord} from all of the caches known by this instance.
     * This API is akin to a device-wide signout for a non-joined user. Note, this affects the cache
     * ONLY so don't rely on this API for things like clearing cookies.
     *
     * @param accountRecord The AccountRecord to remove.
     * @return An {@link AccountDeletionRecord} indicating which AccountRecords were removed, if any.
     */
    @SuppressWarnings(UNCHECKED)
    public AccountDeletionRecord removeAccountFromDevice(@NonNull final AccountRecord accountRecord) {
        final String methodName = ":removeAccountFromDevice";

        if (null == accountRecord) {
            Logger.error(
                    TAG + methodName,
                    "Illegal arg. Cannot delete a null AccountRecord!",
                    null
            );

            throw new IllegalArgumentException("AccountRecord may not be null.");
        }

        final Set<String> allClientIds = mApplicationMetadataCache.getAllClientIds();

        Logger.info(
                TAG + methodName,
                "Found ["
                        + allClientIds.size()
                        + "] client ids."
        );

        final List<AccountDeletionRecord> deletionRecordList = new ArrayList<>();

        for (final String clientId : allClientIds) {
            deletionRecordList.add(
                    removeAccountInternal(
                            accountRecord.getEnvironment(),
                            clientId,
                            accountRecord.getHomeAccountId(),
                            null,
                            true
                    )
            );
        }

        // Create a List of the deleted AccountRecords...
        final List<AccountRecord> deletedAccountRecords = new ArrayList<>();

        for (final AccountDeletionRecord accountDeletionRecord : deletionRecordList) {
            deletedAccountRecords.addAll(accountDeletionRecord);
        }

        Logger.info(
                TAG + methodName,
                "Deleted ["
                        + deletedAccountRecords.size()
                        + "] AccountRecords."
        );

        return new AccountDeletionRecord(deletedAccountRecords);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This override adds some broker-specific behavior. Specifically, the following:
     * Attempts to delete any provided matching account criteria from all caches which can be
     * found via {@link BrokerApplicationMetadata}. Depending on whether wildcards are used or not,
     * calling removeAccount may remove 0 or more accounts in 0 or more caches.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @return An {@link AccountDeletionRecord}, containing the deleted {@link AccountDeletionRecord}s.
     * @see #removeAccountFromDevice(AccountRecord).
     */
    @Override
    public AccountDeletionRecord removeAccount(@Nullable final String environment,
                                               @Nullable final String clientId,
                                               @Nullable final String homeAccountId,
                                               @Nullable final String realm) {
        return removeAccountInternal(
                environment,
                clientId,
                homeAccountId,
                realm,
                false
        );
    }

    @Override
    public void clearAll() {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

    /**
     * Tests if a clientId is 'known' to the cache. A clientId is known if a token has been
     * previously saved to the cache with it.
     *
     * @param clientId The clientId whose known status should be queried.
     * @return True if this clientId is known. False otherwise.
     */
    public boolean isClientIdKnownToCache(@NonNull final String clientId) {
        return getAllClientIds().contains(clientId);
    }

    /**
     * Returns the List of FoCI users in the cache. This API is provided so that the broker may
     * **internally** query the cache for known users, such that the broker may verify an
     * unknown clientId is a part of the FoCI family.
     * <p>
     * Please note, the ICacheRecords returned by this query are NOT fully populated. Only the
     * {@link GenericAccount} and {@link GenericRefreshToken} will be returned.
     * will be resutned.
     *
     * @return A List of ICacheRecords for the FoCI accounts.
     */
    @SuppressWarnings(UNCHECKED)
    public List<ICacheRecord> getFociCacheRecords() {
        final String methodName = ":getFociCacheRecords";

        final List<ICacheRecord> result = new ArrayList<>();

        final List<BrokerApplicationMetadata> allFociApplicationMetadata =
                mApplicationMetadataCache.getAllFociApplicationMetadata();

        for (final BrokerApplicationMetadata fociAppMetadata : allFociApplicationMetadata) {
            // Load all the accounts
            final List<AccountRecord> accounts = mFociCache.getAccounts(
                    fociAppMetadata.getEnvironment(),
                    fociAppMetadata.getClientId()
            );

            // For each account, load the RT
            for (final AccountRecord account : accounts) {
                final String homeAccountId = account.getHomeAccountId();
                final String environment = account.getEnvironment();
                final String clientId = fociAppMetadata.getClientId();
                final String realm = account.getRealm();

                // Load the refresh token (1 per user per environment)
                final List<Credential> refreshTokens =
                        mFociCache
                                .getAccountCredentialCache()
                                .getCredentialsFilteredBy(
                                        homeAccountId,
                                        environment,
                                        CredentialType.RefreshToken,
                                        clientId,
                                        null, // wildcard (*)
                                        null, // wildcard (*)
                                        null // Not applicable
                                );

                // Load the V1IdToken (v1 if adal used)
                final List<Credential> v1IdTokens =
                        mFociCache
                                .getAccountCredentialCache()
                                .getCredentialsFilteredBy(
                                        homeAccountId,
                                        environment,
                                        CredentialType.V1IdToken,
                                        clientId,
                                        realm,
                                        null,
                                        null // Not applicable
                                );

                // Load the IdToken
                final List<Credential> idTokens =
                        mFociCache
                                .getAccountCredentialCache()
                                .getCredentialsFilteredBy(
                                        homeAccountId,
                                        environment,
                                        CredentialType.IdToken,
                                        clientId,
                                        realm,
                                        null,
                                        null // not applicable
                                );

                // Construct the ICacheRecord
                if (!refreshTokens.isEmpty()) {
                    final CacheRecord cacheRecord = new CacheRecord();
                    cacheRecord.setAccount(account);
                    cacheRecord.setRefreshToken((RefreshTokenRecord) refreshTokens.get(0));

                    // Add the V1IdToken (if exists, should have 1 if ADAL used)
                    if (!v1IdTokens.isEmpty()) {
                        Logger.verbose(
                                TAG + methodName,
                                "Found ["
                                        + v1IdTokens.size()
                                        + "] V1IdTokens"
                        );

                        cacheRecord.setV1IdToken((IdTokenRecord) v1IdTokens.get(0));
                    } else {
                        Logger.warn(
                                TAG + methodName,
                                "No V1IdTokens exist for this account."
                        );
                    }

                    // Add the IdTokens (if exists, should have 1 if MSAL used)
                    if (!idTokens.isEmpty()) {
                        Logger.verbose(
                                TAG + methodName,
                                "Found ["
                                        + idTokens.size()
                                        + "] IdTokens"
                        );

                        cacheRecord.setIdToken((IdTokenRecord) idTokens.get(0));
                    } else {
                        Logger.warn(
                                TAG + methodName,
                                "No IdTokens exist for this account."
                        );
                    }

                    // Add it to the result
                    result.add(cacheRecord);
                }
            }
        }

        return result;
    }

    private AccountDeletionRecord removeAccountInternal(@Nullable final String environment,
                                                        @Nullable final String clientId,
                                                        @Nullable final String homeAccountId,
                                                        @Nullable final String realm,
                                                        boolean deviceWide) {
        final String methodName = ":removeAccountInternal";

        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        final List<AccountDeletionRecord> deletionRecordList = new ArrayList<>();

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            final OAuth2TokenCache candidateCache = getTokenCacheForClient(
                    metadata.getClientId(),
                    metadata.getEnvironment(),
                    deviceWide
                            ? metadata.getUid() // Supports the removeAccountFromDevice() function
                            : mCallingProcessUid
            );

            if (null != candidateCache) {
                deletionRecordList.add(
                        candidateCache.removeAccount(
                                environment,
                                clientId,
                                homeAccountId,
                                realm
                        )
                );
            }
        }

        // Create a List of the deleted AccountRecords...
        final List<AccountRecord> deletedAccountRecords = new ArrayList<>();

        for (final AccountDeletionRecord accountDeletionRecord : deletionRecordList) {
            deletedAccountRecords.addAll(accountDeletionRecord);
        }

        Logger.info(
                TAG + methodName,
                "Deleted ["
                        + deletedAccountRecords.size()
                        + "] AccountRecords."
        );

        return new AccountDeletionRecord(deletedAccountRecords);
    }

    @Override
    @SuppressWarnings(UNCHECKED)
    protected Set<String> getAllClientIds() {
        return mApplicationMetadataCache.getAllClientIds();
    }

    @Override
    public AccountRecord getAccountByHomeAccountId(@Nullable final String environment,
                                                   @NonNull final String clientId,
                                                   @NonNull final String homeAccountId) {
        final String methodName = "getAccountByHomeAccountId";

        Logger.verbose(
                TAG + methodName,
                "Loading account by home account id."
        );

        if (null != environment) {
            OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    environment,
                    mCallingProcessUid
            );

            Logger.info(
                    TAG + methodName,
                    "Loading from FOCI cache? ["
                            + (targetCache == null)
                            + "]"
            );

            if (null != targetCache) {
                return targetCache.getAccountByHomeAccountId(
                        environment,
                        clientId,
                        homeAccountId
                );
            } else {
                return mFociCache.getAccountByHomeAccountId(
                        environment,
                        clientId,
                        homeAccountId
                );
            }
        } else {
            AccountRecord result = null;

            final List<OAuth2TokenCache> cachesToInspect = getTokenCachesForClientId(clientId);
            final Iterator<OAuth2TokenCache> cacheIterator = cachesToInspect.iterator();

            while (null == result && cacheIterator.hasNext()) {
                result = cacheIterator
                        .next()
                        .getAccountByHomeAccountId(
                                environment,
                                clientId,
                                homeAccountId
                        );
            }

            return result;
        }
    }

    private MsalOAuth2TokenCache initializeProcessUidCache(@NonNull final Context context,
                                                           final int bindingProcessUid) {
        final String methodName = ":initializeProcessUidCache";

        Logger.verbose(
                TAG + methodName,
                "Initializing uid cache."
        );

        if (null != mDelegate) {
            Logger.warn(
                    TAG + methodName,
                    "Using swapped delegate cache."
            );

            return mDelegate.getTokenCache(context, bindingProcessUid);
        }

        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        SharedPreferencesAccountCredentialCache
                                .getBrokerUidSequesteredFilename(bindingProcessUid),
                        storageHelper
                );

        return getTokenCache(context, sharedPreferencesFileManager, false);
    }

    private static MicrosoftFamilyOAuth2TokenCache initializeFociCache(@NonNull final Context context) {
        final String methodName = ":initializeFociCache";
        Logger.verbose(
                TAG + methodName,
                "Initializing foci cache"
        );
        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        storageHelper
                );

        return getTokenCache(context, sharedPreferencesFileManager, true);
    }

    @SuppressWarnings(UNCHECKED)
    private static <T extends MsalOAuth2TokenCache> T getTokenCache(@NonNull final Context context,
                                                                    @NonNull final ISharedPreferencesFileManager spfm,
                                                                    boolean isFoci) {
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final IAccountCredentialCache accountCredentialCache =
                new SharedPreferencesAccountCredentialCache(
                        cacheKeyValueDelegate,
                        spfm
                );
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter =
                new MicrosoftStsAccountCredentialAdapter();

        return (T)
                (isFoci ? // Decide which cache type to create
                        new MicrosoftFamilyOAuth2TokenCache<>(
                                context,
                                accountCredentialCache,
                                accountCredentialAdapter
                        )
                        :
                        new MsalOAuth2TokenCache<>(
                                context,
                                accountCredentialCache,
                                accountCredentialAdapter
                        )
                );
    }

    /**
     * Returns the TokenCache to use for supplied client and environment.
     *
     * @param clientId          The target client id.
     * @param environment       The target environment.
     * @param callingProcessUid The uid of the calling process.
     * @return The {@link MsalOAuth2TokenCache} matching the supplied criteria or null, if no matching
     * cache was found.
     */
    @Nullable
    private MsalOAuth2TokenCache getTokenCacheForClient(@NonNull final String clientId,
                                                        @NonNull final String environment,
                                                        final int callingProcessUid) {
        final String methodName = ":getTokenCacheForClient";

        final BrokerApplicationMetadata metadata = mApplicationMetadataCache.getMetadata(
                clientId,
                environment,
                callingProcessUid
        );

        MsalOAuth2TokenCache targetCache = null;

        if (null != metadata) {
            final boolean isFoci = null != metadata.getFoci();

            Logger.verbose(
                    TAG + methodName,
                    "is Foci? ["
                            + isFoci
                            + "]"
            );

            if (isFoci) {
                targetCache = mFociCache;
            } else {
                targetCache = initializeProcessUidCache(getContext(), callingProcessUid);
            }
        }

        if (null == targetCache) {
            Logger.warn(
                    TAG + methodName,
                    "Could not locate a cache for this app."
            );
        }

        return targetCache;
    }

    /**
     * Sets the SSO state for the supplied Account, relative to the provided uid.
     *
     * @param uidStr       The uid of the app whose SSO token is being inserted.
     * @param account      The account for which the supplied token is being inserted.
     * @param refreshToken The token to insert.
     */
    public void setSingleSignOnState(@NonNull final String uidStr,
                                     @NonNull final GenericAccount account,
                                     @NonNull final GenericRefreshToken refreshToken) {
        final String methodName = ":setSingleSignOnState";

        final boolean isFrt = refreshToken.getIsFamilyRefreshToken();

        MsalOAuth2TokenCache targetCache;

        final int uid = Integer.valueOf(uidStr);

        if (isFrt) {
            Logger.verbose(
                    TAG + methodName,
                    "Saving tokens to foci cache."
            );

            targetCache = mFociCache;
        } else {
            // If there is an existing cache for this client id, use it. Otherwise, create a new
            // one based on the supplied uid.
            targetCache = getTokenCacheForClient(
                    refreshToken.getClientId(),
                    refreshToken.getEnvironment(),
                    mCallingProcessUid
            );

            if (null == targetCache) {
                Logger.verbose(
                        TAG + methodName,
                        "Existing cache could not be found. Creating a new one..."
                );

                targetCache = initializeProcessUidCache(
                        getContext(),
                        uid
                );
            }
        }
        try {
            targetCache.setSingleSignOnState(
                    account,
                    refreshToken
            );
            updateApplicationMetadataCache(
                    refreshToken.getClientId(),
                    refreshToken.getEnvironment(),
                    refreshToken.getFamilyId(),
                    uid
            );
        } catch (ClientException e) {
            Logger.warn(
                    TAG + methodName,
                    "Failed to save account/refresh token. Skipping."
            );
        }
    }
}
