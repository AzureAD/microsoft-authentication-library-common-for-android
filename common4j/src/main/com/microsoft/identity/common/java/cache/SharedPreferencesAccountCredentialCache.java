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

import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.java.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SharedPreferencesAccountCredentialCache extends AbstractAccountCredentialCache {

    private static final String TAG = SharedPreferencesAccountCredentialCache.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk.
     */
    public static final String DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.account_credential_cache";

    /**
     * The name of the Broker FOCI file on disk.
     */
    public static final String BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES
                    + ".foci-1";

    /**
     * Returns the generated filename for UID-specific caches.
     *
     * @param uid The uid of the current (or targeted) application.
     * @return The uid-based cache filename.
     */
    public static String getBrokerUidSequesteredFilename(final int uid) {
        return DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES
                + ".uid-"
                + uid;
    }

    private static final AccountRecord EMPTY_ACCOUNT = new AccountRecord();
    private static final AccessTokenRecord EMPTY_AT = new AccessTokenRecord();
    private static final RefreshTokenRecord EMPTY_RT = new RefreshTokenRecord();
    private static final IdTokenRecord EMPTY_ID = new IdTokenRecord();
    private static final String DESERIALIZATION_FAILED = "Deserialization failed. Skipping ";
    private static final String ACCOUNT_RECORD_DESERIALIZATION_FAILED = DESERIALIZATION_FAILED + AccountRecord.class.getSimpleName();
    private static final String CREDENTIAL_DESERIALIZATION_FAILED = DESERIALIZATION_FAILED + Credential.class.getSimpleName();

    // SharedPreferences used to store Accounts and Credentials
    private final INameValueStorage<String> mSharedPreferencesFileManager;

    private final ICacheKeyValueDelegate mCacheValueDelegate;

    /**
     * Constructor of SharedPreferencesAccountCredentialCache.
     *
     * @param accountCacheValueDelegate    ICacheKeyValueDelegate
     * @param sharedPreferencesFileManager INameValueStorage
     */
    public SharedPreferencesAccountCredentialCache(
            @NonNull final ICacheKeyValueDelegate accountCacheValueDelegate,
            @NonNull final INameValueStorage<String> sharedPreferencesFileManager) {
        Logger.verbose(TAG, "Init: " + TAG);
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    @Override
    public synchronized void saveAccount(@NonNull final AccountRecord accountToSave) {
        Logger.verbose(TAG, "Saving Account...");
        Logger.verbose(TAG, "Account type: [" + accountToSave.getClass().getSimpleName() + "]");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(accountToSave);
        Logger.verbosePII(TAG, "Generated cache key: [" + cacheKey + "]");

        // Perform any necessary field merging on the Account to save...
        final AccountRecord existingAccount = getAccount(cacheKey);

        if (null != existingAccount) {
            accountToSave.mergeAdditionalFields(existingAccount);
        }

        final String cacheValue = mCacheValueDelegate.generateCacheValue(accountToSave);
        mSharedPreferencesFileManager.put(cacheKey, cacheValue);
    }

    @Override
    public synchronized void saveCredential(@NonNull Credential credentialToSave) {
        Logger.verbose(TAG, "Saving credential...");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToSave);
        Logger.verbosePII(TAG, "Generated cache key: [" + cacheKey + "]");

        // Perform any necessary field merging on the Credential to save...
        final Credential existingCredential = getCredential(cacheKey);

        if (null != existingCredential) {
            credentialToSave.mergeAdditionalFields(existingCredential);
        }

        final String cacheValue = mCacheValueDelegate.generateCacheValue(credentialToSave);
        mSharedPreferencesFileManager.put(cacheKey, cacheValue);
    }

    @Override
    public synchronized AccountRecord getAccount(@NonNull final String cacheKey) {
        Logger.verbose(TAG, "Loading Account by key...");
        AccountRecord account = mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.get(cacheKey),
                AccountRecord.class
        );

        if (null == account) {
            // We could not deserialize the target AccountRecord...
            // Maybe it was encrypted for another application?
            Logger.warn(
                    TAG,
                    ACCOUNT_RECORD_DESERIALIZATION_FAILED
            );
        } else if (EMPTY_ACCOUNT.equals(account)) {
            Logger.warn(TAG, "The returned Account was uninitialized. Removing...");
            mSharedPreferencesFileManager.remove(cacheKey);
            account = null;
        }

        return account;
    }

    @Override
    @Nullable
    public synchronized Credential getCredential(@NonNull final String cacheKey) {
        // TODO add support for more Credential types...
        Logger.verbose(TAG, "getCredential()");
        Logger.verbosePII(TAG, "Using cache key: [" + cacheKey + "]");

        final CredentialType type = getCredentialTypeForCredentialCacheKey(cacheKey);
        Class<? extends Credential> clazz = null;

        if (null != type) {
            clazz = getTargetClassForCredentialType(cacheKey, type);
        }

        Credential credential = null;

        if (null != clazz) {
            credential = mCacheValueDelegate.fromCacheValue(
                    mSharedPreferencesFileManager.get(cacheKey),
                    clazz
            );
        }

        if (null == credential) {
            // We could not deserialize the target Credential...
            // Maybe it was encrypted for another application?
            Logger.warn(
                    TAG,
                    CREDENTIAL_DESERIALIZATION_FAILED
            );
        } else if ((AccessTokenRecord.class == clazz && EMPTY_AT.equals(credential))
                || (RefreshTokenRecord.class == clazz && EMPTY_RT.equals(credential))
                || (IdTokenRecord.class == clazz) && EMPTY_ID.equals(credential)) {
            // The returned credential came back uninitialized...
            // Remove the entry and return null...
            Logger.warn(TAG, "The returned Credential was uninitialized. Removing...");
            mSharedPreferencesFileManager.remove(cacheKey);
            credential = null;
        }

        return credential;
    }

    @NonNull
    private Map<String, AccountRecord> getAccountsWithKeys() {
        Logger.verbose(TAG, "Loading Accounts + keys...");
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
                    Logger.warn(TAG, ACCOUNT_RECORD_DESERIALIZATION_FAILED);
                } else {
                    accounts.put(cacheKey, account);
                }
            }
        }

        Logger.verbose(TAG, "Returning [" + accounts.size() + "] Accounts w/ keys...");

        return accounts;
    }

    @Override
    @NonNull
    public synchronized List<AccountRecord> getAccounts() {
        final String methodTag = TAG + ":getAccounts";
        Logger.verbose(methodTag, "Loading Accounts...(no arg)");
        final Map<String, AccountRecord> allAccounts = getAccountsWithKeys();
        final List<AccountRecord> accounts = new ArrayList<>(allAccounts.values());
        Logger.info(methodTag, "Found [" + accounts.size() + "] Accounts...");
        return accounts;
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
    private Map<String, Credential> getCredentialsWithKeys() {
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
            final Credential credential = mCacheValueDelegate.fromCacheValue(
                    cacheValue.getValue().toString(),
                    credentialClassForType(cacheKey)
            );

            if (null == credential) {
                Logger.warn(methodTag, CREDENTIAL_DESERIALIZATION_FAILED);
            } else {
                credentials.put(cacheKey, credential);
            }
        }

        Logger.verbose(methodTag, "Loaded [" + credentials.size() + "] Credentials...");

        return credentials;
    }

    @Override
    @NonNull
    public synchronized List<Credential> getCredentials() {
        final String methodTag = TAG + ":getCredentials";
        Logger.verbose(methodTag, "Loading Credentials...");
        final Map<String, Credential> allCredentials = getCredentialsWithKeys();
        final List<Credential> creds = new ArrayList<>(allCredentials.values());
        return creds;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> allCredentials = getCredentials();

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                homeAccountId,
                environment,
                credentialType,
                clientId,
                realm,
                target,
                authScheme,
                null,
                allCredentials
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
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy() -- with input list");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                homeAccountId,
                environment,
                credentialType,
                clientId,
                realm,
                target,
                authScheme,
                null,
                inputCredentials
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
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> allCredentials = getCredentials();

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                homeAccountId,
                environment,
                credentialType,
                clientId,
                realm,
                target,
                authScheme,
                requestedClaims,
                allCredentials
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
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            @Nullable final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(TAG, "getCredentialsFilteredBy()");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                homeAccountId,
                environment,
                credentialType,
                clientId,
                realm,
                target,
                authScheme,
                requestedClaims,
                inputCredentials
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(@Nullable final String homeAccountId,
                                                     @Nullable final String environment,
                                                     @NonNull final Set<CredentialType> credentialTypes,
                                                     @Nullable final String clientId,
                                                     @Nullable final String realm,
                                                     @Nullable final String target,
                                                     @Nullable final String authScheme,
                                                     @Nullable final String requestedClaims) {
        final List<Credential> allCredentials = getCredentials();

        final List<Credential> result = new ArrayList<>();
        for (final CredentialType type : credentialTypes) {
            result.addAll(
                    getCredentialsFilteredByInternal(
                            homeAccountId,
                            environment,
                            type,
                            clientId,
                            realm,
                            target,
                            authScheme,
                            requestedClaims,
                            allCredentials
                    )
            );
        }

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

        boolean accountRemoved = false;
        if (mSharedPreferencesFileManager.keySet().contains(cacheKey))
        {
            mSharedPreferencesFileManager.remove(cacheKey);
            accountRemoved = true;
        }

        Logger.info(methodTag, "Account was removed? [" + accountRemoved + "]");

        return accountRemoved;
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credentialToRemove) {
        final String methodTag = TAG + ":removeCredential";
        Logger.info(methodTag, "Removing Credential...");

        if (null == credentialToRemove) {
            throw new IllegalArgumentException("Param [credentialToRemove] cannot be null.");
        }

        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToRemove);

        boolean credentialRemoved = false;
        if (mSharedPreferencesFileManager.keySet().contains(cacheKey))
        {
            mSharedPreferencesFileManager.remove(cacheKey);
            credentialRemoved = true;
        }

        Logger.info(methodTag, "Credential was removed? [" + credentialRemoved + "]");

        return credentialRemoved;
    }

    @Override
    public void clearAll() {
        final String methodTag = TAG + ":clearAll";
        Logger.info(methodTag, "Clearing all SharedPreferences entries...");
        mSharedPreferencesFileManager.clear();
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

        Logger.verbosePII(methodTag, "Evaluating cache key for CredentialType [" + cacheKey + "]");

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR + credentialTypeStr + CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR)) {
                Logger.verbose(methodTag, "Cache key is a Credential type...");

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

        Logger.verbose(methodTag, "Cache key was type: [" + type + "]");

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
