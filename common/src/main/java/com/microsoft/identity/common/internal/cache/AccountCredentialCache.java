package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
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

    // SharedPreferences used to store Accounts and Credentials
    private final SharedPreferencesFileManager mSharedPreferencesFileManager;

    private final Context mContext;
    private final IAccountCredentialCacheKeyValueDelegate mCacheValueDelegate;

    public AccountCredentialCache(
            final Context context,
            final IAccountCredentialCacheKeyValueDelegate accountCacheValueDelegate) {
        mContext = context;
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(mContext, sAccountCredentialSharedPreferences);
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
        return mCacheValueDelegate.fromCacheValue(mSharedPreferencesFileManager.getString(cacheKey), Account.class);
    }

    @Override
    public synchronized Credential getCredential(final String cacheKey) {
        // TODO add support for more Credential types...
        return mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.getString(cacheKey),
                getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken
                        ? AccessToken.class : RefreshToken.class
        );
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
        final boolean mustMatchOnUniqueId = !StringExtensions.isNullOrBlank(uniqueId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final List<Account> allAccounts = getAccounts();
        final List<Account> matchingAccounts = new ArrayList<>();

        for (final Account account : allAccounts) {
            boolean matches = true;

            if (mustMatchOnUniqueId) {
                matches = uniqueId.equalsIgnoreCase(account.getUniqueId());
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
        final boolean mustMatchOnUniqueId = !StringExtensions.isNullOrBlank(uniqueId);
        final boolean mustMatchOnRealm = !StringExtensions.isNullOrBlank(realm);
        final boolean mustMatchOnTarget = !StringExtensions.isNullOrBlank(target);
        final List<Credential> allCredentials = getCredentials();
        final List<Credential> matchingCredentials = new ArrayList<>();

        for (final Credential credential : allCredentials) {
            boolean matches = true;

            if (mustMatchOnUniqueId) {
                matches = uniqueId.equalsIgnoreCase(credential.getUniqueId());
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
    public boolean removeAccount(final String uniqueId, final String environment) {
        final Map<String, Account> accounts = getAccountsWithKeys();

        for (final Map.Entry<String, Account> entry : accounts.entrySet()) {
            final Account currentAccount = entry.getValue();

            if (uniqueId.equalsIgnoreCase(currentAccount.getUniqueId())
                    && environment.equalsIgnoreCase(currentAccount.getEnvironment())) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean removeCredential(final Credential credentialToClear) {
        final Map<String, Credential> credentials = getCredentialsWithKeys();

        for (final Map.Entry<String, Credential> entry : credentials.entrySet()) {
            final Credential currentCredential = entry.getValue();

            if (currentCredential.equals(credentialToClear)) {
                mSharedPreferencesFileManager.remove(entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    public int removeAll(final String uniqueId, final String environment) {
        int entriesRemoved = removeAccount(uniqueId, environment) ? 1 : 0;

        final List<Credential> credentialsToRemove = getCredentials(
                uniqueId,
                environment,
                CredentialType.AccessToken,
                null, // clientId
                null, // realm
                null // target
        );

        credentialsToRemove.addAll(
                getCredentials(
                        uniqueId,
                        environment,
                        CredentialType.RefreshToken,
                        null, // clientId
                        null, // realm
                        null // target
                )
        );

        final Map<String, Credential> allCredentialsAndKeys = getCredentialsWithKeys();

        for (final Map.Entry<String, Credential> entry : allCredentialsAndKeys.entrySet()) {
            final Credential credential = entry.getValue();

            if (credentialsToRemove.contains(credential)) {
                entriesRemoved++;
                mSharedPreferencesFileManager.remove(entry.getKey());
            }
        }

        return entriesRemoved;
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

    private CredentialType getCredentialTypeForCredentialCacheKey(final String cacheKey) {
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
