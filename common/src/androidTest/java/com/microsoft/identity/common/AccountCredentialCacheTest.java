package com.microsoft.identity.common;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.AccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AccountCredentialCacheTest {

    private static final String UNIQUE_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String ENVIRONMENT = "login.microsoftonline.com";
    private static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String TARGET = "user.read user.write https://graph.windows.net";
    private static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    private static final String CREDENTIAL_TYPE_ACCESS_TOKEN = CredentialType.AccessToken.name().toLowerCase(Locale.US);
    private static final String CREDENTIAL_TYPE_REFRESH_TOKEN = CredentialType.RefreshToken.name().toLowerCase(Locale.US);

    private IAccountCredentialCache mAccountCredentialCache;
    private IAccountCredentialCacheKeyValueDelegate mDelegate;


    @Before
    public void setUp() {
        final Context testContext = InstrumentationRegistry.getTargetContext();
        mAccountCredentialCache = new AccountCredentialCache(
                testContext,
                new AccountCredentialCacheKeyValueDelegate()
        );
        mDelegate = new AccountCredentialCacheKeyValueDelegate();
    }

    public void tearDown() {
        // Wipe the SharedPreferences between tests...
        mAccountCredentialCache.clearAll();
    }

    @Test
    public void saveAccount() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final com.microsoft.identity.common.internal.dto.Account restoredAccount
                = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoUniqueId() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final com.microsoft.identity.common.internal.dto.Account restoredAccount
                = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoRealm() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final com.microsoft.identity.common.internal.dto.Account restoredAccount
                = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoUniqueIdNoRealm() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setEnvironment(ENVIRONMENT);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final com.microsoft.identity.common.internal.dto.Account restoredAccount
                = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveCredential() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoUniqueIdNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoUniqueId() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoUniqueIdNoRealm() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoUniqueIdNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoRealm() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void getAccounts() throws Exception {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Verify getAccounts() returns one matching element
        final List<Account> accounts = mAccountCredentialCache.getAccounts();
        assertTrue(accounts.size() == 1);
        assertEquals(account, accounts.get(0));
    }

    @Test
    public void getCredentials() throws Exception {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Verify getCredentials() returns two matching elements
        final List<Credential> credentials = mAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
        // TODO compare the items
    }

    @Test
    public void clearAccounts() throws Exception {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAccounts()
        mAccountCredentialCache.clearAccounts();

        // Verify getAccounts() returns zero items
        assertTrue(mAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns two items
        final List<Credential> credentials = mAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
        // TODO compare contents
    }

    @Test
    public void clearCredentials() throws Exception {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearCredentials()
        mAccountCredentialCache.clearCredentials();

        // Verify getAccounts() returns 1 item
        assertTrue(mAccountCredentialCache.getAccounts().size() == 1);

        // Verify getCredentials() returns zero items
        assertTrue(mAccountCredentialCache.getCredentials().isEmpty());
    }

    @Test
    public void clearAll() throws Exception {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAll()
        mAccountCredentialCache.clearAll();

        // Verify getAccounts() returns zero items
        assertTrue(mAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns zero items
        assertTrue(mAccountCredentialCache.getCredentials().isEmpty());
    }

}
