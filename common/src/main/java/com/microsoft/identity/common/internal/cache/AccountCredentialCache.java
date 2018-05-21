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
import android.support.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IAccount;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

public class AccountCredentialCache implements IAccountCredentialCache {

    private static final String TAG = AccountCredentialCache.class.getSimpleName();

    // The names of the SharedPreferences file on disk.
    private static final String ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.account_credential_cache";

    private static final Account EMPTY_ACCOUNT = new Account();
    private static final AccessToken EMPTY_AT = new AccessToken();
    private static final RefreshToken EMPTY_RT = new RefreshToken();
    private static final IdToken EMPTY_ID = new IdToken();

    // SharedPreferences used to store Accounts and Credentials
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    private final Context mContext;
    private final ICacheKeyValueDelegate mCacheValueDelegate;

    /**
     * Constructor of AccountCredentialCache.
     *
     * @param context                   Context
     * @param accountCacheValueDelegate ICacheKeyValueDelegate
     */
    public AccountCredentialCache(
            final Context context,
            final ICacheKeyValueDelegate accountCacheValueDelegate) {
        Logger.verbose(TAG, "Init: " + TAG);
        mContext = context;
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(
                mContext,
                ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(mContext)
        );
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    /**
     * Constructor of AccountCredentialCache.
     *
     * @param context                      Context
     * @param accountCacheValueDelegate    ICacheKeyValueDelegate
     * @param sharedPreferencesFileManager ISharedPreferencesFileManager
     */
    public AccountCredentialCache(
            final Context context,
            final ICacheKeyValueDelegate accountCacheValueDelegate,
            final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        Logger.verbose(TAG, "Init: " + TAG);
        mContext = context;
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    @Override
    public synchronized void saveAccount(final Account account) {
        final String methodName = "saveAccount";
        Logger.entering(TAG, methodName, account);

        final String cacheKey = mCacheValueDelegate.generateCacheKey(account);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(account);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);

        Logger.exiting(TAG, methodName);
    }

    @Override
    public synchronized void saveCredential(Credential credential) {
        final String methodName = "saveCredential";
        Logger.entering(TAG, methodName, credential);

        final String cacheKey = mCacheValueDelegate.generateCacheKey(credential);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(credential);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);

        Logger.exiting(TAG, methodName);
    }

    @Override
    public synchronized Account getAccount(final String cacheKey) {
        final String methodName = "getAccount";
        Logger.entering(TAG, methodName, cacheKey);

        Account account = mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.getString(cacheKey),
                Account.class
        );

        if (null == account || EMPTY_ACCOUNT.equals(account)) { // Either we found nothing or it wasn't an Account
            // The returned account came back uninitialized...
            // Remove the entry and return null...
            mSharedPreferencesFileManager.remove(cacheKey);
            account = null;
        }

        Logger.exiting(TAG, methodName, account);

        return account;
    }

    @Override
    public synchronized Credential getCredential(final String cacheKey) {
        final String methodName = "getCredential";
        Logger.entering(TAG, methodName, cacheKey);

        // TODO add support for more Credential types...
        final CredentialType type = getCredentialTypeForCredentialCacheKey(cacheKey);
        final Class<? extends Credential> clazz;
        if (CredentialType.AccessToken == type) {
            clazz = AccessToken.class;
        } else if (CredentialType.RefreshToken == type) {
            clazz = RefreshToken.class;
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
                || (AccessToken.class == clazz && EMPTY_AT.equals(credential))
                || (RefreshToken.class == clazz && EMPTY_RT.equals(credential))
                || (IdToken.class == clazz) && EMPTY_ID.equals(credential)) {
            // The returned credential came back uninitialized...
            // Remove the entry and return null...
            mSharedPreferencesFileManager.remove(cacheKey);
            credential = null;
        }

        Logger.exiting(TAG, methodName, credential);

        return credential;
    }

    private Map<String, Account> getAccountsWithKeys() {
        final String methodName = "getAccountsWithKeys";
        Logger.entering(TAG, methodName);

        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final Map<String, Account> accounts = new HashMap<>();

        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccount(cacheKey)) {
                final Account account = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        Account.class
                );
                accounts.put(cacheKey, account);
            }
        }

        Logger.exiting(TAG, methodName, accounts);

        return accounts;
    }

    @Override
    public synchronized List<Account> getAccounts() {
        final String methodName = "getAccounts";
        Logger.entering(TAG, methodName);

        final Map<String, Account> allAccounts = getAccountsWithKeys();
        final List<Account> accounts = new ArrayList<>(allAccounts.values());

        Logger.exiting(TAG, methodName, accounts);

        return accounts;
    }

    @Override
    public List<Account> getAccounts(
            final @Nullable String homeAccountId,
            final String environment,
            final @Nullable String realm) {
        final String methodName = "getAccounts";
        Logger.entering(TAG, methodName, homeAccountId, environment, realm);

        if (StringExtensions.isNullOrBlank(environment)) {
            throw new IllegalArgumentException("Param [environment] cannot be null.");
        }

        final boolean mustMatchOnHomeAccountId = !StringExtensions.isNullOrBlank(homeAccountId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final List<Account> allAccounts = getAccounts();
        final List<Account> matchingAccounts = new ArrayList<>();

        for (final Account account : allAccounts) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = homeAccountId.equalsIgnoreCase(account.getHomeAccountId());
            }

            matches = matches && environment.equalsIgnoreCase(account.getEnvironment());

            if (mustMatchOnRealm) {
                matches = matches && realm.equalsIgnoreCase(account.getRealm());
            }

            if (matches) {
                matchingAccounts.add(account);
            }
        }

        Logger.exiting(TAG, methodName, matchingAccounts);

        return matchingAccounts;
    }

    private Map<String, Credential> getCredentialsWithKeys() {
        final String methodName = "getCredentialsWithKeys";
        Logger.entering(TAG, methodName);

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

        Logger.exiting(TAG, methodName, credentials);

        return credentials;
    }

    @Override
    public synchronized List<Credential> getCredentials() {
        final String methodName = "getCredentials";
        Logger.entering(TAG, methodName);

        final Map<String, Credential> allCredentials = getCredentialsWithKeys();
        final List<Credential> creds = new ArrayList<>(allCredentials.values());

        Logger.exiting(TAG, methodName, creds);

        return creds;
    }

    @Override
    public List<Credential> getCredentials(
            final @Nullable String homeAccountId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            final @Nullable String realm,
            final @Nullable String target) {
        final String methodName = "getCredentials";
        Logger.entering(TAG, methodName, homeAccountId, environment, credentialType, clientId, realm, target);

        if (StringExtensions.isNullOrBlank(environment)) {
            throw new IllegalArgumentException("Param [environment] cannot be null.");
        }

        if (null == credentialType) {
            throw new IllegalArgumentException("Param [credentialType] cannot be null.");
        }

        if (StringExtensions.isNullOrBlank(clientId)) {
            throw new IllegalArgumentException("Param [clientId] cannot be null.");
        }

        final boolean mustMatchOnHomeAccountId = !StringExtensions.isNullOrBlank(homeAccountId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final boolean mustMatchOnTarget = !StringExtensions.isNullOrBlank(target);
        final List<Credential> allCredentials = getCredentials();
        final List<Credential> matchingCredentials = new ArrayList<>();

        for (final Credential credential : allCredentials) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = homeAccountId.equalsIgnoreCase(credential.getHomeAccountId());
            }

            matches = matches && environment.equalsIgnoreCase(credential.getEnvironment());
            matches = matches && credentialType.name().equalsIgnoreCase(credential.getCredentialType());
            matches = matches && clientId.equalsIgnoreCase(credential.getClientId());

            if (mustMatchOnRealm && credential instanceof AccessToken) {
                final AccessToken accessToken = (AccessToken) credential;
                matches = matches && realm.equalsIgnoreCase(accessToken.getRealm());
            }

            if (mustMatchOnTarget) {
                if (credential instanceof AccessToken) {
                    final AccessToken accessToken = (AccessToken) credential;
                    matches = matches && target.equalsIgnoreCase(accessToken.getTarget());
                } else if (credential instanceof RefreshToken) {
                    final RefreshToken refreshToken = (RefreshToken) credential;
                    matches = matches && target.equalsIgnoreCase(refreshToken.getTarget());
                }
            }

            if (matches) {
                matchingCredentials.add(credential);
            }
        }

        Logger.exiting(TAG, methodName, matchingCredentials);

        return matchingCredentials;
    }

    @Override
    public boolean removeAccount(final Account accountToRemove) {
        final String methodName = "removeAccount";
        Logger.entering(TAG, methodName, accountToRemove);

        if (null == accountToRemove) {
            throw new IllegalArgumentException("Param [accountToRemove] cannot be null.");
        }

        final Map<String, Account> accounts = getAccountsWithKeys();

        boolean accountRemoved = false;
        for (final Map.Entry<String, Account> entry : accounts.entrySet()) {
            final IAccount currentAccount = entry.getValue();

            if (currentAccount.equals(accountToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                accountRemoved = true;
                break;
            }
        }

        Logger.exiting(TAG, methodName, accountRemoved);

        return accountRemoved;
    }

    @Override
    public boolean removeCredential(final Credential credentialToRemove) {
        final String methodName = "removeCredential";
        Logger.entering(TAG, methodName, credentialToRemove);

        if (null == credentialToRemove) {
            throw new IllegalArgumentException("Param [credentialToRemove] cannot be null.");
        }

        final Map<String, Credential> credentials = getCredentialsWithKeys();

        boolean credentialRemoved = false;
        for (final Map.Entry<String, Credential> entry : credentials.entrySet()) {
            final Credential currentCredential = entry.getValue();

            if (currentCredential.equals(credentialToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                credentialRemoved = true;
                break;
            }
        }

        Logger.exiting(TAG, methodName, credentialRemoved);

        return credentialRemoved;
    }

    @Override
    public void clearAll() {
        final String methodName = "clearAll";
        Logger.entering(TAG, methodName);
        mSharedPreferencesFileManager.clear();
        Logger.exiting(TAG, methodName);
    }

    private Class<? extends Credential> credentialClassForType(final String cacheKey) {
        final String methodName = "credentialClassForType";
        Logger.entering(TAG, methodName, cacheKey);

        final CredentialType targetType = getCredentialTypeForCredentialCacheKey(cacheKey);
        Class<? extends Credential> credentialClass = null;

        switch (targetType) {
            case AccessToken:
                credentialClass = AccessToken.class;
                break;
            case RefreshToken:
                credentialClass = RefreshToken.class;
                break;
            case IdToken:
                credentialClass = IdToken.class;
                break;
            default:
                // TODO Log a warning? Throw an Exception?
        }

        Logger.exiting(TAG, methodName, credentialClass);

        return credentialClass;
    }

    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    private CredentialType getCredentialTypeForCredentialCacheKey(final String cacheKey) {
        final String methodName = "getCredentialTypeForCredentialCacheKey";
        Logger.entering(TAG, methodName, cacheKey);

        if (StringExtensions.isNullOrBlank(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CACHE_VALUE_SEPARATOR + credentialTypeStr + CACHE_VALUE_SEPARATOR)) {
                // it's a Credential
                // now choose whether to serialize an AT or RT...
                if (credentialTypeStr.equalsIgnoreCase(CredentialType.AccessToken.name())) {
                    type = CredentialType.AccessToken;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                    type = CredentialType.RefreshToken;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.IdToken.name())) {
                    type = CredentialType.IdToken;
                } else {
                    // TODO Log a warning and skip this value?
                    Logger.warn(TAG, "Unexpected credential type.");
                }
            }
        }

        Logger.exiting(TAG, methodName, type);

        return type;
    }

    private boolean isAccount(final String cacheKey) {
        final String methodName = "isAccount";
        Logger.entering(TAG, methodName, cacheKey);

        boolean isAccount = null == getCredentialTypeForCredentialCacheKey(cacheKey);

        Logger.exiting(TAG, methodName, isAccount);

        return isAccount;
    }

    private boolean isCredential(String cacheKey) {
        final String methodName = "isCredential";
        Logger.entering(TAG, methodName, cacheKey);

        boolean isCredential = null != getCredentialTypeForCredentialCacheKey(cacheKey);

        Logger.exiting(TAG, methodName, isCredential);

        return isCredential;
    }

}
