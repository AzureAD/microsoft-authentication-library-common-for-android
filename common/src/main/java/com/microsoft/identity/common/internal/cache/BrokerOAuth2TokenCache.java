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

import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

/**
 * "Combined" cache implementation to cache tokens inside of the broker.
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

    private static final String ERR_UNSUPPORTED_OPERATION = "This method is unsupported by the ADALOAuth2TokenCache";
    private static final String UNCHECKED = "unchecked";

    private final FociOAuth2TokenCache mFociCache;
    private MsalOAuth2TokenCache mAppUidCache;
    private List<MsalOAuth2TokenCache> mOptionalCaches;

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context         The current application context.
     * @param appPrimaryUid   The calling app UID (current app).
     * @param optionalAppUids An array of other app UID whose caches may be inspected.
     */
    public BrokerOAuth2TokenCache(@NonNull final Context context,
                                  int appPrimaryUid,
                                  @Nullable final int[] optionalAppUids) {
        super(context);
        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );
        mFociCache = initializeFociCache(context);
        mAppUidCache = initializeAppUidCache(context, appPrimaryUid);
        mOptionalCaches = initializeOptionalCaches(context, appPrimaryUid, optionalAppUids);
    }

    /**
     * Constructs a new BrokerOAuth2TokenCache.
     *
     * @param context        The current application context.
     * @param fociCache      The FOCI cache implementation to use.
     * @param appUidCache    The app-UID-specific cache implementation to use.
     * @param otherAppCaches A List of other app caches to inspect.
     */
    public BrokerOAuth2TokenCache(@NonNull Context context,
                                  @NonNull final FociOAuth2TokenCache fociCache,
                                  @NonNull final MsalOAuth2TokenCache appUidCache,
                                  @NonNull final List<MsalOAuth2TokenCache> otherAppCaches) {
        super(context);
        Logger.verbose(
                TAG + "ctor",
                "Init::" + TAG
        );
        mFociCache = fociCache;
        mAppUidCache = appUidCache;
        mOptionalCaches = otherAppCaches;
    }

    @Override
    public ICacheRecord save(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                             @NonNull final GenericAuthorizationRequest request,
                             @NonNull final GenericTokenResponse response) throws ClientException {
        final String methodName = ":save";

        final boolean isFoci = !StringExtensions.isNullOrBlank(response.getFamilyId());

        Logger.info(
                TAG + methodName,
                "Saving to FOCI cache? ["
                        + isFoci
                        + "}"
        );

        final OAuth2TokenCache targetCache = isFoci ? mFociCache : mAppUidCache;

        return targetCache.save(
                oAuth2Strategy,
                request,
                response
        );
    }

    @Override
    public ICacheRecord save(@NonNull final AccountRecord accountRecord,
                             @NonNull final IdTokenRecord idTokenRecord) {
        throw new UnsupportedOperationException(
                ERR_UNSUPPORTED_OPERATION
        );
    }

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
        ICacheRecord resultRecord = mAppUidCache.load(
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

        boolean removed = mAppUidCache.removeCredential(credential);

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

        final AccountRecord account = mAppUidCache.getAccount(
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

        // First, check the primary cache...
        AccountRecord accountRecord = mAppUidCache.getAccountWithLocalAccountId(
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

        allAccounts.addAll(mAppUidCache.getAccounts(environment, clientId));
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

        allAccounts.addAll(mAppUidCache.getAccountCredentialCache().getAccounts());
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

    @SuppressWarnings(UNCHECKED)
    public AccountDeletionRecord removeAccountFromDevice(@NonNull final AccountRecord accountRecord) {
        final Set<String> allClientIds = new HashSet<>();

        allClientIds.addAll(mFociCache.getAllClientIds());
        allClientIds.addAll(mAppUidCache.getAllClientIds());

        for (final MsalOAuth2TokenCache optionalTokenCache : mOptionalCaches) {
            allClientIds.addAll(optionalTokenCache.getAllClientIds());
        }

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

        return new AccountDeletionRecord(deletedAccountRecords);
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment,
                                               String clientId,
                                               String homeAccountId,
                                               @Nullable String realm) {
        final String methodName = ":removeAccount";

        AccountDeletionRecord deletionRecord = mAppUidCache.removeAccount(
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
        result.addAll(mAppUidCache.getAllClientIds());

        for (final MsalOAuth2TokenCache optionalCache : mOptionalCaches) {
            result.addAll(optionalCache.getAllClientIds());
        }

        return result;
    }

    private List<MsalOAuth2TokenCache> initializeOptionalCaches(@NonNull final Context context,
                                                                final int appPrimaryUid,
                                                                @Nullable final int[] optionalAppUids) {
        final String methodName = ":initializeOptionalCaches";

        Logger.verbose(
                TAG + methodName,
                "Initializing optional caches."
        );

        final List<MsalOAuth2TokenCache> caches = new ArrayList<>();

        if (null != optionalAppUids) {
            final Set<Integer> uids = new HashSet<>();

            for (final int uid : optionalAppUids) {
                uids.add(uid);
            }

            for (final Integer uid : uids) {
                if (uid != appPrimaryUid) { // do not allow the primary cache to exist twice
                    caches.add(
                            initializeAppUidCache(
                                    context,
                                    uid
                            )
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

    private static MsalOAuth2TokenCache initializeAppUidCache(@NonNull final Context context,
                                                              final int bindingAppUid) {
        final String methodName = ":initializeAppUidCache";

        Logger.verbose(
                TAG + methodName,
                ""
        );

        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        SharedPreferencesAccountCredentialCache
                                .getBrokerUidSequesteredFilename(bindingAppUid),
                        storageHelper
                );

        return getTokenCache(context, sharedPreferencesFileManager, false);
    }

    private static FociOAuth2TokenCache initializeFociCache(@NonNull final Context context) {
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
                        new FociOAuth2TokenCache<>(
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
