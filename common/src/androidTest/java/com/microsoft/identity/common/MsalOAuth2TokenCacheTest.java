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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.AUTHORITY_TYPE;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CACHED_AT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.EXPIRES_ON;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.LOCAL_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.REALM;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SECRET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.TARGET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MsalOAuth2TokenCacheTest extends AndroidSecretKeyEnabledHelper {

    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mOauth2TokenCache;
    private ISharedPreferencesFileManager mSharedPreferencesFileManager;

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    IAccountCredentialAdapter<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mockCredentialAdapter;

    private AccountCredentialTestBundle defaultTestBundle;
    private IAccountCredentialCache accountCredentialCache;

    static class AccountCredentialTestBundle {

        final AccountRecord mGeneratedAccount;
        final AccessTokenRecord mGeneratedAccessToken;
        final RefreshTokenRecord mGeneratedRefreshToken;
        final IdTokenRecord mGeneratedIdToken;

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
                                    final String rtSecret,
                                    final String idTokenSecret,
                                    final String familyId) {
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
            mGeneratedAccessToken.setCredentialType(CredentialType.AccessToken.name());
            mGeneratedAccessToken.setClientId(clientId);

            mGeneratedRefreshToken = new RefreshTokenRecord();
            mGeneratedRefreshToken.setSecret(rtSecret);
            mGeneratedRefreshToken.setTarget(target);
            mGeneratedRefreshToken.setHomeAccountId(homeAccountId);
            mGeneratedRefreshToken.setEnvironment(environment);
            mGeneratedRefreshToken.setCredentialType(CredentialType.RefreshToken.name());
            mGeneratedRefreshToken.setClientId(clientId);
            mGeneratedRefreshToken.setFamilyId(familyId);

            mGeneratedIdToken = new IdTokenRecord();
            mGeneratedIdToken.setHomeAccountId(homeAccountId);
            mGeneratedIdToken.setEnvironment(environment);
            mGeneratedIdToken.setRealm(realm);
            mGeneratedIdToken.setCredentialType(CredentialType.IdToken.name());
            mGeneratedIdToken.setClientId(clientId);
            mGeneratedIdToken.setSecret(idTokenSecret);
            mGeneratedIdToken.setAuthority("https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/");
        }
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // Used by mocks
        defaultTestBundle = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
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
                SECRET,
                MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                null
        );

        // Mocks
        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(defaultTestBundle.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(defaultTestBundle.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(defaultTestBundle.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(defaultTestBundle.mGeneratedIdToken);

        // Context and related init
        final Context context = InstrumentationRegistry.getTargetContext();
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                "test_prefs",
                new StorageHelper(context)
        );

        final ICacheKeyValueDelegate keyValueDelegate = new CacheKeyValueDelegate();

        accountCredentialCache = new SharedPreferencesAccountCredentialCache(
                keyValueDelegate,
                mSharedPreferencesFileManager
        );

        mOauth2TokenCache = new MsalOAuth2TokenCache<>(
                context,
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
        assertEquals(defaultTestBundle.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        for (final Credential credential : credentials) {
            if (credential.getCredentialType().equalsIgnoreCase(CredentialType.AccessToken.name())) {
                ats.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                rts.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.IdToken.name())) {
                ids.add(credential);
            } else {
                fail();
            }
        }

        assertEquals(defaultTestBundle.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundle.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundle.mGeneratedIdToken, ids.get(0));
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
        accessTokenToClear.setCredentialType(CredentialType.AccessToken.name());
        accessTokenToClear.setClientId(CLIENT_ID);
        accessTokenToClear.setTarget(TARGET);

        // Save this dummy AT
        accountCredentialCache.saveCredential(accessTokenToClear);

        // Set the wider target on the new AT to write...
        defaultTestBundle.mGeneratedAccessToken.setTarget(extendedScopes);

        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(defaultTestBundle.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(3, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();
        final List<Credential> ids = new ArrayList<>();

        for (final Credential credential : credentials) {
            if (credential.getCredentialType().equalsIgnoreCase(CredentialType.AccessToken.name())) {
                ats.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                rts.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.IdToken.name())) {
                ids.add(credential);
            } else {
                fail();
            }
        }

        assertEquals(defaultTestBundle.mGeneratedAccessToken, ats.get(0));
        assertEquals(defaultTestBundle.mGeneratedRefreshToken, rts.get(0));
        assertEquals(defaultTestBundle.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void saveAccountDirect() {
        mOauth2TokenCache.save(
                defaultTestBundle.mGeneratedAccount,
                defaultTestBundle.mGeneratedIdToken
        );

        final AccountRecord account = mOauth2TokenCache.getAccount(
                ENVIRONMENT,
                CLIENT_ID,
                HOME_ACCOUNT_ID,
                REALM
        );

        final ICacheRecord cacheRecord = mOauth2TokenCache.load(
                CLIENT_ID,
                TARGET,
                account
        );

        assertNotNull(cacheRecord);
        assertNotNull(cacheRecord.getAccount());
        assertNotNull(cacheRecord.getIdToken());

        assertNull(cacheRecord.getAccessToken());
        assertNull(cacheRecord.getRefreshToken());

        assertEquals(
                defaultTestBundle.mGeneratedAccount,
                cacheRecord.getAccount()
        );

        assertEquals(
                defaultTestBundle.mGeneratedIdToken,
                cacheRecord.getIdToken()
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

        final AccountRecord account = mOauth2TokenCache.getAccount(
                ENVIRONMENT,
                CLIENT_ID,
                HOME_ACCOUNT_ID,
                REALM
        );

        assertNotNull(account);
        assertEquals(MicrosoftAccount.AUTHORITY_TYPE_V1_V2, account.getAuthorityType());
        assertEquals(LOCAL_ACCOUNT_ID, account.getLocalAccountId());
        assertEquals(USERNAME, account.getUsername());
        assertEquals(HOME_ACCOUNT_ID, account.getHomeAccountId());
        assertEquals(ENVIRONMENT, account.getEnvironment());
        assertEquals(REALM, account.getRealm());
    }

    @Test
    public void getAccountDisambiguate() throws ClientException {
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
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
                            SECRET,
                            MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                            null
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            when(
                    mockCredentialAdapter.createAccount(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccount);

            when(
                    mockCredentialAdapter.createAccessToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccessToken);

            when(
                    mockCredentialAdapter.createRefreshToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedRefreshToken);

            when(
                    mockCredentialAdapter.createIdToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedIdToken);

            mOauth2TokenCache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );
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
        final AccountRecord account = mOauth2TokenCache.getAccountWithLocalAccountId(
                ENVIRONMENT,
                CLIENT_ID,
                LOCAL_ACCOUNT_ID
        );

        assertNotNull(account);
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
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
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
                            SECRET,
                            MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                            null
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            when(
                    mockCredentialAdapter.createAccount(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccount);

            when(
                    mockCredentialAdapter.createAccessToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccessToken);

            when(
                    mockCredentialAdapter.createRefreshToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedRefreshToken);

            when(
                    mockCredentialAdapter.createIdToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedIdToken);

            mOauth2TokenCache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );
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
        final int iterations = 10;
        final List<AccountCredentialTestBundle> testBundles = new ArrayList<>();

        final String randomHomeAccountId = UUID.randomUUID().toString();

        for (int ii = 0; ii < iterations; ii++) {
            testBundles.add(
                    new AccountCredentialTestBundle(
                            MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
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
                            SECRET,
                            MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                            null
                    )
            );
        }

        for (int i = 0; i < iterations; i++) {
            when(
                    mockCredentialAdapter.createAccount(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccount);

            when(
                    mockCredentialAdapter.createAccessToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedAccessToken);

            when(
                    mockCredentialAdapter.createRefreshToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedRefreshToken);

            when(
                    mockCredentialAdapter.createIdToken(
                            mockStrategy,
                            mockRequest,
                            mockResponse
                    )
            ).thenReturn(testBundles.get(i).mGeneratedIdToken);

            mOauth2TokenCache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );
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

        assertEquals(defaultTestBundle.mGeneratedAccount, result.getAccount());
        assertEquals(defaultTestBundle.mGeneratedAccessToken, result.getAccessToken());
        assertEquals(defaultTestBundle.mGeneratedRefreshToken, result.getRefreshToken());
        assertEquals(defaultTestBundle.mGeneratedIdToken, result.getIdToken());

        final ICacheRecord secondaryLoad = mOauth2TokenCache.load(
                CLIENT_ID,
                TARGET,
                defaultTestBundle.mGeneratedAccount
        );

        assertEquals(result, secondaryLoad);
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
                TARGET,
                defaultTestBundle.mGeneratedAccount
        );

        assertEquals(defaultTestBundle.mGeneratedAccount, secondaryLoad.getAccount());
        assertNull(secondaryLoad.getAccessToken());
        assertEquals(defaultTestBundle.mGeneratedRefreshToken, secondaryLoad.getRefreshToken());
        assertEquals(defaultTestBundle.mGeneratedIdToken, secondaryLoad.getIdToken());
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
                TARGET,
                defaultTestBundle.mGeneratedAccount
        );

        assertEquals(defaultTestBundle.mGeneratedAccount, secondaryLoad.getAccount());
        assertEquals(defaultTestBundle.mGeneratedAccessToken, secondaryLoad.getAccessToken());
        assertNull(secondaryLoad.getRefreshToken());
        assertEquals(defaultTestBundle.mGeneratedIdToken, secondaryLoad.getIdToken());
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
                TARGET,
                defaultTestBundle.mGeneratedAccount
        );

        assertEquals(defaultTestBundle.mGeneratedAccount, secondaryLoad.getAccount());
        assertEquals(defaultTestBundle.mGeneratedAccessToken, secondaryLoad.getAccessToken());
        assertEquals(defaultTestBundle.mGeneratedRefreshToken, secondaryLoad.getRefreshToken());
        assertNull(secondaryLoad.getIdToken());
    }

    @Test
    public void testRetrieveFrt() throws ClientException {
        final String randomHomeAccountId = UUID.randomUUID().toString();

        final AccountCredentialTestBundle frtTestBundle = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
                UUID.randomUUID().toString(),
                "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId,
                ENVIRONMENT,
                UUID.randomUUID().toString(),
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                SECRET,
                MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                "1"
        );

        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedIdToken);

        // Save the family token data
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final ICacheRecord familyCacheRecord = mOauth2TokenCache.loadByFamilyId(
                null,
                null,
                frtTestBundle.mGeneratedAccount
        );

        assertNotNull(familyCacheRecord);
        assertNotNull(familyCacheRecord.getAccount());
        assertNotNull(familyCacheRecord.getRefreshToken());
        assertNull(familyCacheRecord.getIdToken());
        assertNull(familyCacheRecord.getAccessToken());

        final ICacheRecord familyCacheRecordWithClientId = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID,
                null,
                frtTestBundle.mGeneratedAccount
        );

        assertNotNull(familyCacheRecordWithClientId);
        assertNotNull(familyCacheRecordWithClientId.getAccount());
        assertNotNull(familyCacheRecordWithClientId.getRefreshToken());
        assertNotNull(familyCacheRecordWithClientId.getIdToken());
        assertNotNull(familyCacheRecordWithClientId.getAccessToken());

        final ICacheRecord familyCacheRecordWithClientIdButNonMatchingTarget =
                mOauth2TokenCache.loadByFamilyId(
                        CLIENT_ID,
                        "foo",
                        frtTestBundle.mGeneratedAccount
                );

        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget);
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getAccount());
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getRefreshToken());
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getIdToken());
        assertNull(familyCacheRecordWithClientIdButNonMatchingTarget.getAccessToken());

        final ICacheRecord wrongClientIdResult =
                mOauth2TokenCache.loadByFamilyId(
                        "12345",
                        "foo",
                        frtTestBundle.mGeneratedAccount
                );

        assertNotNull(wrongClientIdResult);
        assertNotNull(wrongClientIdResult.getAccount());
        assertNotNull(wrongClientIdResult.getRefreshToken());
        assertNull(wrongClientIdResult.getIdToken());
        assertNull(wrongClientIdResult.getAccessToken());
    }

    @Test
    public void testOnlyOneFrtMayExistAcrossClientsForAccount() throws ClientException {
        // Save an FRT
        final String randomHomeAccountId = UUID.randomUUID().toString();
        final String localAccountId = UUID.randomUUID().toString();
        final String realm = UUID.randomUUID().toString();

        final AccountCredentialTestBundle frtTestBundle = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
                localAccountId,
                "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId,
                ENVIRONMENT,
                realm,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                SECRET,
                MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                "1"
        );

        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle.mGeneratedIdToken);

        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        // Save another FRT, this time with a different client id
        final AccountCredentialTestBundle frtTestBundle2 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_V1_V2,
                localAccountId,
                "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId,
                ENVIRONMENT,
                realm,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID + "2",
                SECRET,
                MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                "1"
        );

        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle2.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle2.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle2.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundle2.mGeneratedIdToken);

        // Save the family token data
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        // Test only one FRT exists and it is the second one saved...
        final ICacheRecord cacheRecord = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID,
                null,
                frtTestBundle2.mGeneratedAccount
        );

        assertNotNull(cacheRecord);
        assertNotNull(cacheRecord.getRefreshToken());
        assertNotNull(cacheRecord.getAccessToken());
        assertNotNull(cacheRecord.getIdToken());
        assertEquals(
                CLIENT_ID + "2",
                cacheRecord.getRefreshToken().getClientId()
        );

        // Check querying for the FRT in the second app yields the same FRT
        final ICacheRecord cacheRecord2 = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID + "2",
                null,
                frtTestBundle2.mGeneratedAccount
        );

        assertNotNull(cacheRecord2);
        assertNotNull(cacheRecord2.getRefreshToken());
        assertNotNull(cacheRecord2.getAccessToken());
        assertNotNull(cacheRecord2.getIdToken());
        assertEquals(
                CLIENT_ID + "2",
                cacheRecord2.getRefreshToken().getClientId()
        );

        // Test querying with a different account yields nothing at all....

        final AccountRecord randomAcct = new AccountRecord();
        randomAcct.setAuthorityType(AUTHORITY_TYPE);
        randomAcct.setLocalAccountId(UUID.randomUUID().toString());
        randomAcct.setUsername("foo@bar.com");
        randomAcct.setHomeAccountId(UUID.randomUUID().toString());
        randomAcct.setEnvironment(ENVIRONMENT);
        randomAcct.setRealm(REALM);

        final ICacheRecord cacheRecord3 = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID + "2",
                null,
                randomAcct
        );

        assertNotNull(cacheRecord3);
        assertNotNull(cacheRecord3.getAccount());
        assertNull(cacheRecord3.getRefreshToken());
        assertNull(cacheRecord3.getAccessToken());
        assertNull(cacheRecord3.getIdToken());
    }

}
