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
 * 1 "Primary" cache which, if the calligProcessUid (broker-bound app) is NOT in the family, is used
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
    private MsalOAuth2TokenCache mProcessUidCache;
    private List<MsalOAuth2TokenCache> mOptionalCaches;
    private final int mCallingProcessUid;

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context                  The current application context.
     * @param callingProcessUid        The UID of the current broker-calling app.
     * @param applicationMetadataCache The metadata cache to use.
     * @param initializeOptionalCaches True, if the caller wants to view and modify other caches (ADAL).
     *                                 False otherwise.
     */
    public BrokerOAuth2TokenCache(@NonNull final Context context,
                                  int callingProcessUid,
                                  @NonNull IBrokerApplicationMetadataCache applicationMetadataCache,
                                  boolean initializeOptionalCaches) {
        super(context);

        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );

        mCallingProcessUid = callingProcessUid;
        mFociCache = initializeFociCache(context);
        mProcessUidCache = initializeProcessUidCache(context, callingProcessUid);
        mApplicationMetadataCache = applicationMetadataCache;

        final int[] processUids;
        if (initializeOptionalCaches) {
            final List<BrokerApplicationMetadata> metadataList = mApplicationMetadataCache.getAll();
            processUids = new int[metadataList.size()];

            for (int ii = 0; ii < metadataList.size(); ii++) {
                processUids[ii] = metadataList.get(ii).getUid();
            }
        } else {
            processUids = null;
        }

        mOptionalCaches = initializeOptionalCaches(context, callingProcessUid, processUids);
    }

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context         The current application context.
     * @param fociCache       The FOCI cache implementation to use.
     * @param processUidCache The app-UID-specific cache implementation to use.
     * @param otherAppCaches  A List of other app caches to inspect.
     */
    public BrokerOAuth2TokenCache(@NonNull Context context,
                                  final int callingProcessUid,
                                  @NonNull IBrokerApplicationMetadataCache applicationMetadataCache,
                                  @NonNull final MicrosoftFamilyOAuth2TokenCache fociCache,
                                  @NonNull final MsalOAuth2TokenCache processUidCache,
                                  @NonNull final List<MsalOAuth2TokenCache> otherAppCaches) {
        super(context);

        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );

        mApplicationMetadataCache = applicationMetadataCache;
        mCallingProcessUid = callingProcessUid;
        mFociCache = fociCache;
        mProcessUidCache = processUidCache;
        mOptionalCaches = otherAppCaches;
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

        final OAuth2TokenCache targetCache = isFoci ? mFociCache : mProcessUidCache;

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
                                                @NonNull final String familyId,
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

        // First look in the app specific cache...
        ICacheRecord resultRecord = mProcessUidCache.load(
                clientId,
                target,
                account
        );

        final boolean resultFound = null != resultRecord.getRefreshToken();

        Logger.verbose(
                TAG + methodName,
                "Result found? ["
                        + resultFound
                        + "]"
        );

        if (!resultFound) {
            resultRecord = mFociCache.loadByFamilyId(
                    clientId,
                    target,
                    account
            );
        }

        return resultRecord;
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credential) {
        final String methodName = ":removeCredential";

        boolean removed = mProcessUidCache.removeCredential(credential);

        if (!removed) {
            Logger.verbose(
                    TAG + methodName,
                    "Attempting to remove credential from FOCI cache."
            );
            removed = mFociCache.removeCredential(credential);
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

        Logger.verbose(
                TAG + methodName,
                "Fetching account..."
        );

        final AccountRecord account = mProcessUidCache.getAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );

        Logger.verbose(
                TAG + methodName,
                "Record was null? ["
                        + (null == account)
                        + "]"
        );

        final AccountRecord result = null != account
                ? account
                : mFociCache.getAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );

        Logger.verbose(
                TAG + methodName,
                "Result AccountRecord located? ["
                        + (null != result)
                        + "]"
        );

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

        // First, check the current calling app's cache...
        AccountRecord accountRecord = mProcessUidCache.getAccountWithLocalAccountId(
                environment,
                clientId,
                localAccountId
        );

        Logger.verbose(
                TAG + methodName,
                "Result found? ["
                        + (null != accountRecord)
                        + "]"
        );

        // If nothing was returned, check the foci cache...
        if (null == accountRecord) {
            Logger.verbose(
                    TAG + methodName,
                    "Inspecting FOCI cache..."
            );

            accountRecord = mFociCache.getAccountWithLocalAccountId(
                    environment,
                    clientId,
                    localAccountId
            );
        }

        Logger.verbose(
                TAG + methodName,
                "Result found? ["
                        + (null != accountRecord)
                        + "]"
        );

        return accountRecord;
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public List<AccountRecord> getAccounts(@Nullable final String environment,
                                           @NonNull final String clientId) {
        final String methodName = ":getAccounts (2 param)";

        final List<AccountRecord> allAccounts = new ArrayList<>();

        allAccounts.addAll(mProcessUidCache.getAccounts(environment, clientId));
        allAccounts.addAll(mFociCache.getAccounts(environment, clientId));

        for (final OAuth2TokenCache optionalTokenCache : mOptionalCaches) {
            allAccounts.addAll(optionalTokenCache.getAccounts(environment, clientId));
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allAccounts.size()
                        + "] accounts."
        );

        return allAccounts;
    }

    /**
     * Broker-only API. Fetches AccountRecords from all provided caches - makes NO GUARANTEES
     * as to whether or not an AT/RT pair exists for these Accounts.
     *
     * @return A List of AccountRecords, may be empty but is never null.
     */
    public List<AccountRecord> getAccounts() {
        final String methodName = ":getAccounts";

        final List<AccountRecord> allAccounts = new ArrayList<>();

        allAccounts.addAll(mProcessUidCache.getAccountCredentialCache().getAccounts());
        allAccounts.addAll(mFociCache.getAccountCredentialCache().getAccounts());

        for (final MsalOAuth2TokenCache optionalTokenCache : mOptionalCaches) {
            allAccounts.addAll(optionalTokenCache.getAccountCredentialCache().getAccounts());
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allAccounts.size()
                        + "] accounts."
        );

        return allAccounts;
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

        final Set<String> allClientIds = new HashSet<>();

        allClientIds.addAll(mFociCache.getAllClientIds());
        allClientIds.addAll(mProcessUidCache.getAllClientIds());

        for (final MsalOAuth2TokenCache optionalTokenCache : mOptionalCaches) {
            allClientIds.addAll(optionalTokenCache.getAllClientIds());
        }

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

        AccountDeletionRecord deletionRecord = mProcessUidCache.removeAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );

        Logger.verbose(
                TAG + methodName,
                "Accounts deleted count (uid): ["
                        + deletionRecord.size()
                        + "]"
        );

        if (deletionRecord.isEmpty()) {
            deletionRecord = mFociCache.removeAccount(
                    environment,
                    clientId,
                    homeAccountId,
                    realm
            );
        }

        Logger.verbose(
                TAG + methodName,
                "Accounts deleted count (foci): ["
                        + deletionRecord.size()
                        + "]"
        );

        // Iterate over the optionalCaches to try and locate the account to delete.
        // This supports the removeAccountFromDevice API -- when this method is called directly,
        // the clientId will not match any records stored in the optional caches (only the
        // callingProcessUid cache and/or the FOCI cache...
        //
        // Effectively, this means that this logic does nothing unless called via
        // removeAccountFromDevice or if the function is invoked using a clientId other than our own.
        final Iterator<MsalOAuth2TokenCache> cacheIterator = mOptionalCaches.iterator();

        while (deletionRecord.isEmpty() && cacheIterator.hasNext()) {
            deletionRecord = cacheIterator
                    .next()
                    .removeAccount(
                            environment,
                            clientId,
                            homeAccountId,
                            realm
                    );
        }

        Logger.verbose(
                TAG + methodName,
                "Accounts deleted count (other caches): ["
                        + deletionRecord.size()
                        + "]"
        );

        return deletionRecord;
    }

    @Override
    @SuppressWarnings(UNCHECKED)
    protected Set<String> getAllClientIds() {
        final Set<String> result = new HashSet<>();

        result.addAll(mFociCache.getAllClientIds());
        result.addAll(mProcessUidCache.getAllClientIds());

        for (final MsalOAuth2TokenCache optionalCache : mOptionalCaches) {
            result.addAll(optionalCache.getAllClientIds());
        }

        return result;
    }

    private List<MsalOAuth2TokenCache> initializeOptionalCaches(@NonNull final Context context,
                                                                final int callingProcessUid,
                                                                @Nullable final int[] optionalProcessUids) {
        final String methodName = ":initializeOptionalCaches";

        Logger.verbose(
                TAG + methodName,
                "Initializing optional caches."
        );

        final List<MsalOAuth2TokenCache> caches = new ArrayList<>();

        if (null != optionalProcessUids) {
            final Set<Integer> uids = new HashSet<>();

            for (final int uid : optionalProcessUids) {
                uids.add(uid);
            }

            Logger.verbose(
                    TAG + methodName,
                    "Attempting to initialize ["
                            + uids.size()
                            + "] caches."
            );

            for (final Integer uid : uids) {
                if (uid != callingProcessUid) { // do not allow the calling process uid cache to exist twice
                    caches.add(
                            initializeProcessUidCache(
                                    context,
                                    uid
                            )
                    );
                } else {
                    Logger.warn(
                            TAG + methodName,
                            "Attempt to create duplicate cache for uid: ["
                                    + uid
                                    + "] -- skipping!"
                    );
                }
            }
        }

        Logger.info(
                TAG + methodName,
                "Initialized ["
                        + caches.size()
                        + "] caches."

        );

        return caches;
    }

    private static MsalOAuth2TokenCache initializeProcessUidCache(@NonNull final Context context,
                                                                  final int bindingProcessUid) {
        final String methodName = ":initializeProcessUidCache";

        Logger.verbose(
                TAG + methodName,
                ""
        );

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
}
