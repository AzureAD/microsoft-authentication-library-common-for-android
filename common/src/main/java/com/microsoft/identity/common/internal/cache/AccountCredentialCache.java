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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AccountCredentialCache implements IAccountCredentialCache {

    private static final String TAG = AccountCredentialCache.class.getSimpleName();

    /**
     * The name of the SharedPreferences file on disk.
     */
    public static final String DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.account_credential_cache";

    private static final AccountRecord EMPTY_ACCOUNT = new AccountRecord();
    private static final AccessTokenRecord EMPTY_AT = new AccessTokenRecord();
    private static final RefreshTokenRecord EMPTY_RT = new RefreshTokenRecord();
    private static final IdToken EMPTY_ID = new IdToken();

    // SharedPreferences used to store Accounts and Credentials
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    private final ICacheKeyValueDelegate mCacheValueDelegate;

    /**
     * Constructor of AccountCredentialCache.
     *
     * @param accountCacheValueDelegate    ICacheKeyValueDelegate
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    public AccountCredentialCache(
            @NonNull final ICacheKeyValueDelegate accountCacheValueDelegate,
            @NonNull final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        Logger.verbose(TAG, "Init: " + TAG);
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    @Override
    public synchronized void saveAccount(@NonNull final AccountRecord account) {
        Logger.verbose(TAG, "Saving Account...");
        Logger.verbose(TAG, "Account type: [" + account.getClass().getSimpleName() + "]");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(account);
        Logger.verbosePII(TAG, "Generated cache key: [" + cacheKey + "]");
        final String cacheValue = mCacheValueDelegate.generateCacheValue(account);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public synchronized void saveCredential(@NonNull Credential credential) {
        Logger.verbose(TAG, "Saving credential...");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credential);
        Logger.verbosePII(TAG, "Generated cache key: [" + cacheKey + "]");
        final String cacheValue = mCacheValueDelegate.generateCacheValue(credential);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public synchronized AccountRecord getAccount(@NonNull final String cacheKey) {
        Logger.verbose(TAG, "Loading Account by key...");
        AccountRecord account = mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.getString(cacheKey),
                AccountRecord.class
        );

        if (null == account || EMPTY_ACCOUNT.equals(account)) { // Either we found nothing or it wasn't an Account
            // The returned account came back uninitialized...
            // Remove the entry and return null...
            Logger.warn(TAG, "The returned Account was uninitialized. Removing...");
            mSharedPreferencesFileManager.remove(cacheKey);
            account = null;
        }

        return account;
    }

    @Override
    public synchronized Credential getCredential(@NonNull final String cacheKey) {
        // TODO add support for more Credential types...
        Logger.verbose(TAG, "getCredential()");
        Logger.verbosePII(TAG, "Using cache key: [" + cacheKey + "]");
        final CredentialType type = getCredentialTypeForCredentialCacheKey(cacheKey);
        final Class<? extends Credential> clazz;
        if (CredentialType.AccessToken == type) {
            clazz = AccessTokenRecord.class;
        } else if (CredentialType.RefreshToken == type) {
            clazz = RefreshTokenRecord.class;
        } else if (CredentialType.IdToken == type) {
            clazz = IdToken.class;
        } else {
            // TODO Log a warning, throw an Exception?
            throw new RuntimeException("Credential type could not be resolved.");
        }

        Credential credential = mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.getString(cacheKey),
                clazz
        );

        if (null == credential
                || (AccessTokenRecord.class == clazz && EMPTY_AT.equals(credential))
                || (RefreshTokenRecord.class == clazz && EMPTY_RT.equals(credential))
                || (IdToken.class == clazz) && EMPTY_ID.equals(credential)) {
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
        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final Map<String, AccountRecord> accounts = new HashMap<>();

        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccount(cacheKey)) {
                final AccountRecord account = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        AccountRecord.class
                );
                accounts.put(cacheKey, account);
            }
        }

        Logger.verbose(TAG, "Returning [" + accounts.size() + "] Accounts w/ keys...");

        return accounts;
    }

    @Override
    @NonNull
    public synchronized List<AccountRecord> getAccounts() {
        Logger.verbose(TAG, "Loading Accounts...(no arg)");
        final Map<String, AccountRecord> allAccounts = getAccountsWithKeys();
        final List<AccountRecord> accounts = new ArrayList<>(allAccounts.values());
        Logger.info(TAG, "Found [" + accounts.size() + "] Accounts...");
        return accounts;
    }

    @Override
    @NonNull
    public List<AccountRecord> getAccountsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final String realm) {
        Logger.verbose(TAG, "Loading Accounts...");

        final boolean mustMatchOnHomeAccountId = !StringExtensions.isNullOrBlank(homeAccountId);
        final boolean mustMatchOnEnvironment = !StringExtensions.isNullOrBlank(environment);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);

        Logger.verbose(TAG, "Account lookup filtered by home_account_id? [" + mustMatchOnHomeAccountId + "]");
        Logger.verbose(TAG, "Account lookup filtered by realm? [" + mustMatchOnRealm + "]");

        final List<AccountRecord> allAccounts = getAccounts();
        final List<AccountRecord> matchingAccounts = new ArrayList<>();

        for (final AccountRecord account : allAccounts) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = homeAccountId.equalsIgnoreCase(account.getHomeAccountId());
            }

            if (mustMatchOnEnvironment) {
                matches = matches && environment.equalsIgnoreCase(account.getEnvironment());
            }

            if (mustMatchOnRealm) {
                matches = matches && realm.equalsIgnoreCase(account.getRealm());
            }

            if (matches) {
                matchingAccounts.add(account);
            }
        }

        Logger.info(TAG, "Found [" + matchingAccounts.size() + "] matching Accounts...");

        return matchingAccounts;
    }

    @NonNull
    private Map<String, Credential> getCredentialsWithKeys() {
        Logger.verbose(TAG, "Loading Credentials with keys...");
        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final Map<String, Credential> credentials = new HashMap<>();

        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isCredential(cacheKey)) {
                final Credential credential = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        credentialClassForType(cacheKey)
                );
                credentials.put(cacheKey, credential);
            }
        }

        Logger.verbose(TAG, "Loaded [" + credentials.size() + "] Credentials...");

        return credentials;
    }

    @Override
    @NonNull
    public synchronized List<Credential> getCredentials() {
        Logger.verbose(TAG, "Loading Credentials...");
        final Map<String, Credential> allCredentials = getCredentialsWithKeys();
        final List<Credential> creds = new ArrayList<>(allCredentials.values());
        Logger.verbose(TAG, "Found [" + creds.size() + "] Credentials");
        return creds;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @NonNull final CredentialType credentialType,
            @NonNull final String clientId,
            @Nullable final String realm,
            @Nullable final String target) {
        Logger.verbose(TAG, "getCredentialsFilteredBy()");

        if (null == credentialType) {
            throw new IllegalArgumentException("Param [credentialType] cannot be null.");
        }

        if (StringExtensions.isNullOrBlank(clientId)) {
            throw new IllegalArgumentException("Param [clientId] cannot be null.");
        }

        final boolean mustMatchOnEnvironment = !StringExtensions.isNullOrBlank(environment);
        final boolean mustMatchOnHomeAccountId = !StringExtensions.isNullOrBlank(homeAccountId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final boolean mustMatchOnTarget = !StringExtensions.isNullOrBlank(target);

        Logger.verbose(TAG, "Credential lookup filtered by home_account_id? [" + mustMatchOnHomeAccountId + "]");
        Logger.verbose(TAG, "Credential lookup filtered by realm? [" + mustMatchOnRealm + "]");
        Logger.verbose(TAG, "Credential lookup filtered by target? [" + mustMatchOnTarget + "]");

        Logger.verbose(TAG, "Loading Credentials...");
        final List<Credential> allCredentials = getCredentials();
        final List<Credential> matchingCredentials = new ArrayList<>();

        for (final Credential credential : allCredentials) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = homeAccountId.equalsIgnoreCase(credential.getHomeAccountId());
            }

            if (mustMatchOnEnvironment) {
                matches = matches && environment.equalsIgnoreCase(credential.getEnvironment());
            }

            matches = matches && credentialType.name().equalsIgnoreCase(credential.getCredentialType());
            matches = matches && clientId.equalsIgnoreCase(credential.getClientId());

            if (mustMatchOnRealm && credential instanceof AccessTokenRecord) {
                final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                matches = matches && realm.equalsIgnoreCase(accessToken.getRealm());
            }

            if (mustMatchOnTarget) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                    matches = matches && targetsIntersect(target, accessToken.getTarget());
                } else if (credential instanceof RefreshTokenRecord) {
                    final RefreshTokenRecord refreshToken = (RefreshTokenRecord) credential;
                    matches = matches && targetsIntersect(target, refreshToken.getTarget());
                } else {
                    Logger.warn(TAG, "Query specified target-match, but no target to match.");
                }
            }

            if (matches) {
                matchingCredentials.add(credential);
            }
        }

        Logger.info(TAG, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    /**
     * Examines the intersections of the provided targets (scopes).
     *
     * @param targetToMatch    The target value[s] our cache-query is looking for.
     * @param credentialTarget The target against which our sought value will be compared.
     * @return True, if the credentialTarget contains all of the targets (scopes) declared by
     * targetToMatch. False otherwise.
     */
    private boolean targetsIntersect(@NonNull final String targetToMatch,
                                     @NonNull final String credentialTarget) {
        // The credentialTarget must contain all of the scopes in the targetToMatch
        // It may contain more, but it must contain minimally those
        // Matching is case-insensitive
        final String splitCriteria = "\\s+";
        final String[] targetToMatchArray = targetToMatch.split(splitCriteria);
        final String[] credentialTargetArray = credentialTarget.split(splitCriteria);

        // Declare Sets to contain these scopes
        final Set<String> soughtTargetSet = new HashSet<>();
        final Set<String> credentialTargetSet = new HashSet<>();

        // Add the array values to these sets, lowercasing them
        for (final String target : targetToMatchArray) {
            soughtTargetSet.add(target.toLowerCase());
        }

        for (final String target : credentialTargetArray) {
            credentialTargetSet.add(target.toLowerCase());
        }

        return credentialTargetSet.containsAll(soughtTargetSet);
    }

    @Override
    public boolean removeAccount(@NonNull final AccountRecord accountToRemove) {
        Logger.info(TAG, "Removing Account...");
        if (null == accountToRemove) {
            throw new IllegalArgumentException("Param [accountToRemove] cannot be null.");
        }

        Logger.verbose(TAG, "Loading Accounts + keys...");
        final Map<String, AccountRecord> accounts = getAccountsWithKeys();
        Logger.info(TAG, "Found [" + accounts.size() + "] Accounts...");

        boolean accountRemoved = false;
        for (final Map.Entry<String, AccountRecord> entry : accounts.entrySet()) {
            Logger.infoPII(TAG, "Inspecting: [" + entry.getKey() + "]");
            final IAccountRecord currentAccount = entry.getValue();

            if (currentAccount.equals(accountToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                accountRemoved = true;
                break;
            }
        }

        Logger.info(TAG, "Account was removed? [" + accountRemoved + "]");

        return accountRemoved;
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credentialToRemove) {
        Logger.info(TAG, "Removing Credential...");
        if (null == credentialToRemove) {
            throw new IllegalArgumentException("Param [credentialToRemove] cannot be null.");
        }

        Logger.verbose(TAG, "Loading Credentials + keys...");
        final Map<String, Credential> credentials = getCredentialsWithKeys();
        Logger.info(TAG, "Found [" + credentials.size() + "] Credentials...");

        boolean credentialRemoved = false;
        for (final Map.Entry<String, Credential> entry : credentials.entrySet()) {
            Logger.infoPII(TAG, "Inspecting: [" + entry.getKey() + "]");
            final Credential currentCredential = entry.getValue();

            if (currentCredential.equals(credentialToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                credentialRemoved = true;
                break;
            }
        }

        Logger.info(TAG, "Credential was removed? [" + credentialRemoved + "]");

        return credentialRemoved;
    }

    @Override
    public void clearAll() {
        Logger.info(TAG, "Clearing all SharedPreferences entries...");
        mSharedPreferencesFileManager.clear();
        Logger.info(TAG, "SharedPreferences cleared.");
    }

    private Class<? extends Credential> credentialClassForType(@NonNull final String cacheKey) {
        Logger.verbose(TAG, "Resolving class for key/CredentialType...");
        Logger.verbosePII(TAG, "Supplied key: [" + cacheKey + "]");

        final CredentialType targetType = getCredentialTypeForCredentialCacheKey(cacheKey);

        Logger.verbose(TAG, "CredentialType matched: [" + targetType + "]");

        Class<? extends Credential> credentialClass = null;
        switch (targetType) {
            case AccessToken:
                credentialClass = AccessTokenRecord.class;
                break;
            case RefreshToken:
                credentialClass = RefreshTokenRecord.class;
                break;
            case IdToken:
                credentialClass = IdToken.class;
                break;
            default:
                Logger.warn(TAG, "Could not match CredentialType to class."
                        + "Did you forget to update this method with a new type?");
                Logger.warnPII(TAG, "Sought key was: [" + cacheKey + "]");
        }

        return credentialClass;
    }

    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    private CredentialType getCredentialTypeForCredentialCacheKey(@NonNull final String cacheKey) {
        if (StringExtensions.isNullOrBlank(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        Logger.verbosePII(TAG, "Evaluating cache key for CredentialType [" + cacheKey + "]");

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        Logger.info(TAG, "Comparing cache key to known types...");

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CACHE_VALUE_SEPARATOR + credentialTypeStr + CACHE_VALUE_SEPARATOR)) {
                Logger.info(TAG, "Cache key is a Credential type...");

                if (credentialTypeStr.equalsIgnoreCase(CredentialType.AccessToken.name())) {
                    type = CredentialType.AccessToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                    type = CredentialType.RefreshToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.IdToken.name())) {
                    type = CredentialType.IdToken;
                    break;
                } else {
                    // TODO Log a warning and skip this value?
                    Logger.warn(TAG, "Unexpected credential type.");
                }
            }
        }

        Logger.info(TAG, "Cache key was type: [" + type + "]");

        return type;
    }

    private boolean isAccount(@NonNull final String cacheKey) {
        Logger.verbosePII(TAG, "Evaluating cache key: [" + cacheKey + "]");
        boolean isAccount = null == getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.info(TAG, "isAccount? [" + isAccount + "]");
        return isAccount;
    }

    private boolean isCredential(@NonNull String cacheKey) {
        Logger.verbosePII(TAG, "Evaluating cache key: [" + cacheKey + "]");
        boolean isCredential = null != getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.info(TAG, "isCredential? [" + isCredential + "]");
        return isCredential;
    }

}
