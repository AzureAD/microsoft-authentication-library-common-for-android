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

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.java.cache.IAccountCredentialCache;
import com.microsoft.identity.common.java.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.PrimaryRefreshTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.shadows.ShadowAndroidSdkStorageEncryptionManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.identity.common.MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.APPLICATION_IDENTIFIER;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.AUTHORITY_TYPE;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.BEARER_AUTHENTICATION_SCHEME;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CACHED_AT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.EXPIRES_ON;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.LOCAL_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.REALM;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.REALM2;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SECRET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SESSION_KEY;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.TARGET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.USERNAME;
import static com.microsoft.identity.common.java.dto.CredentialType.AccessToken;
import static com.microsoft.identity.common.java.dto.CredentialType.IdToken;
import static com.microsoft.identity.common.java.dto.CredentialType.PrimaryRefreshToken;
import static com.microsoft.identity.common.java.dto.CredentialType.RefreshToken;
import static com.microsoft.identity.common.java.dto.CredentialType.V1IdToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class MsalOAuth2TokenCacheTest {

    private static final String JUNK_KEY = "9ac15f53-27ad-471a-ad57-033cdacb0ee9";
    private static final String JUNK_VALUE = "OWFjMTVmNTMtMjdhZC00NzFhLWFkNTctMDMzY2RhY2IwZWU5";

    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mOauth2TokenCache;
    private INameValueStorage<String> mSharedPreferencesFileManager;

    MicrosoftStsOAuth2Strategy mockStrategy;

    MicrosoftStsAuthorizationRequest mockRequest;

    MicrosoftStsTokenResponse mockResponse;
    
    IAccountCredentialAdapter<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mockCredentialAdapter;

    /**
     * For mocking broker responses where ADAL is the connecting client.
     */
    AccountCredentialTestBundle defaultTestBundleV1;

    /**
     * For mocking MSAL & MSAL-with-Broker responses.
     */
    AccountCredentialTestBundle defaultTestBundleV2;

    IAccountCredentialCache accountCredentialCache;

    public static class AccountCredentialTestBundle {

        final AccountRecord mGeneratedAccount;
        final AccessTokenRecord mGeneratedAccessToken;
        final RefreshTokenRecord mGeneratedRefreshToken;
        final IdTokenRecord mGeneratedIdToken;
        final PrimaryRefreshTokenRecord mGeneratedPrimaryRefreshToken;

        AccountCredentialTestBundle(final String authorityType,
                                    final String localAccountId, //guid
                                    final String username,
                                    final String homeAccountId, //guid
                                    final String environment,
                                    final String realm, //guid
                                    final String target,
                                    final String cacheAt,
                                    final String expiresOn,
                                    final String atSecret,
                                    final String clientId, //guid
                                    final String applicationIdentifier,
                                    final String rtSecret,
                                    final String idTokenSecret,
                                    final String familyId,
                                    final String prtSessionKey,
                                    final CredentialType idTokenType) {
            mGeneratedAccount = new AccountRecord();
            mGeneratedAccount.setAuthorityType(authorityType);
            mGeneratedAccount.setLocalAccountId(localAccountId);
            mGeneratedAccount.setUsername(username);
            mGeneratedAccount.setHomeAccountId(homeAccountId);
            mGeneratedAccount.setEnvironment(environment);
            mGeneratedAccount.setRealm(realm);

            mGeneratedAccessToken = new AccessTokenRecord();
            mGeneratedAccessToken.setRealm(realm);
            mGeneratedAccessToken.setTarget(target);
            mGeneratedAccessToken.setCachedAt(cacheAt);
            mGeneratedAccessToken.setExpiresOn(expiresOn);
            mGeneratedAccessToken.setSecret(atSecret);
            mGeneratedAccessToken.setHomeAccountId(homeAccountId);
            mGeneratedAccessToken.setEnvironment(environment);
            mGeneratedAccessToken.setCredentialType(AccessToken.name());
            mGeneratedAccessToken.setClientId(clientId);
            mGeneratedAccessToken.setApplicationIdentifier(applicationIdentifier);

            mGeneratedRefreshToken = new RefreshTokenRecord();
            mGeneratedRefreshToken.setSecret(rtSecret);
            mGeneratedRefreshToken.setTarget(target);
            mGeneratedRefreshToken.setHomeAccountId(homeAccountId);
            mGeneratedRefreshToken.setEnvironment(environment);
            mGeneratedRefreshToken.setCredentialType(RefreshToken.name());
            mGeneratedRefreshToken.setClientId(clientId);
            mGeneratedRefreshToken.setFamilyId(familyId);

            mGeneratedIdToken = new IdTokenRecord();
            mGeneratedIdToken.setHomeAccountId(homeAccountId);
            mGeneratedIdToken.setEnvironment(environment);
            mGeneratedIdToken.setRealm(realm);
            mGeneratedIdToken.setCredentialType(idTokenType.name());
            mGeneratedIdToken.setClientId(clientId);
            mGeneratedIdToken.setSecret(idTokenSecret);
            mGeneratedIdToken.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");

            mGeneratedPrimaryRefreshToken = new PrimaryRefreshTokenRecord();
            mGeneratedPrimaryRefreshToken.setSecret(rtSecret);
            mGeneratedPrimaryRefreshToken.setHomeAccountId(homeAccountId);
            mGeneratedPrimaryRefreshToken.setEnvironment(environment);
            mGeneratedPrimaryRefreshToken.setCredentialType(PrimaryRefreshToken.name());
            mGeneratedPrimaryRefreshToken.setClientId(clientId);
            mGeneratedPrimaryRefreshToken.setFamilyId(familyId);
            mGeneratedPrimaryRefreshToken.setExpiresOn(expiresOn);
            mGeneratedPrimaryRefreshToken.setCachedAt(cacheAt);
            mGeneratedPrimaryRefreshToken.setSessionKey(prtSessionKey);
            mGeneratedPrimaryRefreshToken.setPrtProtocolVersion("3.0");
        }
    }

    Context mContext;

    @Before
    public void setUp() throws Exception {
        mockStrategy = PowerMockito.mock(MicrosoftStsOAuth2Strategy.class);
        mockRequest = PowerMockito.mock(MicrosoftStsAuthorizationRequest.class);
        mockResponse = PowerMockito.mock(MicrosoftStsTokenResponse.class);
        mockCredentialAdapter = PowerMockito.mock(IAccountCredentialAdapter.class);

        // Used by mocks
        defaultTestBundleV1 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                LOCAL_ACCOUNT_ID,
                USERNAME,
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                REALM,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
                SESSION_KEY,
                V1IdToken
        );

        defaultTestBundleV2 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                LOCAL_ACCOUNT_ID,
                USERNAME,
                HOME_ACCOUNT_ID,
                ENVIRONMENT,
                REALM,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
                SESSION_KEY,
                IdToken
        );

        // Mocks
        configureMocksForTestBundle(defaultTestBundleV2);

        // Context and related init
        mContext = ApplicationProvider.getApplicationContext();

        final IPlatformComponents components = AndroidPlatformComponentsFactory.createFromContext(mContext);
        mSharedPreferencesFileManager = components.getEncryptedNameValueStore(
                "test_prefs",
                components.getStorageEncryptionManager(),
                String.class
        );

        final ICacheKeyValueDelegate keyValueDelegate = new CacheKeyValueDelegate();

        accountCredentialCache = new SharedPreferencesAccountCredentialCache(
                keyValueDelegate,
                mSharedPreferencesFileManager
        );

        mOauth2TokenCache = new MsalOAuth2TokenCache<>(
                components,
                accountCredentialCache,
                mockCredentialAdapter
        );
    }

    @After
    public void tearDown() {
        mSharedPreferencesFileManager.clear();
    }

    @Test
    public void saveTokens() throws Exception {
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV2.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        sortResultToLists(credentials, rts, ats, ids);

        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void saveTokensWithMalformedDataInCache() throws Exception {
        // Prepopulate the cache with unparseable, junk data
        mSharedPreferencesFileManager.put(JUNK_KEY, JUNK_VALUE);

        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV2.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        sortResultToLists(credentials, rts, ats, ids);

        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, ids.get(0));

        // Verify that our junk data still exists
        assertEquals(JUNK_VALUE, mSharedPreferencesFileManager.get(JUNK_KEY));
    }

    @Test
    public void saveTokensWithMalformedDataInCacheReverseOrder() throws Exception {
        // Prepopulate valid data into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        // Then insert unparseable, junk data
        mSharedPreferencesFileManager.put(JUNK_KEY, JUNK_VALUE);

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV2.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        sortResultToLists(credentials, rts, ats, ids);

        validateResultListContents(rts, ats, ids);

        // Verify that our junk data still exists
        assertEquals(JUNK_VALUE, mSharedPreferencesFileManager.get(JUNK_KEY));
    }

    private void validateResultListContents(List<Credential> rts, List<Credential> ats, List<Credential> ids) {
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void saveTokensV1Compat() throws ClientException {
        // This test asserts that if an IdToken is returned in the v1 format (broker cases),
        // it is saved property.
        loadTestBundleIntoCache(defaultTestBundleV1);

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV1.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        for (final Credential credential : credentials) {
            switch (CredentialType.fromString(credential.getCredentialType())) {
                case AccessToken:
                    ats.add(credential);
                    break;
                case RefreshToken:
                    rts.add(credential);
                    break;
                case V1IdToken:
                    ids.add(credential);
                    break;
                default:
                    fail("Unexpected value: " + credential.getCredentialType());
            }
        }

        assertEquals(defaultTestBundleV1.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundleV1.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundleV1.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void saveTokensWithAggregationSingleEntry() throws ClientException {
        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV2
        );

        assertEquals(1, result.size());

        final ICacheRecord entry = result.get(0);

        assertNotNull(entry.getAccount());
        assertNotNull(entry.getIdToken());
        assertNotNull(entry.getAccessToken());
        assertNotNull(entry.getRefreshToken());
    }

    @Test
    public void saveTokensWithAggregationSingleEntryWithMalformedDataInCache() throws ClientException {
        // Prepopulate the cache with unparseable, junk data
        mSharedPreferencesFileManager.put(JUNK_KEY, JUNK_VALUE);

        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV2
        );

        assertEquals(1, result.size());

        final ICacheRecord entry = result.get(0);

        assertEquals(defaultTestBundleV2.mGeneratedAccount, entry.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, entry.getIdToken());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, entry.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, entry.getRefreshToken());

        // Verify that our junk data still exists
        assertEquals(JUNK_VALUE, mSharedPreferencesFileManager.get(JUNK_KEY));
    }

    @Test
    public void saveTokensWithAggregationV1SingleEntry() throws ClientException {
        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV1
        );

        assertEquals(1, result.size());

        final ICacheRecord entry = result.get(0);

        assertNotNull(entry.getAccount());
        assertNotNull(entry.getV1IdToken());
        assertNotNull(entry.getAccessToken());
        assertNotNull(entry.getRefreshToken());
    }

    @Test
    public void saveTokensWithAggregationMultiEntry() throws ClientException {
        // Load additional creds into the cache to simulate a guest account...
        // at, id, account
        final AccessTokenRecord at = new AccessTokenRecord();
        at.setRealm(REALM2);
        at.setCachedAt(CACHED_AT);
        at.setExpiresOn(EXPIRES_ON);
        at.setSecret(SECRET);
        at.setHomeAccountId(HOME_ACCOUNT_ID);
        at.setEnvironment(ENVIRONMENT);
        at.setCredentialType(AccessToken.name());
        at.setClientId(CLIENT_ID);
        at.setTarget(TARGET);

        final IdTokenRecord id = new IdTokenRecord();
        id.setHomeAccountId(HOME_ACCOUNT_ID);
        id.setEnvironment(ENVIRONMENT);
        id.setRealm(REALM2);
        id.setCredentialType(IdToken.name());
        id.setClientId(CLIENT_ID);
        id.setSecret(MOCK_ID_TOKEN_WITH_CLAIMS);
        id.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");

        final AccountRecord acct = new AccountRecord();
        acct.setAuthorityType(AUTHORITY_TYPE);
        acct.setLocalAccountId(UUID.randomUUID().toString());
        acct.setUsername(USERNAME);
        acct.setHomeAccountId(HOME_ACCOUNT_ID);
        acct.setEnvironment(ENVIRONMENT);
        acct.setRealm(REALM2);

        accountCredentialCache.saveAccount(acct);
        accountCredentialCache.saveCredential(at);
        accountCredentialCache.saveCredential(id);

        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV2
        );

        assertEquals(2, result.size());

        final ICacheRecord entry1 = result.get(0);

        assertNotNull(entry1.getAccount());
        assertNotNull(entry1.getIdToken());
        assertNotNull(entry1.getAccessToken());
        assertNotNull(entry1.getRefreshToken());

        final ICacheRecord entry2 = result.get(1);

        assertNotNull(entry2.getAccount());
        assertNotNull(entry2.getIdToken());
        assertNull(entry2.getAccessToken());
        assertNull(entry2.getRefreshToken());
    }

    @Test
    public void saveTokensWithAggregationV1MultiEntry() throws ClientException {
        // Load additional creds into the cache to simulate a guest account...
        // at, id, account
        final AccessTokenRecord at = new AccessTokenRecord();
        at.setRealm(REALM2);
        at.setCachedAt(CACHED_AT);
        at.setExpiresOn(EXPIRES_ON);
        at.setSecret(SECRET);
        at.setHomeAccountId(HOME_ACCOUNT_ID);
        at.setEnvironment(ENVIRONMENT);
        at.setCredentialType(AccessToken.name());
        at.setClientId(CLIENT_ID);
        at.setTarget(TARGET);

        final IdTokenRecord id = new IdTokenRecord();
        id.setHomeAccountId(HOME_ACCOUNT_ID);
        id.setEnvironment(ENVIRONMENT);
        id.setRealm(REALM2);
        id.setCredentialType(IdToken.name());
        id.setClientId(CLIENT_ID);
        id.setSecret(MOCK_ID_TOKEN_WITH_CLAIMS);
        id.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");

        final AccountRecord acct = new AccountRecord();
        acct.setAuthorityType(AUTHORITY_TYPE);
        acct.setLocalAccountId(UUID.randomUUID().toString());
        acct.setUsername(USERNAME);
        acct.setHomeAccountId(HOME_ACCOUNT_ID);
        acct.setEnvironment(ENVIRONMENT);
        acct.setRealm(REALM2);

        accountCredentialCache.saveAccount(acct);
        accountCredentialCache.saveCredential(at);
        accountCredentialCache.saveCredential(id);

        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV1
        );

        assertEquals(2, result.size());

        final ICacheRecord entry1 = result.get(0);

        assertNotNull(entry1.getAccount());
        assertNotNull(entry1.getV1IdToken());
        assertNotNull(entry1.getAccessToken());
        assertNotNull(entry1.getRefreshToken());

        final ICacheRecord entry2 = result.get(1);

        assertNotNull(entry2.getAccount());
        assertNotNull(entry2.getIdToken());
        assertNull(entry2.getAccessToken());
        assertNull(entry2.getRefreshToken());
    }

    public void save2ArgOverloadMethodV1Compat() {
        // This test asserts that if an IdToken is returned in the v1 format (broker cases),
        // it is saved properly.

        mOauth2TokenCache.save(defaultTestBundleV1.mGeneratedAccount, defaultTestBundleV1.mGeneratedIdToken);

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV1.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(1, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        sortResultToLists(credentials, rts, ats, ids);

        assertEquals(defaultTestBundleV1.mGeneratedIdToken, ids.get(0));
    }

    private void sortResultToLists(List<Credential> credentials, List<Credential> rts, List<Credential> ats, List<Credential> ids) {
        for (final Credential credential : credentials) {
            switch (CredentialType.fromString(credential.getCredentialType())) {
                case AccessToken:
                    ats.add(credential);
                    break;
                case RefreshToken:
                    rts.add(credential);
                    break;
                case IdToken:
                    ids.add(credential);
                    break;
                default:
                    fail("Unexpected value: " + credential.getCredentialType());
            }
        }
    }

    @Test
    public void saveTokensWithIntersect() throws Exception {
        // Manually insert an AT with a ltd scope into the cache
        final String extendedScopes = "calendar.modify user.read user.write https://graph.windows.net";

        AccessTokenRecord accessTokenToClear = new AccessTokenRecord();
        accessTokenToClear.setRealm(REALM);
        accessTokenToClear.setCachedAt(CACHED_AT);
        accessTokenToClear.setExpiresOn(EXPIRES_ON);
        accessTokenToClear.setSecret(SECRET);
        accessTokenToClear.setHomeAccountId(HOME_ACCOUNT_ID);
        accessTokenToClear.setEnvironment(ENVIRONMENT);
        accessTokenToClear.setCredentialType(AccessToken.name());
        accessTokenToClear.setClientId(CLIENT_ID);
        accessTokenToClear.setTarget(TARGET);

        // Save this dummy AT
        accountCredentialCache.saveCredential(accessTokenToClear);

        // Set the wider target on the new AT to write...
        defaultTestBundleV2.mGeneratedAccessToken.setTarget(extendedScopes);

        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundleV2.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        sortResultToLists(credentials, rts, ats, ids);

        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void saveAccountDirect() {
        saveAccountDirect(defaultTestBundleV2);
    }

    @Test
    public void saveAccountDirectV1Compat() {
        saveAccountDirect(defaultTestBundleV1);
    }

    private void saveAccountDirect(@NonNull final AccountCredentialTestBundle testBundle) {
        mOauth2TokenCache.save(
                testBundle.mGeneratedAccount,
                testBundle.mGeneratedIdToken
        );

        final AccountRecord account = mOauth2TokenCache.getAccount(
                ENVIRONMENT,
                CLIENT_ID,
                HOME_ACCOUNT_ID,
                REALM
        );

        final ICacheRecord cacheRecord = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                account,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(cacheRecord);
        assertNotNull(cacheRecord.getAccount());

        if (testBundle == defaultTestBundleV2) {
            assertNotNull(cacheRecord.getIdToken());
            assertNull(cacheRecord.getV1IdToken());
            assertEquals(
                    testBundle.mGeneratedIdToken,
                    cacheRecord.getIdToken()
            );
        } else {
            assertNotNull(cacheRecord.getV1IdToken());
            assertNull(cacheRecord.getIdToken());
            assertEquals(
                    testBundle.mGeneratedIdToken,
                    cacheRecord.getV1IdToken()
            );
        }

        assertNull(cacheRecord.getAccessToken());
        assertNull(cacheRecord.getRefreshToken());

        assertEquals(
                testBundle.mGeneratedAccount,
                cacheRecord.getAccount()
        );
    }

    @Test
    public void getAccount() throws ClientException {
        // Save an Account into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertAccountLoaded();
    }

    @Test
    public void getAccountV1Compat() throws ClientException {
        loadTestBundleIntoCache(defaultTestBundleV1);

        assertAccountLoaded();
    }

    private void assertAccountLoaded() {
        final AccountRecord account = mOauth2TokenCache.getAccount(
                ENVIRONMENT,
                CLIENT_ID,
                HOME_ACCOUNT_ID,
                REALM
        );

        assertNotNull(account);
        assertEquals(MicrosoftAccount.AUTHORITY_TYPE_MS_STS, account.getAuthorityType());
        Assert.assertEquals(LOCAL_ACCOUNT_ID, account.getLocalAccountId());
        Assert.assertEquals(USERNAME, account.getUsername());
        Assert.assertEquals(HOME_ACCOUNT_ID, account.getHomeAccountId());
        Assert.assertEquals(ENVIRONMENT, account.getEnvironment());
        Assert.assertEquals(REALM, account.getRealm());
    }

    @Test
    public void getAccountDisambiguate() throws ClientException {
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                            UUID.randomUUID().toString(),
                            "test.user@tenant.onmicrosoft.com",
                            HOME_ACCOUNT_ID,
                            ENVIRONMENT,
                            UUID.randomUUID().toString(),
                            TARGET,
                            CACHED_AT,
                            EXPIRES_ON,
                            SECRET,
                            CLIENT_ID,
                            APPLICATION_IDENTIFIER,
                            SECRET,
                            MOCK_ID_TOKEN_WITH_CLAIMS,
                            null,
                            SESSION_KEY,
                            IdToken
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            loadTestBundleIntoCache(testBundles.get(i));
        }

        final List<AccountRecord> accounts = mOauth2TokenCache.getAccounts(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(testBundles.size(), accounts.size());
        for (final AccountCredentialTestBundle bundle : testBundles) {
            assertTrue(accounts.contains(bundle.mGeneratedAccount));
        }
    }

    @Test
    public void getAccountByLocalAccountId() throws ClientException {
        // Save an Account into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        // Find it by the local_account_id
        final AccountRecord account = mOauth2TokenCache.getAccountByLocalAccountId(
                ENVIRONMENT,
                CLIENT_ID,
                LOCAL_ACCOUNT_ID
        );

        assertNotNull(account);
    }

    @Test
    public void getAccountByLocalAccountIdWithAggregatedData() throws ClientException {
        // Save an Account into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final ICacheRecord resultRecord =
                mOauth2TokenCache.getAccountWithAggregatedAccountDataByLocalAccountId(
                        ENVIRONMENT,
                        CLIENT_ID,
                        LOCAL_ACCOUNT_ID
                );

        assertNotNull(resultRecord);
        assertNotNull(resultRecord.getAccount());
        assertNotNull(resultRecord.getIdToken());
        assertNull(resultRecord.getV1IdToken());
        assertNull(resultRecord.getAccessToken());
        assertNull(resultRecord.getRefreshToken());
    }

    @Test
    public void getAccountByLocalAccountIdWithAggregatedDataV1() throws ClientException {
        // Save an Account into the cache
        loadTestBundleIntoCache(defaultTestBundleV1);

        final ICacheRecord resultRecord =
                mOauth2TokenCache.getAccountWithAggregatedAccountDataByLocalAccountId(
                        ENVIRONMENT,
                        CLIENT_ID,
                        LOCAL_ACCOUNT_ID
                );

        assertNotNull(resultRecord);
        assertNotNull(resultRecord.getAccount());
        assertNotNull(resultRecord.getV1IdToken());
        assertNull(resultRecord.getIdToken());
        assertNull(resultRecord.getAccessToken());
        assertNull(resultRecord.getRefreshToken());
    }

    @Test
    public void getAccountsWithAggregatedAccountData() throws ClientException {
        loadTestBundleIntoCache(defaultTestBundleV2);

        final List<ICacheRecord> cacheRecords =
                mOauth2TokenCache.getAccountsWithAggregatedAccountData(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID
                );

        assertEquals(1, cacheRecords.size());

        final ICacheRecord cacheRecord = cacheRecords.get(0);

        assertNotNull(cacheRecord.getAccount());
        assertNotNull(cacheRecord.getIdToken());
        assertNull(cacheRecord.getAccessToken());
        assertNull(cacheRecord.getRefreshToken());
        assertNull(cacheRecord.getV1IdToken());
    }

    @Test
    public void getAccountsWithAggregatedAccountDataV1() throws ClientException {
        loadTestBundleIntoCache(defaultTestBundleV1);

        final List<ICacheRecord> cacheRecords =
                mOauth2TokenCache.getAccountsWithAggregatedAccountData(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID
                );

        assertEquals(1, cacheRecords.size());

        final ICacheRecord cacheRecord = cacheRecords.get(0);

        assertNotNull(cacheRecord.getAccount());
        assertNotNull(cacheRecord.getV1IdToken());
        assertNull(cacheRecord.getAccessToken());
        assertNull(cacheRecord.getRefreshToken());
        assertNull(cacheRecord.getIdToken());
    }

    @Test
    public void getAccountCacheEmpty() {
        final AccountRecord account = mOauth2TokenCache.getAccount(
                ENVIRONMENT,
                CLIENT_ID,
                HOME_ACCOUNT_ID,
                REALM
        );

        assertNull(account);
    }

    @Test
    public void getAccounts() throws ClientException {
        getAccounts(IdToken);
    }

    @Test
    public void getAccountsV1Compat() throws ClientException {
        getAccounts(V1IdToken);
    }

    private void getAccounts(@NonNull CredentialType idTokenType) throws ClientException {
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                            UUID.randomUUID().toString(),
                            "test.user" + ii + "@tenant.onmicrosoft.com",
                            UUID.randomUUID().toString(),
                            ENVIRONMENT,
                            UUID.randomUUID().toString(),
                            TARGET,
                            CACHED_AT,
                            EXPIRES_ON,
                            SECRET,
                            CLIENT_ID,
                            APPLICATION_IDENTIFIER,
                            SECRET,
                            MOCK_ID_TOKEN_WITH_CLAIMS,
                            null,
                            SESSION_KEY,
                            idTokenType
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            loadTestBundleIntoCache(testBundles.get(i));
        }

        final List<AccountRecord> accounts = mOauth2TokenCache.getAccounts(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(testBundles.size(), accounts.size());
        for (final AccountCredentialTestBundle bundle : testBundles) {
            assertTrue(accounts.contains(bundle.mGeneratedAccount));
        }
    }

    @Test
    public void getAccountsWithDeletion() throws ClientException {
        getAccountsWithDeletion(IdToken);
    }

    @Test
    public void getAccountsWithDeletionV1Compat() throws ClientException {
        getAccountsWithDeletion(V1IdToken);
    }

    private void getAccountsWithDeletion(@NonNull final CredentialType idTokenType) throws ClientException {
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        final String randomHomeAccountId = UUID.randomUUID().toString();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                            UUID.randomUUID().toString(),
                            "test.user" + ii + "@tenant.onmicrosoft.com",
                            randomHomeAccountId,
                            ENVIRONMENT,
                            UUID.randomUUID().toString(),
                            TARGET,
                            CACHED_AT,
                            EXPIRES_ON,
                            SECRET,
                            CLIENT_ID,
                            APPLICATION_IDENTIFIER,
                            SECRET,
                            MOCK_ID_TOKEN_WITH_CLAIMS,
                            null,
                            SESSION_KEY,
                            idTokenType
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            loadTestBundleIntoCache(testBundles.get(i));
        }

        final List<AccountRecord> accounts = mOauth2TokenCache.getAccounts(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(
                mOauth2TokenCache.removeAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        randomHomeAccountId,
                        null
                ).size(),
                accounts.size()
        );
    }

    @Test
    public void getAccountsCacheEmpty() {
        final List<AccountRecord> accounts = mOauth2TokenCache.getAccounts(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(0, accounts.size());
    }

    @Test
    public void removeAccount() throws ClientException {
        // Save an Account into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertEquals(
                mOauth2TokenCache.removeAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                ).size(),
                1
        );
    }

    @Test
    public void removeAccountV1Compat() throws ClientException {
        loadTestBundleIntoCache(defaultTestBundleV1);

        assertEquals(
                mOauth2TokenCache.removeAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                ).size(),
                1
        );
    }

    private ICacheRecord loadTestBundleIntoCache(
            @NonNull final AccountCredentialTestBundle testBundle) throws ClientException {
        configureMocksForTestBundle(testBundle);

        return mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );
    }

    private List<ICacheRecord> loadTestBundleIntoCacheWithAggregation(
            @NonNull final AccountCredentialTestBundle testBundle
    ) throws ClientException {
        configureMocksForTestBundle(testBundle);

        return mOauth2TokenCache.saveAndLoadAggregatedAccountData(
                mockStrategy,
                mockRequest,
                mockResponse
        );
    }

    private void configureMocksForTestBundle(@NonNull final AccountCredentialTestBundle testBundle) {
        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedIdToken);
    }

    @Test
    public void removeAccountNoMatch() throws ClientException {
        // Save an Account into the cache
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertEquals(
                mOauth2TokenCache.removeAccount(
                        "login.chinacloudapi.cn",
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                ).size(),
                0
        );
    }

    @Test
    public void removeAccountCacheEmpty() {
        assertEquals(
                mOauth2TokenCache.removeAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                ).size(),
                0
        );
    }

    @Test
    public void loadTokens() throws ClientException {
        // Save an Account into the cache
        final ICacheRecord result = mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertEquals(defaultTestBundleV2.mGeneratedAccount, result.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, result.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, result.getRefreshToken());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, result.getIdToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV2.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(result, secondaryLoad);
    }

    @Test
    public void loadTokensV1Compat() throws ClientException {
        final ICacheRecord result = loadTestBundleIntoCache(defaultTestBundleV1);

        assertEquals(defaultTestBundleV1.mGeneratedAccount, result.getAccount());
        assertEquals(defaultTestBundleV1.mGeneratedAccessToken, result.getAccessToken());
        assertEquals(defaultTestBundleV1.mGeneratedRefreshToken, result.getRefreshToken());
        assertEquals(defaultTestBundleV1.mGeneratedIdToken, result.getV1IdToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV1.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(result, secondaryLoad);
    }

    @Test
    public void loadTokensWithAggregatedData() throws ClientException {
        final ICacheRecord result = loadTestBundleIntoCache(defaultTestBundleV2);

        assertEquals(defaultTestBundleV2.mGeneratedAccount, result.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, result.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, result.getRefreshToken());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, result.getIdToken());

        final List<ICacheRecord> secondaryLoad =
                mOauth2TokenCache.loadWithAggregatedAccountData(
                        CLIENT_ID,
                        APPLICATION_IDENTIFIER,
                        TARGET,
                        defaultTestBundleV2.mGeneratedAccount,
                        BEARER_AUTHENTICATION_SCHEME
                );

        assertEquals(1, secondaryLoad.size());

        final ICacheRecord secondaryResult = secondaryLoad.get(0);

        assertEquals(defaultTestBundleV2.mGeneratedAccount, secondaryResult.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, secondaryResult.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, secondaryResult.getIdToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, secondaryResult.getRefreshToken());
    }

    @Test
    public void loadTokensWithAggregatedDataV1() throws ClientException {
        final ICacheRecord result = loadTestBundleIntoCache(defaultTestBundleV1);

        assertEquals(defaultTestBundleV1.mGeneratedAccount, result.getAccount());
        assertEquals(defaultTestBundleV1.mGeneratedAccessToken, result.getAccessToken());
        assertEquals(defaultTestBundleV1.mGeneratedRefreshToken, result.getRefreshToken());
        assertEquals(defaultTestBundleV1.mGeneratedIdToken, result.getV1IdToken());

        final List<ICacheRecord> secondaryLoad =
                mOauth2TokenCache.loadWithAggregatedAccountData(
                        CLIENT_ID,
                        APPLICATION_IDENTIFIER,
                        TARGET,
                        defaultTestBundleV1.mGeneratedAccount,
                        BEARER_AUTHENTICATION_SCHEME
                );

        assertEquals(1, secondaryLoad.size());

        final ICacheRecord secondaryResult = secondaryLoad.get(0);

        assertEquals(defaultTestBundleV1.mGeneratedAccount, secondaryResult.getAccount());
        assertEquals(defaultTestBundleV1.mGeneratedAccessToken, secondaryResult.getAccessToken());
        assertEquals(defaultTestBundleV1.mGeneratedIdToken, secondaryResult.getV1IdToken());
        assertEquals(defaultTestBundleV1.mGeneratedRefreshToken, secondaryResult.getRefreshToken());
    }

    @Test
    public void removeAccessToken() throws ClientException {
        // Save an Account into the cache
        final ICacheRecord result = mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        mOauth2TokenCache.removeCredential(result.getAccessToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV2.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(defaultTestBundleV2.mGeneratedAccount, secondaryLoad.getAccount());
        assertNull(secondaryLoad.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, secondaryLoad.getRefreshToken());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, secondaryLoad.getIdToken());
    }

    @Test
    public void removeRefreshToken() throws ClientException {
        // Save an Account into the cache
        final ICacheRecord result = mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        mOauth2TokenCache.removeCredential(result.getRefreshToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV2.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(defaultTestBundleV2.mGeneratedAccount, secondaryLoad.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, secondaryLoad.getAccessToken());
        assertNull(secondaryLoad.getRefreshToken());
        assertEquals(defaultTestBundleV2.mGeneratedIdToken, secondaryLoad.getIdToken());
    }

    @Test
    public void removeIdToken() throws ClientException {
        // Save an Account into the cache
        final ICacheRecord result = mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        mOauth2TokenCache.removeCredential(result.getIdToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV2.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(defaultTestBundleV2.mGeneratedAccount, secondaryLoad.getAccount());
        assertEquals(defaultTestBundleV2.mGeneratedAccessToken, secondaryLoad.getAccessToken());
        assertEquals(defaultTestBundleV2.mGeneratedRefreshToken, secondaryLoad.getRefreshToken());
        assertNull(secondaryLoad.getIdToken());
        assertNull(secondaryLoad.getV1IdToken());
    }

    @Test
    public void removeV1IdToken() throws ClientException {
        final ICacheRecord result = loadTestBundleIntoCache(defaultTestBundleV1);

        mOauth2TokenCache.removeCredential(result.getV1IdToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                TARGET,
                defaultTestBundleV2.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertEquals(defaultTestBundleV1.mGeneratedAccount, secondaryLoad.getAccount());
        assertEquals(defaultTestBundleV1.mGeneratedAccessToken, secondaryLoad.getAccessToken());
        assertEquals(defaultTestBundleV1.mGeneratedRefreshToken, secondaryLoad.getRefreshToken());
        assertNull(secondaryLoad.getIdToken());
        assertNull(secondaryLoad.getV1IdToken());
    }

    @Test
    public void getAccountsWithIdTokens() throws ClientException {
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<ICacheRecord> records = mOauth2TokenCache.getAccountsWithAggregatedAccountData(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(1, records.size());

        final ICacheRecord retrievedRecord = records.get(0);

        assertNotNull(retrievedRecord);
        assertNotNull(retrievedRecord.getAccount());
        assertNotNull(retrievedRecord.getIdToken());
        assertNull(retrievedRecord.getV1IdToken());
        assertNull(retrievedRecord.getAccessToken());
        assertNull(retrievedRecord.getRefreshToken());
    }

    @Test
    public void getAccountsWithIdTokensV1() throws ClientException {
        final ICacheRecord result = loadTestBundleIntoCache(defaultTestBundleV1);

        final List<ICacheRecord> records = mOauth2TokenCache.getAccountsWithAggregatedAccountData(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertEquals(1, records.size());

        final ICacheRecord retrievedRecord = records.get(0);

        assertNotNull(retrievedRecord);
        assertNotNull(retrievedRecord.getAccount());
        assertNotNull(retrievedRecord.getV1IdToken());
        assertNull(retrievedRecord.getIdToken());
        assertNull(retrievedRecord.getAccessToken());
        assertNull(retrievedRecord.getRefreshToken());
    }

    @Test
    public void getAccoutsWithIdTokensEmpty() {
        final List<ICacheRecord> records = mOauth2TokenCache.getAccountsWithAggregatedAccountData(
                ENVIRONMENT,
                CLIENT_ID
        );

        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testFrtFallback() throws ClientException {
        // This test verifies changes in common/#893
        // We will fallback on an FRT in the local cache for the current user if one is avail
        final List<ICacheRecord> result = loadTestBundleIntoCacheWithAggregation(
                defaultTestBundleV2
        );

        assertEquals(1, result.size());

        final ICacheRecord entry = result.get(0);

        assertNotNull(entry.getAccount());
        assertNotNull(entry.getIdToken());
        assertNotNull(entry.getAccessToken());
        assertNotNull(entry.getRefreshToken());

        // delete the existing RT and insert our FRT
        accountCredentialCache.removeCredential(entry.getRefreshToken());

        // Modify the existing RT to change the client_id and set a FoCI affiliation
        final String fociRtClientId = UUID.randomUUID().toString();
        final RefreshTokenRecord modifiedRT = entry.getRefreshToken();
        modifiedRT.setFamilyId("1");
        modifiedRT.setClientId(fociRtClientId);

        accountCredentialCache.saveCredential(modifiedRT);

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                entry.getAccessToken().getClientId(),
                APPLICATION_IDENTIFIER,
                TARGET,
                entry.getAccount(),
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(secondaryLoad.getRefreshToken());
        assertEquals(fociRtClientId, secondaryLoad.getRefreshToken().getClientId());
    }

    @Test
    public void testGetFamilyRefreshTokenForHomeAccountIdValidCase() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        accountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accountCredentialCache.saveCredential(accessToken);

        // Save a Family RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("1");
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        accountCredentialCache.saveCredential(refreshToken);

        final IdTokenRecord id = new IdTokenRecord();
        id.setHomeAccountId(HOME_ACCOUNT_ID);
        id.setEnvironment(ENVIRONMENT);
        id.setRealm(REALM2);
        id.setCredentialType(IdToken.name());
        id.setClientId(CLIENT_ID);
        id.setSecret(MOCK_ID_TOKEN_WITH_CLAIMS);
        id.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");
        accountCredentialCache.saveCredential(id);

        final RefreshTokenRecord refreshTokenRecord = mOauth2TokenCache.
                getFamilyRefreshTokenForHomeAccountId(HOME_ACCOUNT_ID);
        assertNotNull(refreshTokenRecord);
        Assert.assertEquals(refreshTokenRecord.getSecret(), SECRET);
    }

    @Test
    public void testGetFamilyRefreshTokenForHomeAccountIdNullCase() {
        final RefreshTokenRecord refreshTokenRecord = mOauth2TokenCache.
                getFamilyRefreshTokenForHomeAccountId(HOME_ACCOUNT_ID);
        assertNull(refreshTokenRecord);
    }

    @Test
    public void testGetFamilyRefreshTokenForHomeAccountIdNoAccountWithHomeAccountId() {
        // Save an Account into the cache
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setAuthorityType(AUTHORITY_TYPE);
        accountCredentialCache.saveAccount(account);

        // Save an AccessToken into the cache
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setCredentialType(AccessToken.name());
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setRealm("Foo");
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);
        accessToken.setCachedAt(CACHED_AT);
        accessToken.setExpiresOn(EXPIRES_ON);
        accessToken.setSecret(SECRET);
        accountCredentialCache.saveCredential(accessToken);

        // Save a Family RefreshToken into the cache
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setCredentialType(RefreshToken.name());
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("1");
        refreshToken.setSecret(SECRET);
        refreshToken.setTarget(TARGET);
        accountCredentialCache.saveCredential(refreshToken);

        final IdTokenRecord id = new IdTokenRecord();
        id.setHomeAccountId(HOME_ACCOUNT_ID);
        id.setEnvironment(ENVIRONMENT);
        id.setRealm(REALM2);
        id.setCredentialType(IdToken.name());
        id.setClientId(CLIENT_ID);
        id.setSecret(MOCK_ID_TOKEN_WITH_CLAIMS);
        id.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");
        accountCredentialCache.saveCredential(id);

        final RefreshTokenRecord refreshTokenRecord = mOauth2TokenCache.
                getFamilyRefreshTokenForHomeAccountId("26685724-1f8e-4b97-a0ca-1863e33b9fb1"); // different home account id
        assertNull(refreshTokenRecord);
    }
}
