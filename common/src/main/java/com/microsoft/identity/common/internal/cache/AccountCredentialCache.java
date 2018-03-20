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
    public void saveAccount(final Account account) {
        final String cacheKey = mCacheValueDelegate.generateCacheKey(account);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(account);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public void saveCredential(Credential credential) {
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credential);
        final String cacheValue = mCacheValueDelegate.generateCacheValue(credential);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public Account getAccount(final String cacheKey) {
        return mCacheValueDelegate.fromCacheValue(mSharedPreferencesFileManager.getString(cacheKey), Account.class);
    }

    @Override
    public Credential getCredential(final String cacheKey) {
        return mCacheValueDelegate.fromCacheValue(mSharedPreferencesFileManager.getString(cacheKey), Credential.class);
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
