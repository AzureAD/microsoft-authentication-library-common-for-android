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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.AccountCredentialCacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

public class AccountCredentialCache implements IAccountCredentialCache {

    // The names of the SharedPreferences file on disk.
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    private static final Account EMPTY_ACCOUNT = new Account();
    private static final AccessToken EMPTY_AT = new AccessToken();
    private static final RefreshToken EMPTY_RT = new RefreshToken();
    private static final IdToken EMPTY_ID = new IdToken();

    // SharedPreferences used to store Accounts and Credentials
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    private final Context mContext;
    private final IAccountCredentialCacheKeyValueDelegate mCacheValueDelegate;

    public AccountCredentialCache(
            final Context context,
            final IAccountCredentialCacheKeyValueDelegate accountCacheValueDelegate) {
        mContext = context;
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(
                mContext,
                sAccountCredentialSharedPreferences,
                new StorageHelper(mContext)
        );
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    public AccountCredentialCache(
            final Context context,
            final IAccountCredentialCacheKeyValueDelegate accountCacheValueDelegate,
            final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        mContext = context;
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
        mCacheValueDelegate = accountCacheValueDelegate;
    }

    @Override
    public synchronized void saveAccount(final Account account) {
        final String cacheKey = mCacheValueDelegate.generateCacheKey(account);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(account);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public synchronized void saveCredential(Credential credential) {
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credential);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(credential);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public synchronized Account getAccount(final String cacheKey) {
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

        return account;
    }

    @Override
    public synchronized Credential getCredential(final String cacheKey) {
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

        return credential;
    }

    private Map<String, Account> getAccountsWithKeys() {
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

        return accounts;
    }

    @Override
    public synchronized List<Account> getAccounts() {
        final Map<String, Account> allAccounts = getAccountsWithKeys();
        return new ArrayList<>(allAccounts.values());
    }

    @Override
    public List<Account> getAccounts(
            final @Nullable String uniqueId,
            final String environment,
            final @Nullable String realm) {
        if (StringExtensions.isNullOrBlank(environment)) {
            throw new IllegalArgumentException("Param [environment] cannot be null.");
        }

        final boolean mustMatchOnUniqueId = !StringExtensions.isNullOrBlank(uniqueId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final List<Account> allAccounts = getAccounts();
        final List<Account> matchingAccounts = new ArrayList<>();

        for (final Account account : allAccounts) {
            boolean matches = true;

            if (mustMatchOnUniqueId) {
                matches = uniqueId.equalsIgnoreCase(account.getUniqueUserId());
            }

            matches = matches && environment.equalsIgnoreCase(account.getEnvironment());

            if (mustMatchOnRealm) {
                matches = matches && realm.equalsIgnoreCase(account.getRealm());
            }

            if (matches) {
                matchingAccounts.add(account);
            }
        }

        return matchingAccounts;
    }

    private Map<String, Credential> getCredentialsWithKeys() {
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

        return credentials;
    }

    @Override
    public synchronized List<Credential> getCredentials() {
        final Map<String, Credential> allCredentials = getCredentialsWithKeys();
        return new ArrayList<>(allCredentials.values());
    }

    @Override
    public List<Credential> getCredentials(
            final @Nullable String uniqueId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            final @Nullable String realm,
            final @Nullable String target) {
        if (StringExtensions.isNullOrBlank(environment)) {
            throw new IllegalArgumentException("Param [environment] cannot be null.");
        }

        if (null == credentialType) {
            throw new IllegalArgumentException("Param [credentialType] cannot be null.");
        }

        if (StringExtensions.isNullOrBlank(clientId)) {
            throw new IllegalArgumentException("Param [clientId] cannot be null.");
        }

        final boolean mustMatchOnUniqueId = !StringExtensions.isNullOrBlank(uniqueId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final boolean mustMatchOnTarget = !StringExtensions.isNullOrBlank(target);
        final List<Credential> allCredentials = getCredentials();
        final List<Credential> matchingCredentials = new ArrayList<>();

        for (final Credential credential : allCredentials) {
            boolean matches = true;

            if (mustMatchOnUniqueId) {
                matches = uniqueId.equalsIgnoreCase(credential.getUniqueUserId());
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

        return matchingCredentials;
    }

    @Override
    public boolean removeAccount(final Account accountToRemove) {
        if (null == accountToRemove) {
            throw new IllegalArgumentException("Param [accountToRemove] cannot be null.");
        }

        final Map<String, Account> accounts = getAccountsWithKeys();

        for (final Map.Entry<String, Account> entry : accounts.entrySet()) {
            final IAccount currentAccount = entry.getValue();

            if (currentAccount.equals(accountToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean removeCredential(final Credential credentialToRemove) {
        if (null == credentialToRemove) {
            throw new IllegalArgumentException("Param [credentialToRemove] cannot be null.");
        }

        final Map<String, Credential> credentials = getCredentialsWithKeys();

        for (final Map.Entry<String, Credential> entry : credentials.entrySet()) {
            final Credential currentCredential = entry.getValue();

            if (currentCredential.equals(credentialToRemove)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    public void clearAll() {
        mSharedPreferencesFileManager.clear();
    }

    private Class<? extends Credential> credentialClassForType(final String cacheKey) {
        final CredentialType targetType = getCredentialTypeForCredentialCacheKey(cacheKey);
        Class<? extends Credential> credentialClass = null;

        switch (targetType) {
            case AccessToken:
                credentialClass = AccessToken.class;
                break;
            case RefreshToken:
                credentialClass = RefreshToken.class;
                break;
            default:
                // TODO Log a warning? Throw an Exception?
        }

        return credentialClass;
    }

    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    private CredentialType getCredentialTypeForCredentialCacheKey(final String cacheKey) {
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
                }
            }
        }

        return type;
    }

    private boolean isAccount(final String cacheKey) {
        return null == getCredentialTypeForCredentialCacheKey(cacheKey);
    }

    private boolean isCredential(String cacheKey) {
        return null != getCredentialTypeForCredentialCacheKey(cacheKey);
    }
}
