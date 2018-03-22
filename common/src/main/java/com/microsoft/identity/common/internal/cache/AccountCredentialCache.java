package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import java.util.ArrayList;
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

    @Override
    public synchronized List<Account> getAccounts() {
        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final List<Account> accounts = new ArrayList<>();

        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccount(cacheKey)) {
                final Account account = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        Account.class
                );
                accounts.add(account);
            }
        }

        return accounts;
    }

    private boolean isAccount(final String cacheKey) {
        return null == getCredentialTypeForCredentialCacheKey(cacheKey);
    }

    private boolean isCredential(String cacheKey) {
        return null != getCredentialTypeForCredentialCacheKey(cacheKey);
    }

    @Override
    public synchronized List<Credential> getCredentials() {
        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final List<Credential> credentials = new ArrayList<>();

        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isCredential(cacheKey)) {
                final Credential credential = mCacheValueDelegate.fromCacheValue(
                        cacheValue.getValue().toString(),
                        credentialClassForType(cacheKey)
                );
                credentials.add(credential);
            }
        }

        return credentials;
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

    interface Evaluator<T> {
        boolean evaluate(final T t);
    }

    private void clearCacheValuesOfType(final Evaluator<String> evaluator) {
        final Map<String, ?> cacheEntries = mSharedPreferencesFileManager.getAll();

        for (Map.Entry<String, ?> cacheEntry : cacheEntries.entrySet()) {
            final String cacheKey = cacheEntry.getKey();
            if (evaluator.evaluate(cacheKey)) {
                mSharedPreferencesFileManager.remove(cacheKey);
            }
        }
    }

    @Override
    public synchronized void clearAccounts() {
        clearCacheValuesOfType(new Evaluator<String>() {
            @Override
            public boolean evaluate(final String cacheKey) {
                return isAccount(cacheKey);
            }
        });
    }

    @Override
    public synchronized void clearCredentials() {
        clearCacheValuesOfType(new Evaluator<String>() {
            @Override
            public boolean evaluate(String cacheKey) {
                return isCredential(cacheKey);
            }
        });
    }

    @Override
    public synchronized void clearAll() {
        mSharedPreferencesFileManager.clear();
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
}
