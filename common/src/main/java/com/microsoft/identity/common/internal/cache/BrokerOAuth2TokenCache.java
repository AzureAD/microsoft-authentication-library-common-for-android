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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
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
 * 1 "Primary" cache which, if the callingProcessUid (broker-bound app) is NOT in the family, is used
 * to store tokens.
 * <p>
 * 0 or more "optional caches" -- these are initialized by passing the known processUids of other
 * broker-binding apps to this cache. Because all of the SharedPrefernces-based cache files'
 * names are deterministically chosen based on this UID, we can construct a reference to these
 * caches using this information.
 * <p>
 * Operations performed on the BrokerOAuth2TokenCache are designed to route the caller to the
 * proper data store: a good example of this is when calling save(). Save() will inspect the contents
 * of the response and determine if the payload contains a family id. If it does, the Account and
 * any associated credentials are written to the FOCI cache and nowhere else.
 * <p>
 * The reverse is true for non-family apps: if the response does not contain a family id, then the
 * account and credentials are written to the process uid-specific cache (the "primary cache").
 * <p>
 * Some operations will be performed on multiple caches; a good example of this is the
 * removeAccountFromDevice() API. This call affects multiple caches by iterating over the family,
 * app-specific, and optional caches to locate occurrences of an Account: if it is found, the account
 * and corresponding credential entries are removed.
 *
 * @param <GenericOAuth2Strategy>       The strategy type to use.
 * @param <GenericAuthorizationRequest> The AuthorizationRequest type to use.
 * @param <GenericTokenResponse>        The TokenResponse type to use.
 * @param <GenericAccount>              The Account type to use.
 * @param <GenericRefreshToken>         The RefreshToken type to use.
 */
public class BrokerOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends MicrosoftTokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends com.microsoft.identity.common.internal.providers.oauth2.RefreshToken>
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

    public interface ProcessUidCacheFactory {

        MsalOAuth2TokenCache getTestDelegate(final Context context, final int bindingProcessUid);

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

    @Override
    public ICacheRecord save(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                             @NonNull final GenericAuthorizationRequest request,
                             @NonNull final GenericTokenResponse response) throws ClientException {
        final String methodName = ":save";

        final boolean isFoci = !StringExtensions.isNullOrBlank(response.getFamilyId());

        if (isFoci) {
            Logger.info(
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
                        + "}"
        );

        OAuth2TokenCache targetCache;

        if (isFoci) {
            targetCache = mFociCache;
        } else {
            // Try to find an existing cache for this application
            targetCache = getTokenCacheForClient(
                    request.getClientId(),
                    oAuth2Strategy.getIssuerCacheIdentifier(request)
            );

            if (null == targetCache) {// No existing cache could be found... Make a new one!
                // TODO Add logging
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
                result.getIdToken().getClientId(),
                result.getIdToken().getEnvironment(),
                result.getRefreshToken().getFamilyId(),
                mCallingProcessUid
        );

        return result;
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
     * If the result contains only a RefreshTokenRecord then the caller should attempt to refresh
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
                             @NonNull final AccountRecord account) {
        final String methodName = ":load";

        Logger.verbose(
                TAG + methodName,
                "Performing lookup in app-specific cache."
        );

        final OAuth2TokenCache targetCache = getTokenCacheForClient(clientId, account.getEnvironment());
        final boolean shouldUseFociCache = null == targetCache;
        final ICacheRecord resultRecord;

        if (shouldUseFociCache) {
            // We do not have a cache for this app or it is not yet known to be a member of the family
            // use the foci cache....
            resultRecord = mFociCache.loadByFamilyId(
                    clientId,
                    target,
                    account
            );
        } else {
            resultRecord = targetCache.load(
                    clientId,
                    target,
                    account
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

    @Override
    public boolean removeCredential(@NonNull final Credential credential) {
        final String methodName = ":removeCredential";

        final OAuth2TokenCache targetCache = getTokenCacheForClient(
                credential.getClientId(),
                credential.getEnvironment()
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
                    environment
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
                            metadata.getEnvironment()
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
    public AccountRecord getAccountWithLocalAccountId(@Nullable final String environment,
                                                      @NonNull final String clientId,
                                                      @NonNull final String localAccountId) {
        final String methodName = ":getAccountWithLocalAccountId";

        Logger.verbose(
                TAG + methodName,
                "Loading account by local account id."
        );

        if (null != environment) {
            OAuth2TokenCache targetCache = getTokenCacheForClient(
                    clientId,
                    environment
            );

            if (null != targetCache) {
                return targetCache.getAccountWithLocalAccountId(
                        environment,
                        clientId,
                        localAccountId
                );
            } else {
                return mFociCache.getAccountWithLocalAccountId(
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
                        .getAccountWithLocalAccountId(
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
                    environment
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
                    metadata.getEnvironment()
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
                    removeAccount(
                            accountRecord.getEnvironment(),
                            clientId,
                            accountRecord.getHomeAccountId(),
                            null
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
     * Attempts to delete any provided matching account criteria from the callingProcessUid cache,
     * followed by the foci cache, followed by the List of optional caches. Deletion from the
     * optional caches should only have an effect if the clientId matches. In the base-case, these
     * values will not match and as such, calling removeAccount iteratively will not remove anything.
     * <p>
     * In the case where the provided clientId matches neither the current callingProcessUid nor any
     * save cache value in the FOCI, then that account will be removed from one of the optional
     * caches. This supports removeAccountFromDevice.
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
        final String methodName = ":removeAccount";

        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        final List<AccountDeletionRecord> deletionRecordList = new ArrayList<>();

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            final OAuth2TokenCache candidateCache = getTokenCacheForClient(
                    metadata.getClientId(),
                    metadata.getEnvironment()
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

            return mDelegate.getTestDelegate(context, bindingProcessUid);
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
     * Returns the TokenCache to use for supplied client and environment or null, if none can be found.
     *
     * @param clientId
     * @param environment
     * @return
     */
    @Nullable
    private MsalOAuth2TokenCache getTokenCacheForClient(@NonNull final String clientId,
                                                        @NonNull final String environment) {

        // TODO Add logging
        // TODO javadoc

        final BrokerApplicationMetadata metadata = mApplicationMetadataCache.getMetadata(
                clientId,
                environment
        );

        MsalOAuth2TokenCache targetCache = null;

        if (null != metadata) {
            final boolean isFoci = null != metadata.getFoci();

            if (isFoci) {
                targetCache = mFociCache;
            } else {
                targetCache = initializeProcessUidCache(getContext(), metadata.getUid());
            }
        }

        return targetCache;
    }
}
