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
package com.microsoft.identity.common;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCacheWithMemoryCache;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.PrimaryRefreshTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.shadows.ShadowAndroidSdkStorageEncryptionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.java.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class SharedPreferencesAccountCredentialCacheWithMemoryCacheTest {

    static final BearerAuthenticationSchemeInternal BEARER_AUTHENTICATION_SCHEME = new BearerAuthenticationSchemeInternal();
    static final String HOME_ACCOUNT_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String ENVIRONMENT = "login.microsoftonline.com";
    static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String APPLICATION_IDENTIFIER_SHA1 = "some.package/AbCdEfGhIjKlMnOpQrStUvWxYz/=";
    static final String APPLICATION_IDENTIFIER_SHA512 = "some.package/AbCdEfGhIjKlMnOpQrStUvWxYz/+0123456789AbCdEfGhIjKlMnOpQrStUvWxYz/+0123456789AbCdEfGhIj==";
    static final String MAM_ENROLLMENT_IDENTIFIER = "UNSET";
    static final String TARGET = "user.read user.write https://graph.windows.net";
    // In the case of AAD, the realm is the tenantId
    static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    static final String MIDDLE_NAME = "Q";
    static final String NAME = "Jane Doe";
    static final String LOCAL_ACCOUNT_ID = "00000000-0000-0000-088f-0e042cc22ac0";
    static final String USERNAME = "user.foo@tenant.onmicrosoft.com";
    static final String AUTHORITY_TYPE = "MSSTS";
    static final String CACHED_AT = "0";
    static final String EXPIRES_ON = "0";
    static final String SECRET = "3642fe2f-2c46-4824-9f27-e44b0e3e1278";
    static final String REALM2 = "20d3e9fa-982a-40bc-bea4-26bbe3fd332e";
    static final String SESSION_KEY = "ZmVldnVvWDBvYmVldGg5aQo=";
    public static final String ESCAPE_SEQ_CHARS = "\r\f\n\t";

    private static final String ENVIRONMENT_LEGACY = "login.windows.net";
    private static final String REALM3 = "fc5171ec-2889-4ba6-bd1f-216fe87a8613";

    // The names of the SharedPreferences file on disk - must match SharedPreferencesAccountCredentialCache declaration to test impl
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    private SharedPreferencesAccountCredentialCacheWithMemoryCache mSharedPreferencesAccountCredentialCache;
    private CacheKeyValueDelegate mDelegate;
    private INameValueStorage<String> mSharedPreferencesFileManager;

    @Before
    public void setUp() throws Exception {
        final Context testContext = ApplicationProvider.getApplicationContext();
        mDelegate = new CacheKeyValueDelegate();
        mSharedPreferencesFileManager = AndroidPlatformComponentsFactory.createFromContext(testContext)
                .getStorageSupplier()
                .getEncryptedNameValueStore(
                    sAccountCredentialSharedPreferences,
                    String.class
        );
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );
    }

    @After
    public void tearDown() {
        // Wipe the SharedPreferences between tests...
        mSharedPreferencesAccountCredentialCache.clearAll();
    }

    private static String wrapInEscapeSequenceChars(final String inputString) {
        return ESCAPE_SEQ_CHARS + inputString + ESCAPE_SEQ_CHARS;
    }

    @Test
    public void saveAccount() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setMiddleName(MIDDLE_NAME);
        account.setName(NAME);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final AccountRecord restoredAccount = mSharedPreferencesAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final AccountRecord restoredAccount = mSharedPreferencesAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveAccountNoHomeAccountIdNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setEnvironment(ENVIRONMENT);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String accountCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Account
        final AccountRecord restoredAccount = mSharedPreferencesAccountCredentialCache.getAccount(accountCacheKey);
        assertTrue(account.equals(restoredAccount));
    }

    @Test
    public void saveIdToken() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setRealm(REALM);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);
        idToken.setSecret(SECRET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(idToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(idToken);

        // Resurrect the Credential
        final Credential restoredIdToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertEquals(idToken.getHomeAccountId(), restoredIdToken.getHomeAccountId());
        assertEquals(idToken.getEnvironment(), restoredIdToken.getEnvironment());
        assertEquals(idToken.getCredentialType(), restoredIdToken.getCredentialType());
        assertEquals(idToken.getClientId(), restoredIdToken.getClientId());
        assertTrue(idToken.equals(restoredIdToken));
    }

    @Test
    public void saveCredential() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialWithEscapeChars() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(wrapInEscapeSequenceChars(CredentialType.RefreshToken.name()));
        refreshToken.setEnvironment(wrapInEscapeSequenceChars(ENVIRONMENT));
        refreshToken.setHomeAccountId(wrapInEscapeSequenceChars(HOME_ACCOUNT_ID));
        refreshToken.setClientId(wrapInEscapeSequenceChars(CLIENT_ID));
        refreshToken.setSecret(wrapInEscapeSequenceChars(SECRET));
        refreshToken.setTarget(wrapInEscapeSequenceChars(TARGET));

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoRealmNoTarget() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoHomeAccountIdNoRealmNoTarget() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoHomeAccountId() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
    }

    @Test
    public void saveCredentialNoHomeAccountIdNoRealm() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void saveCredentialNoHomeAccountIdNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertEquals(refreshToken, restoredRefreshToken);
    }

    @Test
    public void saveCredentialNoRealm() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
    }

    @Test
    public void getAccounts() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Verify getAccountsFilteredBy() returns one matching element
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccounts();
        assertTrue(accounts.size() == 1);
        assertEquals(account, accounts.get(0));
    }

    @Test
    public void getAccountsNullEnvironment() {
        final AccountRecord account1 = new AccountRecord();
        account1.setHomeAccountId(HOME_ACCOUNT_ID);
        account1.setEnvironment(ENVIRONMENT);
        account1.setRealm(REALM);
        account1.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account1.setUsername(USERNAME);
        account1.setAuthorityType(AUTHORITY_TYPE);

        final AccountRecord account2 = new AccountRecord();
        account2.setHomeAccountId(HOME_ACCOUNT_ID);
        account2.setEnvironment(ENVIRONMENT_LEGACY);
        account2.setRealm(REALM);
        account2.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account2.setUsername(USERNAME);
        account2.setAuthorityType(AUTHORITY_TYPE);

        // Save the Accounts
        mSharedPreferencesAccountCredentialCache.saveAccount(account1);
        mSharedPreferencesAccountCredentialCache.saveAccount(account2);

        // Test retrieval
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(
                HOME_ACCOUNT_ID,
                null,
                REALM
        );
        assertEquals(2, accounts.size());
    }

    @Test
    public void getAccountsComplete() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(HOME_ACCOUNT_ID, ENVIRONMENT, REALM);
        assertEquals(1, accounts.size());
        final AccountRecord retrievedAccount = accounts.get(0);
        assertEquals(HOME_ACCOUNT_ID, retrievedAccount.getHomeAccountId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoHomeAccountId() {
        final AccountRecord account = new AccountRecord();
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(null, ENVIRONMENT, REALM);
        assertEquals(1, accounts.size());
        final AccountRecord retrievedAccount = accounts.get(0);
        assertEquals(HOME_ACCOUNT_ID, retrievedAccount.getHomeAccountId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoHomeAccountIdNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(null, ENVIRONMENT, null);
        assertEquals(1, accounts.size());
        final AccountRecord retrievedAccount = accounts.get(0);
        assertEquals(HOME_ACCOUNT_ID, retrievedAccount.getHomeAccountId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);

        // Save the Account
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Test retrieval
        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(HOME_ACCOUNT_ID, ENVIRONMENT, null);
        assertEquals(1, accounts.size());
        final AccountRecord retrievedAccount = accounts.get(0);
        assertEquals(HOME_ACCOUNT_ID, retrievedAccount.getHomeAccountId());
        assertEquals(ENVIRONMENT, retrievedAccount.getEnvironment());
        assertEquals(REALM, retrievedAccount.getRealm());
    }

    @Test
    public void getAccountsWithMatchingHomeAccountIdEnvironment() {
        final AccountRecord account1 = new AccountRecord();
        account1.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account1.setUsername(USERNAME);
        account1.setAuthorityType(AUTHORITY_TYPE);
        account1.setHomeAccountId(HOME_ACCOUNT_ID);
        account1.setEnvironment(ENVIRONMENT);
        account1.setRealm(REALM);

        final AccountRecord account2 = new AccountRecord();
        account2.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account2.setUsername(USERNAME);
        account2.setAuthorityType(AUTHORITY_TYPE);
        account2.setHomeAccountId(HOME_ACCOUNT_ID);
        account2.setEnvironment(ENVIRONMENT);
        account2.setRealm(REALM2);

        final AccountRecord account3 = new AccountRecord();
        account3.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account3.setUsername(USERNAME);
        account3.setAuthorityType(AUTHORITY_TYPE);
        account3.setHomeAccountId(HOME_ACCOUNT_ID);
        account3.setEnvironment(ENVIRONMENT);
        account3.setRealm(REALM3);

        final AccountRecord account4 = new AccountRecord();
        account4.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account4.setUsername(USERNAME);
        account4.setAuthorityType(AUTHORITY_TYPE);
        account4.setHomeAccountId(HOME_ACCOUNT_ID);
        account4.setEnvironment("Foo");
        account4.setRealm(REALM);

        // Save the Accounts
        mSharedPreferencesAccountCredentialCache.saveAccount(account1);
        mSharedPreferencesAccountCredentialCache.saveAccount(account2);
        mSharedPreferencesAccountCredentialCache.saveAccount(account3);
        mSharedPreferencesAccountCredentialCache.saveAccount(account4);

        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(HOME_ACCOUNT_ID, ENVIRONMENT, null);
        assertEquals(3, accounts.size());
    }

    @Test
    public void getAccountsWithMatchingEnvironmentRealm() {
        final AccountRecord account1 = new AccountRecord();
        account1.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account1.setUsername(USERNAME);
        account1.setAuthorityType(AUTHORITY_TYPE);
        account1.setHomeAccountId("Foo");
        account1.setEnvironment(ENVIRONMENT);
        account1.setRealm(REALM);

        final AccountRecord account2 = new AccountRecord();
        account2.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account2.setUsername(USERNAME);
        account2.setAuthorityType(AUTHORITY_TYPE);
        account2.setHomeAccountId("Bar");
        account2.setEnvironment(ENVIRONMENT);
        account2.setRealm(REALM);

        final AccountRecord account3 = new AccountRecord();
        account3.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account3.setUsername(USERNAME);
        account3.setAuthorityType(AUTHORITY_TYPE);
        account3.setHomeAccountId("Baz");
        account3.setEnvironment(ENVIRONMENT);
        account3.setRealm(REALM);

        final AccountRecord account4 = new AccountRecord();
        account4.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account4.setUsername(USERNAME);
        account4.setAuthorityType(AUTHORITY_TYPE);
        account4.setHomeAccountId("qux");
        account4.setEnvironment(ENVIRONMENT);
        account4.setRealm("quz");

        // Save the Accounts
        mSharedPreferencesAccountCredentialCache.saveAccount(account1);
        mSharedPreferencesAccountCredentialCache.saveAccount(account2);
        mSharedPreferencesAccountCredentialCache.saveAccount(account3);
        mSharedPreferencesAccountCredentialCache.saveAccount(account4);

        final List<AccountRecord> accounts = mSharedPreferencesAccountCredentialCache.getAccountsFilteredBy(null, ENVIRONMENT, REALM);
        assertEquals(3, accounts.size());
    }

    @Test
    public void getCredentials() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Verify getCredentials() returns two matching elements
        final List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
    }

    @Test
    public void getCredentialsNoEnvironment() {
        final RefreshTokenRecord refreshToken1 = new RefreshTokenRecord();
        refreshToken1.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken1.setEnvironment(ENVIRONMENT);
        refreshToken1.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken1.setClientId(CLIENT_ID);
        refreshToken1.setSecret(SECRET);
        refreshToken1.setTarget(TARGET);

        final RefreshTokenRecord refreshToken2 = new RefreshTokenRecord();
        refreshToken2.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken2.setEnvironment(ENVIRONMENT_LEGACY);
        refreshToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken2.setClientId(CLIENT_ID);
        refreshToken2.setSecret(SECRET);
        refreshToken2.setTarget(TARGET);

        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken1);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                null, // * wildcard
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoCredentialType() {
        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        final List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                null,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );

        assertEquals(
                2,
                credentials.size()
        );
    }

    @Test
    public void getCredentialsNoClientId() {
        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID + "2");
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        final List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                null,
                null,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );

        assertEquals(
                2,
                credentials.size()
        );
    }

    @Test
    public void getCredentialsComplete() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(1, credentials.size());
        final Credential retrievedCredential = credentials.get(0);
        assertEquals(
                CredentialType.RefreshToken.name(),
                retrievedCredential.getCredentialType()
        );
    }

    @Test
    public void getCredentialsCaseInsensitive() {
        // Uppercase the value we're filtering on to assert
        // that the match is case insensitive
        final String searchTarget = TARGET.toUpperCase();

        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                searchTarget,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(1, credentials.size());
        final Credential retrievedCredential = credentials.get(0);
        assertEquals(
                CredentialType.RefreshToken.name(),
                retrievedCredential.getCredentialType()
        );
    }

    @Test
    public void getCredentialsPartialMatch() {
        final String[] targetScopes = TARGET.split("\\s+");

        // Just in case this value changes on us, just assert that it take the expected format
        assertEquals(3, targetScopes.length);

        // Let's grab a subset of these in a different order and make sure we still get the right
        // results back
        final String searchTarget = targetScopes[2] + " " + targetScopes[0];

        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                searchTarget,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(1, credentials.size());
        final Credential retrievedCredential = credentials.get(0);
        assertEquals(
                CredentialType.RefreshToken.name(),
                retrievedCredential.getCredentialType()
        );
    }

    @Test
    public void getCredentialsPartialMatchWithCapitalization() {
        final String[] targetScopes = TARGET.split("\\s+");

        // Just in case this value changes on us, just assert that it take the expected format
        assertEquals(3, targetScopes.length);

        // Let's grab a subset of these in a different order and make sure we still get the right
        // results back
        final String searchTarget = targetScopes[2].toUpperCase()
                + " "
                + targetScopes[0].toUpperCase();

        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                searchTarget,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(1, credentials.size());
        final Credential retrievedCredential = credentials.get(0);
        assertEquals(
                CredentialType.RefreshToken.name(),
                retrievedCredential.getCredentialType()
        );
    }

    @Test
    public void getCredentialsNoHomeAccountId() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        refreshToken.setHomeAccountId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                null,
                ENVIRONMENT,
                CredentialType.RefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(1, credentials.size());
    }

    @Test
    public void getCredentialsNoHomeAccountIdNoRealm() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId("Baz");
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoHomeAccountIdNoRealmNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId("Foo");
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId("Bar");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId("Baz");
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget("qux");

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget("qux");

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsWhenRequestedClaimsAreNotSpecified() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);
        accessToken2.setRequestedClaims("{\"access_token\":{\"deviceid\":{\"essential\":true}}}");

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsWhenRequestedClaimsAreSpecified() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);
        accessToken2.setRequestedClaims("{\"access_token\":{\"deviceid\":{\"essential\":true}}}");

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName(),
                "{\"access_token\":{\"deviceid\":{\"essential\":true}}}",
                mSharedPreferencesAccountCredentialCache.getCredentials()
        );
        assertEquals(1, credentials.size());
    }

    @Test
    public void getCorrectCredentialWhenRequestedClaimsAreSpecified() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret("SecretA");
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setRequestedClaims("{\"access_token\":{\"deviceid\":{\"essential\":false}}}");

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret("SecretB");
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);
        accessToken2.setRequestedClaims("{\"access_token\":{\"deviceid\":{\"essential\":true}}}");

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName(),
                "{\"access_token\":{\"deviceid\":{\"essential\":true}}}",
                mSharedPreferencesAccountCredentialCache.getCredentials()
        );
        assertEquals(1, credentials.size());
        assertEquals("SecretB", credentials.get(0).getSecret());
    }

    @Test
    public void getCredentialsNoRealm() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setSecret(SECRET);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm("Bar");
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                TARGET,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoRealmNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm("Bar");
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget("qux");
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsNoHomeAccountIdNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accessToken.setHomeAccountId("Quz");
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);

        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm(REALM);
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);

        // Save the Credentials
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                null,
                ENVIRONMENT,
                CredentialType.AccessToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                REALM,
                null,
                BEARER_AUTHENTICATION_SCHEME.getName()
        );
        assertEquals(2, credentials.size());
    }

    @Test
    public void getCredentialsPRTNoClientId() {
        final PrimaryRefreshTokenRecord primaryRefreshToken = new PrimaryRefreshTokenRecord();
        primaryRefreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        primaryRefreshToken.setEnvironment(ENVIRONMENT);
        primaryRefreshToken.setCredentialType(CredentialType.PrimaryRefreshToken.name().toLowerCase(Locale.US));
        primaryRefreshToken.setClientId(CLIENT_ID);
        primaryRefreshToken.setSessionKey(SESSION_KEY);

        mSharedPreferencesAccountCredentialCache.saveCredential(primaryRefreshToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.PrimaryRefreshToken,
                null, /* client id */
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                null,
                null
        );
        assertEquals(1, credentials.size());
    }

    @Test
    public void getCredentialsPRTClientId() {
        final PrimaryRefreshTokenRecord primaryRefreshToken = new PrimaryRefreshTokenRecord();
        primaryRefreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        primaryRefreshToken.setEnvironment(ENVIRONMENT);
        primaryRefreshToken.setCredentialType(CredentialType.PrimaryRefreshToken.name().toLowerCase(Locale.US));
        primaryRefreshToken.setClientId(CLIENT_ID);
        primaryRefreshToken.setSessionKey(SESSION_KEY);

        mSharedPreferencesAccountCredentialCache.saveCredential(primaryRefreshToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.PrimaryRefreshToken,
                CLIENT_ID,
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                null,
                null,
                null
        );
        assertEquals(1, credentials.size());
    }

    @Test
    public void getCredentialsPRTAnotherClientId() {
        final PrimaryRefreshTokenRecord primaryRefreshToken = new PrimaryRefreshTokenRecord();
        primaryRefreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        primaryRefreshToken.setEnvironment(ENVIRONMENT);
        primaryRefreshToken.setCredentialType(CredentialType.PrimaryRefreshToken.name().toLowerCase(Locale.US));
        primaryRefreshToken.setClientId(CLIENT_ID);
        primaryRefreshToken.setSessionKey(SESSION_KEY);

        mSharedPreferencesAccountCredentialCache.saveCredential(primaryRefreshToken);

        List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentialsFilteredBy(
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                CredentialType.PrimaryRefreshToken,
                "another-client-id",
                null,
                null,
                null,
                null,
                null
        );
        assertTrue(credentials.isEmpty());
    }

    @Test
    public void clearAccounts() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAccounts()
        mSharedPreferencesAccountCredentialCache.removeAccount(account);

        // Verify getAccounts() returns zero items
        assertTrue(mSharedPreferencesAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns two items
        final List<Credential> credentials = mSharedPreferencesAccountCredentialCache.getCredentials();
        assertTrue(credentials.size() == 2);
    }

    @Test
    public void clearCredentials() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Call clearCredentials()
        mSharedPreferencesAccountCredentialCache.removeCredential(accessToken);
        mSharedPreferencesAccountCredentialCache.removeCredential(refreshToken);

        // Verify getAccounts() returns 1 item
        assertEquals(1, mSharedPreferencesAccountCredentialCache.getAccounts().size());

        // Verify getCredentials() returns zero items
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getCredentials().size());
    }

    @Test
    public void clearAll() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Call clearAll()
        mSharedPreferencesAccountCredentialCache.clearAll();

        // Verify getAccounts() returns zero items
        assertTrue(mSharedPreferencesAccountCredentialCache.getAccounts().isEmpty());

        // Verify getCredentials() returns zero items
        assertTrue(mSharedPreferencesAccountCredentialCache.getCredentials().isEmpty());
    }

    @Test
    public void testMalformedCredentialCacheKeyReturnsNull() {
        assertNull(mSharedPreferencesAccountCredentialCache.getCredential("Malformed cache key"));
    }

    @Test
    public void noValueForCacheKeyAccount() {
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getAccounts().size());
        final AccountRecord account = (AccountRecord) mSharedPreferencesAccountCredentialCache.getAccount("No account");
        assertNull(account);
    }

    @Test
    public void noValueForCacheKeyAccessToken() {
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getCredentials().size());
        final AccessTokenRecord accessToken = (AccessTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.AccessToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(accessToken);
    }

    @Test
    public void noValueForCacheKeyRefreshToken() {
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getCredentials().size());
        final RefreshTokenRecord refreshToken = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.RefreshToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(refreshToken);
    }

    @Test
    public void noValueForCacheKeyIdToken() {
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getCredentials().size());
        final IdTokenRecord idToken = (IdTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(CACHE_VALUE_SEPARATOR + CredentialType.IdToken.name().toLowerCase() + CACHE_VALUE_SEPARATOR);
        assertNull(idToken);
    }

    @Test
    public void malformedJsonCacheValueForAccount() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(account);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" \"not an account\"}");

        final AccountRecord malformedAccount = mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        assertNull(malformedAccount);
        assertNotNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedCacheValueForAccount() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(account);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" : \"not an account\"}");
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );

        final AccountRecord malformedAccount = mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        assertNull(malformedAccount);
        assertNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForAccessToken() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(accessToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" \"not an accessToken\"}");

        final AccessTokenRecord malformedAccessToken = (AccessTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedAccessToken);
        assertNotNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedCacheValueForAccessToken() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(accessToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" : \"not an accessToken\"}");
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );

        final AccessTokenRecord malformedAccessToken = (AccessTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedAccessToken);
        assertNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForRefreshToken() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.AccessToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(refreshToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" \"not a refreshToken\"}");

        final RefreshTokenRecord malformedRefreshToken = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedRefreshToken);
        assertNotNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedCacheValueForRefreshToken() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.AccessToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(refreshToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" : \"not a refreshToken\"}");
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );

        final RefreshTokenRecord malformedRefreshToken = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(malformedRefreshToken);
        assertNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedJsonCacheValueForIdToken() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(idToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\"  \"not an idToken\"}");

        final IdTokenRecord restoredIdToken = (IdTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(restoredIdToken);
        assertNotNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void malformedCacheValueForIdToken() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);

        // Generate a cache key
        final String cacheKey = mDelegate.generateCacheKey(idToken);

        mSharedPreferencesFileManager.put(cacheKey, "{\"thing\" : \"not an idToken\"}");
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );

        final IdTokenRecord restoredIdToken = (IdTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNull(restoredIdToken);
        assertNull(mSharedPreferencesFileManager.get(cacheKey));
    }

    @Test
    public void persistAndRestoreExtraClaimsAccount() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setMiddleName(MIDDLE_NAME);
        account.setName(NAME);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final String secondAdditionalKey = "extra-prop-2";
        final String secondAdditionalValue = "extra-value-2";
        final JsonElement secondAdditionalValueElement = new JsonPrimitive(secondAdditionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);
        additionalFields.put(secondAdditionalKey, secondAdditionalValueElement);

        account.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(account);

        // Resurrect the Credential
        final AccountRecord restoredAccount = mSharedPreferencesAccountCredentialCache.getAccount(credentialCacheKey);
        assertTrue(account.equals(restoredAccount));
        assertEquals(additionalValue, restoredAccount.getAdditionalFields().get(additionalKey).getAsString());
        assertEquals(secondAdditionalValue, restoredAccount.getAdditionalFields().get(secondAdditionalKey).getAsString());
    }

    @Test
    public void persistAndRestoreExtraClaimsAccessToken() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm(REALM);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        accessToken.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessToken);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessToken.equals(restoredAccessToken));
        assertEquals(additionalValue, restoredAccessToken.getAdditionalFields().get(additionalKey).getAsString());
    }

    @Test
    public void persistAndRestoreExtraClaimsRefreshToken() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        refreshToken.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshToken);

        // Resurrect the Credential
        final Credential restoredRefreshToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshToken.equals(restoredRefreshToken));
        assertEquals(additionalValue, restoredRefreshToken.getAdditionalFields().get(additionalKey).getAsString());
    }

    @Test
    public void persistAndRestoreExtraClaimsIdToken() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setRealm(REALM);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);
        idToken.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        idToken.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(idToken);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(idToken);

        // Resurrect the Credential
        final Credential restoredIdToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(idToken.equals(restoredIdToken));
        assertEquals(additionalValue, restoredIdToken.getAdditionalFields().get(additionalKey).getAsString());
    }

    @Test
    public void testAccountMerge() {
        final AccountRecord accountFirst = new AccountRecord();
        accountFirst.setHomeAccountId(HOME_ACCOUNT_ID);
        accountFirst.setEnvironment(ENVIRONMENT);
        accountFirst.setRealm(REALM);
        accountFirst.setLocalAccountId(LOCAL_ACCOUNT_ID);
        accountFirst.setUsername(USERNAME);
        accountFirst.setAuthorityType(AUTHORITY_TYPE);
        accountFirst.setMiddleName(MIDDLE_NAME);
        accountFirst.setName(NAME);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        accountFirst.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveAccount(accountFirst);

        // Save the second Account, with fields to merge...
        final AccountRecord accountSecond = new AccountRecord();
        accountSecond.setHomeAccountId(HOME_ACCOUNT_ID);
        accountSecond.setEnvironment(ENVIRONMENT);
        accountSecond.setRealm(REALM);
        accountSecond.setLocalAccountId(LOCAL_ACCOUNT_ID);
        accountSecond.setUsername(USERNAME);
        accountSecond.setAuthorityType(AUTHORITY_TYPE);
        accountSecond.setMiddleName(MIDDLE_NAME);
        accountSecond.setName(NAME);

        // Create and set some additional field data...
        final String additionalKey2 = "extra-prop-2";
        final String additionalValue2 = "extra-value-2";
        final JsonElement additionalValueElement2 = new JsonPrimitive(additionalValue2);

        final Map<String, JsonElement> additionalFields2 = new HashMap<>();
        additionalFields2.put(additionalKey2, additionalValueElement2);

        accountSecond.setAdditionalFields(additionalFields2);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveAccount(accountSecond);

        // Synthesize a cache key for it - either is fine.
        final String credentialCacheKey = mDelegate.generateCacheKey(accountFirst);

        // Resurrect the Credential
        final AccountRecord restoredAccount = mSharedPreferencesAccountCredentialCache.getAccount(credentialCacheKey);
        assertTrue(accountFirst.equals(restoredAccount));

        // Assert the presence of both additionalFields
        assertEquals(additionalValue, restoredAccount.getAdditionalFields().get(additionalKey).getAsString());
        assertEquals(additionalValue2, restoredAccount.getAdditionalFields().get(additionalKey2).getAsString());
    }

    @Test
    public void testAccessTokenMerge() {
        final AccessTokenRecord accessTokenFirst = new AccessTokenRecord();
        accessTokenFirst.setCredentialType(CredentialType.AccessToken.name());
        accessTokenFirst.setHomeAccountId(HOME_ACCOUNT_ID);
        accessTokenFirst.setRealm(REALM);
        accessTokenFirst.setEnvironment(ENVIRONMENT);
        accessTokenFirst.setClientId(CLIENT_ID);
        accessTokenFirst.setTarget(TARGET);
        accessTokenFirst.setCachedAt(CACHED_AT);
        accessTokenFirst.setExpiresOn(EXPIRES_ON);
        accessTokenFirst.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        accessTokenFirst.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessTokenFirst);

        final AccessTokenRecord accessTokenSecond = new AccessTokenRecord();
        accessTokenSecond.setCredentialType(CredentialType.AccessToken.name());
        accessTokenSecond.setHomeAccountId(HOME_ACCOUNT_ID);
        accessTokenSecond.setRealm(REALM);
        accessTokenSecond.setEnvironment(ENVIRONMENT);
        accessTokenSecond.setClientId(CLIENT_ID);
        accessTokenSecond.setTarget(TARGET);
        accessTokenSecond.setCachedAt(CACHED_AT);
        accessTokenSecond.setExpiresOn(EXPIRES_ON);
        accessTokenSecond.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey2 = "extra-prop-2";
        final String additionalValue2 = "extra-value-2";
        final JsonElement additionalValueElement2 = new JsonPrimitive(additionalValue2);

        final Map<String, JsonElement> additionalFields2 = new HashMap<>();
        additionalFields2.put(additionalKey2, additionalValueElement2);

        accessTokenSecond.setAdditionalFields(additionalFields2);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(accessTokenSecond);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(accessTokenFirst);

        // Resurrect the Credential
        final Credential restoredAccessToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(accessTokenFirst.equals(restoredAccessToken));
        assertEquals(additionalValue, restoredAccessToken.getAdditionalFields().get(additionalKey).getAsString());
        assertEquals(additionalValue2, restoredAccessToken.getAdditionalFields().get(additionalKey2).getAsString());
    }

    @Test
    public void testIdTokenMerge() {
        final IdTokenRecord idTokenFirst = new IdTokenRecord();
        idTokenFirst.setCredentialType(CredentialType.IdToken.name());
        idTokenFirst.setHomeAccountId(HOME_ACCOUNT_ID);
        idTokenFirst.setRealm(REALM);
        idTokenFirst.setEnvironment(ENVIRONMENT);
        idTokenFirst.setClientId(CLIENT_ID);
        idTokenFirst.setCachedAt(CACHED_AT);
        idTokenFirst.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        idTokenFirst.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(idTokenFirst);

        final IdTokenRecord idTokenSecond = new IdTokenRecord();
        idTokenSecond.setCredentialType(CredentialType.IdToken.name());
        idTokenSecond.setHomeAccountId(HOME_ACCOUNT_ID);
        idTokenSecond.setRealm(REALM);
        idTokenSecond.setEnvironment(ENVIRONMENT);
        idTokenSecond.setClientId(CLIENT_ID);
        idTokenSecond.setCachedAt(CACHED_AT);
        idTokenSecond.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey2 = "extra-prop-2";
        final String additionalValue2 = "extra-value-2";
        final JsonElement additionalValueElement2 = new JsonPrimitive(additionalValue2);

        final Map<String, JsonElement> additionalFields2 = new HashMap<>();
        additionalFields2.put(additionalKey2, additionalValueElement2);

        idTokenSecond.setAdditionalFields(additionalFields2);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(idTokenSecond);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(idTokenFirst);

        // Resurrect the Credential
        final Credential restoredIdToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(idTokenFirst.equals(restoredIdToken));
        assertEquals(additionalValue, restoredIdToken.getAdditionalFields().get(additionalKey).getAsString());
        assertEquals(additionalValue2, restoredIdToken.getAdditionalFields().get(additionalKey2).getAsString());
    }

    @Test
    public void testRefreshTokenMerge() {
        final RefreshTokenRecord refreshTokenFirst = new RefreshTokenRecord();
        refreshTokenFirst.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenFirst.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshTokenFirst.setEnvironment(ENVIRONMENT);
        refreshTokenFirst.setClientId(CLIENT_ID);
        refreshTokenFirst.setCachedAt(CACHED_AT);
        refreshTokenFirst.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        refreshTokenFirst.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshTokenFirst);

        final RefreshTokenRecord refreshTokenSecond = new RefreshTokenRecord();
        refreshTokenSecond.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenSecond.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshTokenSecond.setEnvironment(ENVIRONMENT);
        refreshTokenSecond.setClientId(CLIENT_ID);
        refreshTokenSecond.setCachedAt(CACHED_AT);
        refreshTokenSecond.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey2 = "extra-prop-2";
        final String additionalValue2 = "extra-value-2";
        final JsonElement additionalValueElement2 = new JsonPrimitive(additionalValue2);

        final Map<String, JsonElement> additionalFields2 = new HashMap<>();
        additionalFields2.put(additionalKey2, additionalValueElement2);

        refreshTokenSecond.setAdditionalFields(additionalFields2);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshTokenSecond);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshTokenFirst);

        // Resurrect the Credential
        final Credential restoredIdToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshTokenFirst.equals(restoredIdToken));
        assertEquals(additionalValue, restoredIdToken.getAdditionalFields().get(additionalKey).getAsString());
        assertEquals(additionalValue2, restoredIdToken.getAdditionalFields().get(additionalKey2).getAsString());
    }

    @Test
    public void testLatestMergedPropertyWins() {
        final RefreshTokenRecord refreshTokenFirst = new RefreshTokenRecord();
        refreshTokenFirst.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenFirst.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshTokenFirst.setEnvironment(ENVIRONMENT);
        refreshTokenFirst.setClientId(CLIENT_ID);
        refreshTokenFirst.setCachedAt(CACHED_AT);
        refreshTokenFirst.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey = "extra-prop-1";
        final String additionalValue = "extra-value-1";
        final JsonElement additionalValueElement = new JsonPrimitive(additionalValue);

        final Map<String, JsonElement> additionalFields = new HashMap<>();
        additionalFields.put(additionalKey, additionalValueElement);

        refreshTokenFirst.setAdditionalFields(additionalFields);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshTokenFirst);

        final RefreshTokenRecord refreshTokenSecond = new RefreshTokenRecord();
        refreshTokenSecond.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenSecond.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshTokenSecond.setEnvironment(ENVIRONMENT);
        refreshTokenSecond.setClientId(CLIENT_ID);
        refreshTokenSecond.setCachedAt(CACHED_AT);
        refreshTokenSecond.setSecret(SECRET);

        // Create and set some additional field data...
        final String additionalKey2 = "extra-prop-1";
        final String additionalValue2 = "extra-value-2";
        final JsonElement additionalValueElement2 = new JsonPrimitive(additionalValue2);

        final Map<String, JsonElement> additionalFields2 = new HashMap<>();
        additionalFields2.put(additionalKey2, additionalValueElement2);

        refreshTokenSecond.setAdditionalFields(additionalFields2);

        // Save the Credential
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshTokenSecond);

        // Synthesize a cache key for it
        final String credentialCacheKey = mDelegate.generateCacheKey(refreshTokenFirst);

        // Resurrect the Credential
        final Credential restoredIdToken = mSharedPreferencesAccountCredentialCache.getCredential(credentialCacheKey);
        assertTrue(refreshTokenFirst.equals(restoredIdToken));
        assertEquals(additionalValue2, restoredIdToken.getAdditionalFields().get(additionalKey).getAsString());
    }

    AccountRecord buildDefaultAccountRecord() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setMiddleName(MIDDLE_NAME);
        account.setName(NAME);
        return account;
    }

    RefreshTokenRecord buildDefaultRefreshToken() {
        final RefreshTokenRecord rt = new RefreshTokenRecord();
        rt.setCredentialType(CredentialType.RefreshToken.name());
        rt.setHomeAccountId(HOME_ACCOUNT_ID);
        rt.setEnvironment(ENVIRONMENT);
        rt.setClientId(CLIENT_ID);
        rt.setCachedAt(CACHED_AT);
        rt.setSecret(SECRET);
        return rt;
    }

    @Test
    public void testSavedAccountIsCloned() {
        AccountRecord account = buildDefaultAccountRecord();
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        final String cacheKey = mDelegate.generateCacheKey(account);
        AccountRecord retrieved = (AccountRecord) mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        assertNotSame(account, retrieved);
        assertEquals(account, retrieved);

        account.setLocalAccountId("banana");
        retrieved = (AccountRecord) mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        assertNotSame(account, retrieved);
        assertNotEquals(account, retrieved);
    }

    @Test
    public void testSavedCredentialIsCloned() {
        RefreshTokenRecord rt = buildDefaultRefreshToken();
        mSharedPreferencesAccountCredentialCache.saveCredential(rt);

        final String cacheKey = mDelegate.generateCacheKey(rt);
        RefreshTokenRecord retrieved = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNotSame(rt, retrieved);
        assertEquals(rt, retrieved);

        rt.setCachedAt("banana");
        retrieved = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNotSame(rt, retrieved);
        assertNotEquals(rt, retrieved);
    }

    @Test
    public void testReturnedAccountIsCloned() {
        AccountRecord account = buildDefaultAccountRecord();
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        final String cacheKey = mDelegate.generateCacheKey(account);
        AccountRecord retrieved1 = (AccountRecord) mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        retrieved1.setLocalAccountId("banana");

        AccountRecord retrieved2 = (AccountRecord) mSharedPreferencesAccountCredentialCache.getAccount(cacheKey);
        assertNotSame(retrieved1, retrieved2);
        assertNotEquals(retrieved1, retrieved2);
    }

    @Test
    public void testReturnedCredentialIsCloned() {
        RefreshTokenRecord rt = buildDefaultRefreshToken();
        mSharedPreferencesAccountCredentialCache.saveCredential(rt);

        final String cacheKey = mDelegate.generateCacheKey(rt);
        RefreshTokenRecord retrieved1 = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        retrieved1.setCachedAt("banana");

        RefreshTokenRecord retrieved2 = (RefreshTokenRecord) mSharedPreferencesAccountCredentialCache.getCredential(cacheKey);
        assertNotSame(retrieved1, retrieved2);
        assertNotEquals(retrieved1, retrieved2);
    }

    @Test
    public void testReturnedAllAccountsAreCloned() {
        AccountRecord account = buildDefaultAccountRecord();
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        List<AccountRecord> accounts1 = mSharedPreferencesAccountCredentialCache.getAccounts();
        assertEquals(1, accounts1.size());

        List<AccountRecord> accounts2 = mSharedPreferencesAccountCredentialCache.getAccounts();
        assertEquals(1, accounts2.size());

        assertNotSame(accounts1, accounts2);
        assertNotSame(accounts1.get(0), accounts2.get(0));
        assertEquals(accounts1.get(0), accounts2.get(0));

        accounts1.get(0).setLocalAccountId("banana");
        assertNotEquals(accounts1.get(0), accounts2.get(0));
    }

    @Test
    public void testReturnedAllCredentialsAreCloned() {
        RefreshTokenRecord rt = buildDefaultRefreshToken();
        mSharedPreferencesAccountCredentialCache.saveCredential(rt);

        List<Credential> creds1 = mSharedPreferencesAccountCredentialCache.getCredentials();
        assertEquals(1, creds1.size());

        List<Credential> creds2 = mSharedPreferencesAccountCredentialCache.getCredentials();
        assertEquals(1, creds2.size());

        assertNotSame(creds1, creds2);
        assertNotSame(creds1.get(0), creds2.get(0));
        assertEquals(creds1.get(0), creds2.get(0));

        creds1.get(0).setCachedAt("banana");
        assertNotEquals(creds1.get(0), creds2.get(0));
    }

    @Test
    public void testSha1ToSha512AppIdentifierUpdate() {
        //Mimics the scenario where the cache has access tokens with a SHA-1 app identifier,
        // and then the user updates their app to a version where access tokens should now have a SHA-512 app identifier.

        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        mSharedPreferencesAccountCredentialCache.saveAccount(account);

        // Save an AccessToken with SHA-1 application identifier into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA1);
        accessToken.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);

        // Save a RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        mSharedPreferencesAccountCredentialCache.saveCredential(refreshToken);

        // Verify getCredentials() returns two matching elements
        assertEquals(2, mSharedPreferencesAccountCredentialCache.getCredentials().size());

        //Recreate cache to set off SHA-1 access token removal
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );

        assertEquals(1, mSharedPreferencesAccountCredentialCache.getCredentials().size());

        // Now Save an AccessToken with SHA-512 application identifier into the cache
        final AccessTokenRecord accessToken2 = new AccessTokenRecord();
        accessToken2.setCredentialType(CredentialType.AccessToken.name());
        accessToken2.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken2.setRealm("Foo");
        accessToken2.setEnvironment(ENVIRONMENT);
        accessToken2.setClientId(CLIENT_ID);
        accessToken2.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
        accessToken2.setMamEnrollmentIdentifier(MAM_ENROLLMENT_IDENTIFIER);
        accessToken2.setTarget(TARGET);
        accessToken2.setCachedAt(CACHED_AT);
        accessToken2.setExpiresOn(EXPIRES_ON);
        accessToken2.setSecret(SECRET);
        mSharedPreferencesAccountCredentialCache.saveCredential(accessToken2);

        //Recreate cache and make sure cache didn't remove the SHA-512 token
        mSharedPreferencesAccountCredentialCache = new SharedPreferencesAccountCredentialCacheWithMemoryCache(
                mDelegate,
                mSharedPreferencesFileManager
        );
        assertEquals(2, mSharedPreferencesAccountCredentialCache.getCredentials().size());
    }

    @Test
    public void testClearSha1ApplicationIdentifierAccessTokens() {
        mSharedPreferencesAccountCredentialCache.clearSha1ApplicationIdentifierAccessTokens();
        assertEquals(0, mSharedPreferencesAccountCredentialCache.getCredentials().size());

        for (int i = 0; i < 3; i++) {
            final AccessTokenRecord accessToken = new AccessTokenRecord();
            accessToken.setCredentialType(CredentialType.AccessToken.name());

            switch (i) {
                case 0:
                    accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA512);
                    break;
                case 1:
                    accessToken.setApplicationIdentifier(null);
                    break;
                case 2:
                    accessToken.setApplicationIdentifier(APPLICATION_IDENTIFIER_SHA1);
                    break;
            }
            mSharedPreferencesAccountCredentialCache.saveCredential(accessToken);
        }
        assertEquals(3, mSharedPreferencesAccountCredentialCache.getCredentials().size());

        mSharedPreferencesAccountCredentialCache.clearSha1ApplicationIdentifierAccessTokens();
        assertEquals(2, mSharedPreferencesAccountCredentialCache.getCredentials().size());
    }
}
