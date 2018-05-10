package com.microsoft.identity.common;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class AccountCredentialCacheTest extends AndroidSecretKeyEnabledHelper {

    static final String UNIQUE_USER_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String ENVIRONMENT = "login.microsoftonline.com";
    static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String TARGET = "user.read user.write https://graph.windows.net";
    // In the case of AAD, the realm is the tenantId
    static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    static final String AUTHORITY_ACCOUNT_ID = "00000000-0000-0000-088f-0e042cc22ac0";
    static final String USERNAME = "user.foo@tenant.onmicrosoft.com";
    static final String AUTHORITY_TYPE = "MSSTS";
    static final String CACHED_AT = "0";
    static final String EXPIRES_ON = "0";
    static final String SECRET = "3642fe2f-2c46-4824-9f27-e44b0e3e1278";

    private static final String REALM2 = "20d3e9fa-982a-40bc-bea4-26bbe3fd332e";
    private static final String REALM3 = "fc5171ec-2889-4ba6-bd1f-216fe87a8613";

    // The names of the SharedPreferences file on disk - must match AccountCredentialCache declaration to test impl
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    private AccountCredentialCache mAccountCredentialCache;
    private CacheKeyValueDelegate mDelegate;
    private SharedPreferencesFileManager mSharedPreferencesFileManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Context testContext = InstrumentationRegistry.getTargetContext();
        mAccountCredentialCache = new AccountCredentialCache(
                testContext,
                new CacheKeyValueDelegate()
        );
        mDelegate = new CacheKeyValueDelegate();
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(
                testContext,
                sAccountCredentialSharedPreferences,
                new StorageHelper(testContext) // Use encrypted storage for tests...
        );
    }

    @After
    public void tearDown() {
        // Wipe the SharedPreferences between tests...
        mAccountCredentialCache.clearAll();
    }

    @Test
    public void saveAccount() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final Account restoredAccount = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoRealm() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final Account restoredAccount = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoUniqueIdNoRealm() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setEnvironment(ENVIRONMENT);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final Account restoredAccount = mAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveIdToken() {
        final IdToken idToken = new IdToken();
        idToken.setUniqueUserId(UNIQUE_USER_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setRealm(REALM);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);
        idToken.setSecret(SECRET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(idToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(idToken);

        // Resurrect the Credential
        final Credential restoredIdToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertEquals(idToken.getUniqueUserId(), restoredIdToken.getUniqueUserId());
        assertEquals(idToken.getEnvironment(), restoredIdToken.getEnvironment());
        assertEquals(idToken.getCredentialType(), restoredIdToken.getCredentialType());
        assertEquals(idToken.getClientId(), restoredIdToken.getClientId());
        assertTrue(idToken.equals(restoredIdToken));
    }

    @Test
    public void saveCredential() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
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
    public void saveCredentialNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoRealmNoTarget() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
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
    public void saveCredentialNoUniqueIdNoRealmNoTarget() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoUniqueId() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
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
    public void saveCredentialNoUniqueIdNoRealm() {
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
    public void saveCredentialNoUniqueIdNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertEquals(refreshToken, restoredRefreshToken);
    }

    @Test
    public void saveCredentialNoRealm() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credential
        mAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void getAccounts() {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Verify getAccounts() returns one matching element
        final List<Account> accounts = mAccountCredentialCache.getAccounts();
        assertTrue(accounts.size() == 1);
        assertEquals(account, accounts.get(0));
    }

    @Test
    public void getAccountsNullEnvironment() {
        try {
            mAccountCredentialCache.getAccounts(UNIQUE_USER_ID, null, REALM);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getAccountsComplete() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<Account> accounts = mAccountCredentialCache.getAccounts(UNIQUE_USER_ID, ENVIRONMENT, REALM);
        assertEquals(1, accounts.size());
        final Account retrievedAccount = accounts.get(0);
        assertEquals(UNIQUE_USER_ID, retrievedAccount.getUniqueUserId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoUniqueId() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<Account> accounts = mAccountCredentialCache.getAccounts(null, ENVIRONMENT, REALM);
        assertEquals(1, accounts.size());
        final Account retrievedAccount = accounts.get(0);
        assertEquals(UNIQUE_USER_ID, retrievedAccount.getUniqueUserId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoUniqueIdNoRealm() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<Account> accounts = mAccountCredentialCache.getAccounts(null, ENVIRONMENT, null);
        assertEquals(1, accounts.size());
        final Account retrievedAccount = accounts.get(0);
        assertEquals(UNIQUE_USER_ID, retrievedAccount.getUniqueUserId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoRealm() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<Account> accounts = mAccountCredentialCache.getAccounts(UNIQUE_USER_ID, ENVIRONMENT, null);
        assertEquals(1, accounts.size());
        final Account retrievedAccount = accounts.get(0);
        assertEquals(UNIQUE_USER_ID, retrievedAccount.getUniqueUserId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsWithMatchingUniqueIdEnvironment() {
        final com.microsoft.identity.common.internal.dto.Account account1
                = new com.microsoft.identity.common.internal.dto.Account();
        account1.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account1.setUsername(USERNAME);
        account1.setAuthorityType(AUTHORITY_TYPE);
        account1.setUniqueUserId(UNIQUE_USER_ID);
        account1.setEnvironment(ENVIRONMENT);
        account1.setRealm(REALM);

        final com.microsoft.identity.common.internal.dto.Account account2
                = new com.microsoft.identity.common.internal.dto.Account();
        account2.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account2.setUsername(USERNAME);
        account2.setAuthorityType(AUTHORITY_TYPE);
        account2.setUniqueUserId(UNIQUE_USER_ID);
        account2.setEnvironment(ENVIRONMENT);
        account2.setRealm(REALM2);

        final com.microsoft.identity.common.internal.dto.Account account3
                = new com.microsoft.identity.common.internal.dto.Account();
        account3.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account3.setUsername(USERNAME);
        account3.setAuthorityType(AUTHORITY_TYPE);
        account3.setUniqueUserId(UNIQUE_USER_ID);
        account3.setEnvironment(ENVIRONMENT);
        account3.setRealm(REALM3);

        final com.microsoft.identity.common.internal.dto.Account account4
                = new com.microsoft.identity.common.internal.dto.Account();
        account4.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account4.setUsername(USERNAME);
        account4.setAuthorityType(AUTHORITY_TYPE);
        account4.setUniqueUserId(UNIQUE_USER_ID);
        account4.setEnvironment("Foo");
        account4.setRealm(REALM);

        // Save the Accounts
        mAccountCredentialCache.saveAccount(account1);
        mAccountCredentialCache.saveAccount(account2);
        mAccountCredentialCache.saveAccount(account3);
        mAccountCredentialCache.saveAccount(account4);

        final List<Account> accounts = mAccountCredentialCache.getAccounts(UNIQUE_USER_ID, ENVIRONMENT, null);
        assertEquals(3, accounts.size());
    }

    @Test
    public void getAccountsWithMatchingEnvironmentRealm() {
        final com.microsoft.identity.common.internal.dto.Account account1
                = new com.microsoft.identity.common.internal.dto.Account();
        account1.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account1.setUsername(USERNAME);
        account1.setAuthorityType(AUTHORITY_TYPE);
        account1.setUniqueUserId("Foo");
        account1.setEnvironment(ENVIRONMENT);
        account1.setRealm(REALM);

        final com.microsoft.identity.common.internal.dto.Account account2
                = new com.microsoft.identity.common.internal.dto.Account();
        account2.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account2.setUsername(USERNAME);
        account2.setAuthorityType(AUTHORITY_TYPE);
        account2.setUniqueUserId("Bar");
        account2.setEnvironment(ENVIRONMENT);
        account2.setRealm(REALM);

        final com.microsoft.identity.common.internal.dto.Account account3
                = new com.microsoft.identity.common.internal.dto.Account();
        account3.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account3.setUsername(USERNAME);
        account3.setAuthorityType(AUTHORITY_TYPE);
        account3.setUniqueUserId("Baz");
        account3.setEnvironment(ENVIRONMENT);
        account3.setRealm(REALM);

        final com.microsoft.identity.common.internal.dto.Account account4
                = new com.microsoft.identity.common.internal.dto.Account();
        account4.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account4.setUsername(USERNAME);
        account4.setAuthorityType(AUTHORITY_TYPE);
        account4.setUniqueUserId("qux");
        account4.setEnvironment(ENVIRONMENT);
        account4.setRealm("quz");

        // Save the Accounts
        mAccountCredentialCache.saveAccount(account1);
        mAccountCredentialCache.saveAccount(account2);
        mAccountCredentialCache.saveAccount(account3);
        mAccountCredentialCache.saveAccount(account4);

        final List<Account> accounts = mAccountCredentialCache.getAccounts(null, ENVIRONMENT, REALM);
        assertEquals(3, accounts.size());
    }

    @Test
    public void getCredentials() {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Verify getCredentials() returns two matching elements
        final List<Credential> credentials = mAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
    }

    @Test
    public void getCredentialsNoEnvironment() {
        try {
            mAccountCredentialCache.getCredentials(
                    UNIQUE_USER_ID,
                    null,
                    CredentialType.RefreshToken,
                    CLIENT_ID,
                    REALM,
                    TARGET
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getCredentialsNoCredentialType() {
        try {
            mAccountCredentialCache.getCredentials(
                    UNIQUE_USER_ID,
                    ENVIRONMENT,
                    null,
                    CLIENT_ID,
                    REALM,
                    TARGET
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getCredentialsNoClientId() {
        try {
            mAccountCredentialCache.getCredentials(
                    UNIQUE_USER_ID,
                    ENVIRONMENT,
                    CredentialType.RefreshToken,
                    null,
                    REALM,
                    TARGET
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getCredentialsComplete() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                UNIQUE_USER_ID,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                REALM,
                TARGET
        );
        assertEquals(1, credentials.size());
        final Credential retrievedCredential = credentials.get(0);
        assertEquals(
                CredentialType.RefreshToken.name(),
                retrievedCredential.getCredentialType()
        );
    }

    @Test
    public void getCredentialsNoUniqueId() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        refreshToken.setUniqueUserId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                null,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                REALM,
                TARGET
        );
        assertEquals(1, credentials.size());
    }

    @Test
    public void getCredentialsNoUniqueIdNoRealm() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSecret(SECRET);
        refreshToken.setUniqueUserId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setUniqueUserId("Baz");
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                null,
                TARGET
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoUniqueIdNoRealmNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSecret(SECRET);
        refreshToken.setUniqueUserId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setUniqueUserId("Baz");
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget("qux");

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                null,
                null
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSecret(SECRET);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setUniqueUserId(UNIQUE_USER_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget("qux");

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                UNIQUE_USER_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                REALM,
                null
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoRealm() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSecret(SECRET);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setUniqueUserId(UNIQUE_USER_ID);
        accessToken2.setRealm("Bar");
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                UNIQUE_USER_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                null,
                TARGET
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoRealmNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setUniqueUserId(UNIQUE_USER_ID);
        accessToken2.setRealm("Bar");
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget("qux");
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                UNIQUE_USER_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                null,
                null
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoUniqueIdNoTarget() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessToken accessToken = new AccessToken();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setUniqueUserId("Quz");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final AccessToken accessToken2 = new AccessToken();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setUniqueUserId(UNIQUE_USER_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mAccountCredentialCache.saveCredential(refreshToken);
        mAccountCredentialCache.saveCredential(accessToken);
        mAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mAccountCredentialCache.getCredentials(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                REALM,
                null
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void clearAccounts() {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAccounts()
        mAccountCredentialCache.removeAccount(account);

        // Verify getAccounts() returns zero items
        assertTrue(mAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns two items
        final List<Credential> credentials = mAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
    }

    @Test
    public void clearCredentials() {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setSecret(SECRET);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearCredentials()
        mAccountCredentialCache.removeCredential(accessToken);
        mAccountCredentialCache.removeCredential(refreshToken);

        // Verify getAccounts() returns 1 item
        assertEquals(1, mAccountCredentialCache.getAccounts().size());

        // Verify getCredentials() returns zero items
        assertEquals(0, mAccountCredentialCache.getCredentials().size());
    }

    @Test
    public void clearAll() {
        // Save an Account into the cache
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessToken accessToken = new AccessToken();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAll()
        mAccountCredentialCache.clearAll();

        // Verify getAccounts() returns zero items
        assertTrue(mAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns zero items
        assertTrue(mAccountCredentialCache.getCredentials().isEmpty());
    }

    @Test(expected = RuntimeException.class) // TODO Should this *really* throw a RuntimeException
    public void testThrowsExceptionForMalformedCredentialCacheKey() {
        mAccountCredentialCache.getCredential("Malformed cache key");
    }

    @Test
    public void noValueForCacheKeyAccount() {
        assertEquals(0, mAccountCredentialCache.getAccounts().size());
        final Account account = (Account) mAccountCredentialCache.getAccount("No account");
        assertNull(account);
    }

    @Test
    public void noValueForCacheKeyAccessToken() {
        assertEquals(0, mAccountCredentialCache.getCredentials().size());
        final AccessToken accessToken = (AccessToken) mAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.AccessToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(accessToken);
    }

    @Test
    public void noValueForCacheKeyRefreshToken() {
        assertEquals(0, mAccountCredentialCache.getCredentials().size());
        final RefreshToken refreshToken = (RefreshToken) mAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.RefreshToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(refreshToken);
    }

    @Test
    public void noValueForCacheKeyIdToken() {
        assertEquals(0, mAccountCredentialCache.getCredentials().size());
        final IdToken idToken = (IdToken) mAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.IdToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(idToken);
    }

    @Test
    public void malformedJsonCacheValueForAccount() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(account);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" \"not an account\"}");

        final Account malformedAccount = mAccountCredentialCache.getAccount(cacheKey);
        assertNull(malformedAccount);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedCacheValueForAccount() {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueUserId(UNIQUE_USER_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(account);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" : \"not an account\"}");

        final Account malformedAccount = mAccountCredentialCache.getAccount(cacheKey);
        assertNull(malformedAccount);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForAccessToken() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(accessToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" \"not an accessToken\"}");

        final AccessToken malformedAccessToken = (AccessToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedAccessToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedCacheValueForAccessToken() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueUserId(UNIQUE_USER_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(accessToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" : \"not an accessToken\"}");

        final AccessToken malformedAccessToken = (AccessToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedAccessToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForRefreshToken() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.AccessToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(refreshToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" \"not a refreshToken\"}");

        final RefreshToken malformedRefreshToken = (RefreshToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedRefreshToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedCacheValueForRefreshToken() {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueUserId(UNIQUE_USER_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.AccessToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(refreshToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" : \"not a refreshToken\"}");

        final RefreshToken malformedRefreshToken = (RefreshToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedRefreshToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForIdToken() {
        final IdToken idToken = new IdToken();
        idToken.setUniqueUserId(UNIQUE_USER_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(idToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\"  \"not an idToken\"}");

        final IdToken restoredIdToken = (IdToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(restoredIdToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    @Test
    public void malformedCacheValueForIdToken() {
        final IdToken idToken = new IdToken();
        idToken.setUniqueUserId(UNIQUE_USER_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(idToken);

        mSharedPreferencesFileManager.putString(cacheKey, "{\"thing\" : \"not an idToken\"}");

        final IdToken restoredIdToken = (IdToken) mAccountCredentialCache.getCredential(cacheKey);
        assertNull(restoredIdToken);
        assertNull(mSharedPreferencesFileManager.getString(cacheKey));
    }

    public void persistAndRestoreExtraClaimsAccessToken() {
        // TODO
    }

    public void persistAndRestoreExtraClaimsRefreshToken() {
        // TODO
    }

    public void persistAndRestoreExtraClaimsIdToken() {
        // TODO
    }

}
