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
package com.microsoft.identity.common.java.cache;

import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountCredentialBase;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Alternative version of {@link SharedPreferencesAccountCredentialCache} that assumes all writes and reads
 * are done through a single-instance and can thereforce be cached in memory.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SharedPreferencesAccountCredentialCacheWithMemoryCache extends AbstractAccountCredentialCache {

    private static final String TAG = SharedPreferencesAccountCredentialCacheWithMemoryCache.class.getSimpleName();

    private final ICacheKeyValueDelegate mCacheValueDelegate;

    private final Object mCacheLock = new Object();
    private boolean mLoaded = false;
    private Map<String, AccountRecord> mCachedAccountRecordsWithKeys = new HashMap<>();
    private Map<String, Credential> mCachedCredentialsWithKeys = new HashMap<>();

    /**
     * Constructor of SharedPreferencesAccountCredentialCacheWithMemoryCache.
     *
     * @param accountCacheValueDelegate    ICacheKeyValueDelegate
     * @param sharedPreferencesFileManager INameValueStorage
     */
    public SharedPreferencesAccountCredentialCacheWithMemoryCache(
            @NonNull final ICacheKeyValueDelegate accountCacheValueDelegate,
            @NonNull final INameValueStorage<String> sharedPreferencesFileManager) {
        super(sharedPreferencesFileManager);
        Logger.verbose(TAG, "Init: " + TAG);
        mCacheValueDelegate = accountCacheValueDelegate;
        new Thread(() -> load()).start();
    }

    private void load() {
        final String methodTag = TAG + ":load";

        synchronized (mCacheLock) {
            try {
                mCachedAccountRecordsWithKeys = loadAccountsWithKeys();
                Logger.info(methodTag, "Loaded " + mCachedAccountRecordsWithKeys.size() + " AccountRecords");
                mCachedCredentialsWithKeys = loadCredentialsWithKeys();
                Logger.info(methodTag, "Loaded " + mCachedCredentialsWithKeys.size() + " Credentials");
            } catch (final Throwable t) {
                Logger.error(methodTag, "Failed to load initial accounts or credentials from SharedPreferences", t);
            } finally {
                mLoaded = true;
                mCacheLock.notifyAll();
            }
        }
    }

    private void waitForInitialLoad() {
        final String methodTag = TAG + ":waitForInitialLoad";

        while (!mLoaded) {
            try {
                mCacheLock.wait();
            } catch (final InterruptedException e) {
                Logger.error(methodTag, "Caught InterruptedException while waiting", e);
            }
        }
    }

    @Override
    public void saveAccount(@NonNull final AccountRecord accountInput) {
        final String methodTag = TAG + ":saveAccount";

        AccountRecord accountToSave = null;
        try {
            accountToSave = (AccountRecord) accountInput.clone();
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone AccountRecord", e);
            return;
        }

        Logger.verbose(methodTag, "Saving Account...");
        Logger.verbose(methodTag, "Account type: [" + accountToSave.getClass().getSimpleName() + "]");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(accountToSave);
        Logger.verbosePII(methodTag, "Generated cache key: [" + cacheKey + "]");

        synchronized (mCacheLock) {
            waitForInitialLoad();

            // Perform any necessary field merging on the Account to save...
            final AccountRecord existingAccount = getAccount(cacheKey);

            if (null != existingAccount) {
                accountToSave.mergeAdditionalFields(existingAccount);
            }

            final String cacheValue = mCacheValueDelegate.generateCacheValue(accountToSave);
            mSharedPreferencesFileManager.put(cacheKey, cacheValue);
            mCachedAccountRecordsWithKeys.put(cacheKey, accountToSave);
        }
    }

    @Override
    public void saveCredential(@NonNull Credential credentialInput) {
        final String methodTag = TAG + ":saveCredential";

        Credential credentialToSave = null;
        try {
            credentialToSave = (Credential) credentialInput.clone();
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone Credential", e);
            return;
        }

        Logger.verbose(methodTag, "Saving credential...");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToSave);
        Logger.verbosePII(methodTag, "Generated cache key: [" + cacheKey + "]");

        synchronized (mCacheLock) {
            waitForInitialLoad();

            // Perform any necessary field merging on the Credential to save...
            final Credential existingCredential = getCredential(cacheKey);

            if (null != existingCredential) {
                credentialToSave.mergeAdditionalFields(existingCredential);
            }

            final String cacheValue = mCacheValueDelegate.generateCacheValue(credentialToSave);
            mSharedPreferencesFileManager.put(cacheKey, cacheValue);
            mCachedCredentialsWithKeys.put(cacheKey, credentialToSave);
        }
    }

    @Override
    public AccountRecord getAccount(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getAccount";

        AccountRecord foundValue;

        synchronized (mCacheLock) {
            waitForInitialLoad();
            foundValue = mCachedAccountRecordsWithKeys.get(cacheKey);
        }

        try {
            if (foundValue != null) {
                foundValue = (AccountRecord) foundValue.clone();
            }
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone AccountRecord", e);
        }
        return foundValue;
    }

    @Override
    @Nullable
    public Credential getCredential(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getCredential";

        Credential foundValue;

        synchronized (mCacheLock) {
            waitForInitialLoad();
            foundValue = mCachedCredentialsWithKeys.get(cacheKey);
        }

        try {
            if (foundValue != null) {
                foundValue = (Credential) foundValue.clone();
            }
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone Credential", e);
        }

        return foundValue;
    }

    @NonNull
    private Map<String, AccountRecord> loadAccountsWithKeys() {
        final String methodTag = TAG + ":loadAccountsWithKeys";

        Logger.verbose(methodTag, "Loading Accounts + keys...");
        final Iterator<Map.Entry<String, String>> cacheValues = mSharedPreferencesFileManager.getAllFilteredByKey(new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return isAccount(value);
            }
        });
        final Map<String, AccountRecord> accounts = new HashMap<>();
        if (cacheValues != null) {
            while (cacheValues.hasNext()) {
                Map.Entry<String, ?> cacheValue = cacheValues.next();
                final String cacheKey = cacheValue.getKey();
                final AccountRecord account = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        AccountRecord.class
                );

                if (null == account) {
                    Logger.warn(methodTag, SharedPreferencesAccountCredentialCache.ACCOUNT_RECORD_DESERIALIZATION_FAILED);
                } else if (SharedPreferencesAccountCredentialCache.EMPTY_ACCOUNT.equals(account)) {
                    Logger.warn(methodTag, "The returned Account was uninitialized. Removing...");
                    mSharedPreferencesFileManager.remove(cacheKey);
                } else {
                    accounts.put(cacheKey, account);
                }
            }
        }

        Logger.verbose(methodTag, "Returning [" + accounts.size() + "] Accounts w/ keys...");

        return accounts;
    }

    @Override
    @NonNull
    public List<AccountRecord> getAccounts() {
        final String methodTag = TAG + ":getAccounts";
        Logger.verbose(methodTag, "Loading Accounts...(no arg)");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            final List<AccountRecord> accounts = new ArrayList<>();
            for (AccountRecord record : mCachedAccountRecordsWithKeys.values()) {
                try {
                    accounts.add((AccountRecord) record.clone());
                } catch (final CloneNotSupportedException e) {
                    Logger.error(methodTag, "Failed to clone AccountRecord", e);
                }
            }
            Logger.info(methodTag, "Found [" + accounts.size() + "] Accounts...");
            return accounts;
        }
    }

    @Override
    @NonNull
    public List<AccountRecord> getAccountsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final String realm) {
        final String methodTag = TAG + ":getAccountsFilteredBy";
        Logger.verbose(methodTag, "Loading Accounts...");

        final List<AccountRecord> allAccounts = getAccounts();

        final List<AccountRecord> matchingAccounts = getAccountsFilteredByInternal(
                homeAccountId,
                environment,
                realm,
                allAccounts
        );

        Logger.verbose(methodTag, "Found [" + matchingAccounts.size() + "] matching Accounts...");

        return matchingAccounts;
    }

    @NonNull
    private Map<String, Credential> loadCredentialsWithKeys() {
        final String methodTag = TAG + ":getCredentialsWithKeys";
        Logger.verbose(methodTag, "Loading Credentials with keys...");

        final Map<String, Credential> credentials = new HashMap<>();
        final Iterator<Map.Entry<String, String>> cacheValues = mSharedPreferencesFileManager.getAllFilteredByKey(new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return isCredential(value);
            }
        });

        while (cacheValues.hasNext()) {
            Map.Entry<String, ?> cacheValue = cacheValues.next();
            final String cacheKey = cacheValue.getKey();
            final Class<? extends AccountCredentialBase> clazz = credentialClassForType(cacheKey);
            final Credential credential = mCacheValueDelegate.fromCacheValue(
                    cacheValue.getValue().toString(),
                    clazz
            );

            if (null == credential) {
                Logger.warn(methodTag, SharedPreferencesAccountCredentialCache.CREDENTIAL_DESERIALIZATION_FAILED);
            } else if ((AccessTokenRecord.class == clazz && SharedPreferencesAccountCredentialCache.EMPTY_AT.equals(credential))
                || (RefreshTokenRecord.class == clazz && SharedPreferencesAccountCredentialCache.EMPTY_RT.equals(credential))
                || (IdTokenRecord.class == clazz) && SharedPreferencesAccountCredentialCache.EMPTY_ID.equals(credential)) {
                // The returned credential came back uninitialized...
                // Remove the entry and return null...
                Logger.warn(methodTag, "The returned Credential was uninitialized. Removing...");
                mSharedPreferencesFileManager.remove(cacheKey);
            }
            else {
                credentials.put(cacheKey, credential);
            }
        }

        Logger.verbose(methodTag, "Loaded [" + credentials.size() + "] Credentials...");

        return credentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentials() {
        final String methodTag = TAG + ":getCredentials";
        Logger.verbose(methodTag, "Loading Credentials...");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            ArrayList<Credential> credentials = new ArrayList<>();
            for (Credential credential : mCachedCredentialsWithKeys.values()) {
                try {
                    credentials.add((Credential)credential.clone());
                } catch (final CloneNotSupportedException e) {
                    Logger.error(methodTag, "Failed to clone Credential", e);
                }
            }
            return credentials;
        }
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> allCredentials = getCredentials();

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                allCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                null,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy() -- with input list");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                null,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> allCredentials = getCredentials();

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                allCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                requestedClaims,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                requestedClaims,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            final boolean mustMatchExactClaims,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                requestedClaims,
                null,
                mustMatchExactClaims
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(@Nullable final String homeAccountId,
                                                     @Nullable final String environment,
                                                     @NonNull final Set<CredentialType> credentialTypes,
                                                     @Nullable final String clientId,
                                                     @Nullable final String applicationIdentifier,
                                                     @Nullable final String mamEnrollmentIdentifier,
                                                     @Nullable final String realm,
                                                     @Nullable final String target,
                                                     @Nullable final String authScheme,
                                                     @Nullable final String requestedClaims) {
        final List<Credential> allCredentials = getCredentials();

        final List<Credential> result = new ArrayList<>();
        for (final CredentialType type : credentialTypes) {
            result.addAll(
                    getCredentialsFilteredByInternal(
                            allCredentials,
                            homeAccountId,
                            environment,
                            type,
                            clientId,
                            applicationIdentifier,
                            mamEnrollmentIdentifier,
                            realm,
                            target,
                            authScheme,
                            requestedClaims,
                            null,
                            false
                    )
            );
        }

        return result;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(
            @NonNull List<Credential> inputCredentials,
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            @Nullable final String kid ) {
        final List<Credential> result = new ArrayList<>();
        result.addAll(
                getCredentialsFilteredByInternal(
                        inputCredentials,
                        homeAccountId,
                        environment,
                        credentialType,
                        clientId,
                        applicationIdentifier,
                        mamEnrollmentIdentifier,
                        realm,
                        target,
                        authScheme,
                        requestedClaims,
                        kid,
                        false
                )
        );
        return result;
    }

    @Override
    public boolean removeAccount(@NonNull final AccountRecord accountToRemove) {
        final String methodTag = TAG + ":removeAccount";
        Logger.info(methodTag, "Removing Account...");
        if (null == accountToRemove) {
            throw new IllegalArgumentException("Param [accountToRemove] cannot be null.");
        }

        final String cacheKey = mCacheValueDelegate.generateCacheKey(accountToRemove);

        synchronized (mCacheLock) {
            waitForInitialLoad();
            boolean accountRemoved = false;
            if (mSharedPreferencesFileManager.keySet().contains(cacheKey))
            {
                mSharedPreferencesFileManager.remove(cacheKey);
                accountRemoved = true;
            }
            Logger.info(methodTag, "Account was removed? [" + accountRemoved + "]");

            mCachedAccountRecordsWithKeys.remove(cacheKey);

            return accountRemoved;
        }
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credentialToRemove) {
        final String methodTag = TAG + ":removeCredential";
        Logger.info(methodTag, "Removing Credential...");

        if (null == credentialToRemove) {
            throw new IllegalArgumentException("Param [credentialToRemove] cannot be null.");
        }

        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToRemove);

        synchronized (mCacheLock) {
            waitForInitialLoad();
            boolean credentialRemoved = false;
            if (mSharedPreferencesFileManager.keySet().contains(cacheKey)) {
                mSharedPreferencesFileManager.remove(cacheKey);
                credentialRemoved = true;
            }

            Logger.info(methodTag, "Credential was removed? [" + credentialRemoved + "]");

            mCachedCredentialsWithKeys.remove(cacheKey);

            return credentialRemoved;
        }

    }

    @Override
    public void clearAll() {
        final String methodTag = TAG + ":clearAll";
        Logger.info(methodTag, "Clearing all SharedPreferences entries...");
        synchronized (mCacheLock) {
            waitForInitialLoad();
            mSharedPreferencesFileManager.clear();
            mCachedCredentialsWithKeys.clear();
            mCachedAccountRecordsWithKeys.clear();
        }
        Logger.info(methodTag, "SharedPreferences cleared.");
    }

    @Nullable
    private Class<? extends Credential> credentialClassForType(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":credentialClassForType";
        Logger.verbose(methodTag, "Resolving class for key/CredentialType...");
        Logger.verbosePII(methodTag, "Supplied key: [" + cacheKey + "]");

        final CredentialType targetType = getCredentialTypeForCredentialCacheKey(cacheKey);

        if (targetType == null) {
            return null;
        }

        Logger.verbose(methodTag, "CredentialType matched: [" + targetType + "]");

        return getTargetClassForCredentialType(cacheKey, targetType);
    }

    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    @Nullable
    public static CredentialType getCredentialTypeForCredentialCacheKey(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getCredentialTypeForCredentialCacheKey";
        if (StringUtil.isNullOrEmpty(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR + credentialTypeStr + CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR)) {
                if (CredentialType.AccessToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.AccessToken;
                    break;
                } else if (CredentialType.AccessToken_With_AuthScheme.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.AccessToken_With_AuthScheme;
                    break;
                } else if (CredentialType.RefreshToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.RefreshToken;
                    break;
                } else if (CredentialType.IdToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.IdToken;
                    break;
                } else if (CredentialType.V1IdToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.V1IdToken;
                    break;
                } else if (CredentialType.PrimaryRefreshToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.PrimaryRefreshToken;
                    break;
                } else {
                    // TODO Log a warning and skip this value?
                    Logger.warn(methodTag, "Unexpected credential type.");
                }
            }
        }

        return type;
    }

    private static boolean isAccount(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":isAccount";
        Logger.verbosePII(methodTag, "Evaluating cache key: [" + cacheKey + "]");
        boolean isAccount = null == getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.verbose(methodTag, "isAccount? [" + isAccount + "]");
        return isAccount;
    }

    private static boolean isCredential(@NonNull String cacheKey) {
        final String methodTag = TAG + ":isCredential";
        Logger.verbosePII(methodTag, "Evaluating cache key: [" + cacheKey + "]");
        boolean isCredential = null != getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.verbose(methodTag, "isCredential? [" + isCredential + "]");
        return isCredential;
    }
}
