package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;

import java.util.List;

public class AccountCredentialCache implements IAccountCredentialCache {

    // The names of the SharedPreferences file on disk.
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    // SharedPreferences used to store Accounts and Credentials
    private final SharedPreferencesFileManager mAccountCredentialSharedPreferences;

    private final Context mContext;
    private final ICacheValueDelegate<Account> mAccountCacheValueDelegate;
    private final ICacheValueDelegate<Credential> mCredentialCacheValueDelegate;

    public AccountCredentialCache(
            final Context context,
            final ICacheValueDelegate<Account> accountCacheValueGenerator,
            final ICacheValueDelegate<Credential> credentialCacheValueGenerator) {
        mContext = context;
        mAccountCredentialSharedPreferences =
                new SharedPreferencesFileManager(mContext, sAccountCredentialSharedPreferences);
        mAccountCacheValueDelegate = accountCacheValueGenerator;
        mCredentialCacheValueDelegate = credentialCacheValueGenerator;
    }

    @Override
    public void saveAccount(final Account account) {
        final String cacheKey = mAccountCacheValueDelegate.generateCacheKey(account);
        final String cacheValue = mAccountCacheValueDelegate.generateCacheValue(account);
        mAccountCredentialSharedPreferences.putString(cacheKey, cacheValue);
    }

    @Override
    public void saveCredential(Credential credential) {
        final String cacheKey = mCredentialCacheValueDelegate.generateCacheKey(credential);
        final String cacheValue = mCredentialCacheValueDelegate.generateCacheValue(credential);
        mAccountCredentialSharedPreferences.putString(cacheKey, cacheValue);
    }

    @Override
    public Account getAccount(final String cacheKey) {
        return mAccountCacheValueDelegate.fromCacheValue(
                mAccountCredentialSharedPreferences.getString(cacheKey)
        );
    }

    @Override
    public Credential getCredential(final String cacheKey) {
        return mCredentialCacheValueDelegate.fromCacheValue(
                mAccountCredentialSharedPreferences.getString(cacheKey)
        );
    }

    @Override
    public List<Account> getAccounts() {
        // TODO
        return null;
    }

    @Override
    public List<Credential> getCredentials() {
        // TODO
        return null;
    }
}
