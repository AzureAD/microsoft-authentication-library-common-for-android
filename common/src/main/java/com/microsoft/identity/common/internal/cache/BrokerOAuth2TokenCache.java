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

public class BrokerOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends MicrosoftTokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends com.microsoft.identity.common.internal.providers.oauth2.RefreshToken>
        extends OAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse> {

    private static final String TAG = BrokerOAuth2TokenCache.class.getSimpleName();

    private static final String ERR_UNSUPPORTED_OPERATION = "This method is unsupported by the ADALOAuth2TokenCache";

    private final OAuth2TokenCache mFociCache;
    private OAuth2TokenCache mAppUidCache;
    @SuppressWarnings("PMD.UnusedPrivateField")
    private List<OAuth2TokenCache> mOptionalCaches;

    /**
     * Constructs a new OAuth2TokenCache.
     *
     * @param context The Application Context of the consuming app.
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

    public BrokerOAuth2TokenCache(@NonNull Context context,
                                  @NonNull final OAuth2TokenCache fociCache,
                                  @NonNull final OAuth2TokenCache appUidCache,
                                  @NonNull final List<OAuth2TokenCache> otherAppCaches) {
        super(context);
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
    public ICacheRecord loadByFamilyId(@Nullable final String clientId,
                                       @Nullable final String target,
                                       @NonNull final AccountRecord accountRecord) {
        return mFociCache.loadByFamilyId(
                clientId,
                target,
                accountRecord
        );
    }

    @Override
    public ICacheRecord load(@NonNull final String clientId,
                             @Nullable final String target,
                             @NonNull final AccountRecord account) {
        // First look in the app specific cache...
        ICacheRecord resultRecord = mAppUidCache.load(
                clientId,
                target,
                account
        );

        final boolean resultFound = null != resultRecord.getRefreshToken();

        if (!resultFound) {
            resultRecord = loadByFamilyId(
                    clientId,
                    target,
                    account
            );
        }

        return resultRecord;
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credential) {
        return mAppUidCache.removeCredential(credential) || mFociCache.removeCredential(credential);
    }

    @Override
    @Nullable
    public AccountRecord getAccount(@Nullable final String environment,
                                    @NonNull final String clientId,
                                    @NonNull final String homeAccountId,
                                    @Nullable final String realm) {
        final AccountRecord account = mAppUidCache.getAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );

        return null != account
                ? account
                : mFociCache.getAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );
    }

    @Override
    @Nullable
    public AccountRecord getAccountWithLocalAccountId(@Nullable final String environment,
                                                      @NonNull final String clientId,
                                                      @NonNull final String localAccountId) {
        // First, check the primary cache...
        AccountRecord accountRecord = mAppUidCache.getAccountWithLocalAccountId(
                environment,
                clientId,
                localAccountId
        );

        // If nothing was returned, check the foci cache...
        if (null == accountRecord) {
            accountRecord = mFociCache.getAccountWithLocalAccountId(
                    environment,
                    clientId,
                    localAccountId
            );
        }

        return accountRecord;
    }

    @Override
    public List<AccountRecord> getAccounts(@Nullable final String environment,
                                           @NonNull final String clientId) { // TODO allow this to be nullable...
        final List<AccountRecord> allAccounts = new ArrayList<>();

        allAccounts.addAll(mAppUidCache.getAccounts(environment, clientId));
        allAccounts.addAll(mFociCache.getAccounts(environment, clientId));

        for (final OAuth2TokenCache optionalTokenCache : mOptionalCaches) {
            allAccounts.addAll(optionalTokenCache.getAccounts(environment, clientId));
        }

        return allAccounts;
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment,
                                               String clientId,
                                               String homeAccountId,
                                               @Nullable String realm) {
        AccountDeletionRecord deletionRecord = mAppUidCache.removeAccount(
                environment,
                clientId,
                homeAccountId,
                realm
        );

        if (deletionRecord.isEmpty()) {
            deletionRecord = mFociCache.removeAccount(
                    environment,
                    clientId,
                    homeAccountId,
                    realm
            );
        }

        final Iterator<OAuth2TokenCache> cacheIterator = mOptionalCaches.iterator();

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


        return deletionRecord;
    }

    private List<OAuth2TokenCache> initializeOptionalCaches(@NonNull final Context context,
                                                            final int appPrimaryUid,
                                                            @Nullable final int[] optionalAppUids) {
        final List<OAuth2TokenCache> caches = new ArrayList<>();

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

            return caches;
        }

        return caches;
    }

    private static OAuth2TokenCache initializeAppUidCache(@NonNull final Context context,
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

        return getTokenCache(context, sharedPreferencesFileManager);
    }

    private static OAuth2TokenCache initializeFociCache(@NonNull final Context context) {
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

        return getTokenCache(context, sharedPreferencesFileManager);
    }

    private static OAuth2TokenCache getTokenCache(@NonNull final Context context,
                                                  @NonNull final ISharedPreferencesFileManager spfm) {
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final IAccountCredentialCache accountCredentialCache =
                new SharedPreferencesAccountCredentialCache(
                        cacheKeyValueDelegate,
                        spfm
                );
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter =
                new MicrosoftStsAccountCredentialAdapter();

        return new MsalOAuth2TokenCache<>(
                context,
                accountCredentialCache,
                accountCredentialAdapter
        );
    }
}
