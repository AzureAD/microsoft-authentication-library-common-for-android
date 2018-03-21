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
        // TODO add support for more Credential types...
        return mCacheValueDelegate.fromCacheValue(
                mSharedPreferencesFileManager.getString(cacheKey),
                getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken
                        ? AccessToken.class : RefreshToken.class
        );
    }

    @Override
    public List<Account> getAccounts() {
        // TODO
        return null;
    }

    @Override
    public List<Credential> getCredentials() {
        final Map<String, ?> cacheValues = mSharedPreferencesFileManager.getAll();
        final Set<String> credentialTypesLowerCase = new HashSet<>();
        final List<Credential> credentials = new ArrayList<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }


        // TODO clean this code up for better reuse
        for (final Map.Entry<String, ?> entry : cacheValues.entrySet()) {
            final String key = entry.getKey();
            for (final String credentialTypeStr : credentialTypesLowerCase) {
                if (key.contains(CACHE_VALUE_SEPARATOR + credentialTypeStr + CACHE_VALUE_SEPARATOR)) {
                    // it's a Credential
                    // now chooese whether to serialize an AT or RT...
                    if (credentialTypeStr.equalsIgnoreCase(CredentialType.AccessToken.name())) {
                        final AccessToken accessToken = mCacheValueDelegate.fromCacheValue(entry.getValue().toString(), AccessToken.class);
                        credentials.add(accessToken);
                    } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                        final RefreshToken refreshToken = mCacheValueDelegate.fromCacheValue(entry.getValue().toString(), RefreshToken.class);
                        credentials.add(refreshToken);
                    } else {
                        // TODO Log a warning and skip this value?
                    }
                } else {
                    // It's an Account
                }
            }
        }

        return credentials;
    }

    @Override
    public void clearAccounts() {
        // TODO
    }

    @Override
    public void clearCredentials() {
        // TODO
    }

    @Override
    public void clearAll() {
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
                // now chooese whether to serialize an AT or RT...
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
