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
import com.microsoft.identity.common.internal.cache.AccountDeletionRecord;
import com.microsoft.identity.common.internal.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.FociOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.getBrokerUidSequesteredFilename;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BrokerOAuth2TokenCacheTest extends AndroidSecretKeyEnabledHelper {

    private static final int TEST_APP_UID = 1337;

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    IAccountCredentialAdapter mMockCredentialAdapter;

    private FociOAuth2TokenCache mFociCache;
    private IAccountCredentialCache mFociCredentialCache;

    private MsalOAuth2TokenCache mAppUidCache;
    private IAccountCredentialCache mAppUidCredentialCache;

    private List<MsalOAuth2TokenCache> mOtherAppTokenCaches;
    private List<IAccountCredentialCache> mOtherAppCredentialCaches;

    private BrokerOAuth2TokenCache mBrokerOAuth2TokenCache;

    private MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultFociTestBundle;
    private MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultAppUidTestBundle;
    private List<MsalOAuth2TokenCacheTest.AccountCredentialTestBundle> mOtherCacheTestBundles;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Init Mockito mocks
        MockitoAnnotations.initMocks(this);

        // Our test context
        final Context context = InstrumentationRegistry.getTargetContext();

        // Test Configs for caches...
        initFociCache(context);
        initAppUidCache(context);
        initOtherCaches(context);

        mBrokerOAuth2TokenCache = new BrokerOAuth2TokenCache(
                context,
                mFociCache,
                mAppUidCache,
                mOtherAppTokenCaches
        );

        mDefaultFociTestBundle = new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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
                "1"
        );

        mDefaultAppUidTestBundle = new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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

        mOtherCacheTestBundles = new ArrayList<>();

        for (int ii = 0; ii < mOtherAppTokenCaches.size(); ii++) {
            mOtherCacheTestBundles.add(
                    new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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
                            UUID.randomUUID().toString(),
                            SECRET,
                            MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS,
                            null
                    )
            );
        }
    }

    @After
    public void tearDown() {
        mAppUidCredentialCache.clearAll();
        mFociCredentialCache.clearAll();
        for (final IAccountCredentialCache cache : mOtherAppCredentialCaches) {
            cache.clearAll();
        }
    }

    private void initOtherCaches(final Context context) {
        final int[] testAppUids = new int[]{
                1338,
                1339,
                1340,
                1341
        };

        final List<ISharedPreferencesFileManager> fileManagers = getAppUidFileManagers(
                context,
                testAppUids
        );

        mOtherAppCredentialCaches = getAccountCredentialCaches(
                fileManagers
        );

        mOtherAppTokenCaches = new ArrayList<>();

        for (final IAccountCredentialCache cache : mOtherAppCredentialCaches) {
            mOtherAppTokenCaches.add(
                    getTokenCache(
                            context,
                            cache,
                            false
                    )
            );
        }
    }

    private List<IAccountCredentialCache> getAccountCredentialCaches(final List<ISharedPreferencesFileManager> fileManagers) {
        final List<IAccountCredentialCache> accountCredentialCaches = new ArrayList<>();

        for (final ISharedPreferencesFileManager fileManager : fileManagers) {
            accountCredentialCaches.add(
                    getAccountCredentialCache(fileManager)
            );
        }

        return accountCredentialCaches;
    }

    private List<ISharedPreferencesFileManager> getAppUidFileManagers(final Context context,
                                                                      final int[] testAppUids) {
        final List<ISharedPreferencesFileManager> fileManagers = new ArrayList<>();

        for (final int currentAppUid : testAppUids) {
            fileManagers.add(
                    getAppUidFileManager(
                            context,
                            currentAppUid
                    )
            );
        }

        return fileManagers;
    }

    private ISharedPreferencesFileManager getAppUidFileManager(final Context context,
                                                               final int appUid) {
        return new SharedPreferencesFileManager(
                context,
                getBrokerUidSequesteredFilename(appUid),
                new StorageHelper(context)
        );
    }

    private ISharedPreferencesFileManager getFociFileManager(final Context context) {
        return new SharedPreferencesFileManager(
                context,
                BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(context)
        );
    }

    private SharedPreferencesAccountCredentialCache getAccountCredentialCache(
            final ISharedPreferencesFileManager fm) {
        return new SharedPreferencesAccountCredentialCache(
                new CacheKeyValueDelegate(),
                fm
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends MsalOAuth2TokenCache> T getTokenCache(final Context context,
                                                             final IAccountCredentialCache cache,
                                                             boolean isFoci) {
        return (T) (isFoci ?
                new FociOAuth2TokenCache<>(
                        context,
                        cache,
                        mMockCredentialAdapter
                ) :
                new MsalOAuth2TokenCache(
                        context,
                        cache,
                        mMockCredentialAdapter
                )
        );
    }


    private void initAppUidCache(final Context context) {
        final ISharedPreferencesFileManager appUidCacheFileManager = getAppUidFileManager(
                context,
                TEST_APP_UID
        );

        mAppUidCredentialCache = getAccountCredentialCache(appUidCacheFileManager);

        mAppUidCache = getTokenCache(context, mAppUidCredentialCache, false);
    }

    private void initFociCache(final Context context) {
        final ISharedPreferencesFileManager fociCacheFileManager = getFociFileManager(context);

        mFociCredentialCache = getAccountCredentialCache(fociCacheFileManager);

        mFociCache = getTokenCache(context, mFociCredentialCache, true);
    }

    @SuppressWarnings("unchecked")
    private void configureMocks(final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle) {
        when(
                mMockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedAccount);

        when(
                mMockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedAccessToken);

        when(
                mMockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedRefreshToken);

        when(
                mMockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(testBundle.mGeneratedIdToken);
    }

    private void configureMocksForFoci() {
        configureMocks(mDefaultFociTestBundle);
        when(mockResponse.getFamilyId()).thenReturn("1");
    }

    private void configureMocksForAppUid() {
        configureMocks(mDefaultAppUidTestBundle);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanSaveIntoAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = mAppUidCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(mDefaultAppUidTestBundle.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = mAppUidCredentialCache.getCredentials();
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

        assertEquals(mDefaultAppUidTestBundle.mGeneratedAccessToken, ats.get(0));
        assertEquals(mDefaultAppUidTestBundle.mGeneratedRefreshToken, rts.get(0));
        assertEquals(mDefaultAppUidTestBundle.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void testCanSaveIntoFociCache() throws ClientException {
        configureMocksForFoci();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = mFociCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(mDefaultFociTestBundle.mGeneratedAccount, accounts.get(0));

        final List<Credential> credentials = mFociCredentialCache.getCredentials();
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

        assertEquals(mDefaultFociTestBundle.mGeneratedAccessToken, ats.get(0));
        assertEquals(mDefaultFociTestBundle.mGeneratedRefreshToken, rts.get(0));
        assertEquals(mDefaultFociTestBundle.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void testCacheMiss() {
        final ICacheRecord cacheRecord = mBrokerOAuth2TokenCache.load(
                CLIENT_ID,
                TARGET,
                mDefaultAppUidTestBundle.mGeneratedAccount
        );

        assertNotNull(cacheRecord);
        assertNotNull(cacheRecord.getAccount());
        assertNull(cacheRecord.getAccessToken());
        assertNull(cacheRecord.getRefreshToken());
        assertNull(cacheRecord.getIdToken());
    }

    @Test
    public void testRemoveCredentialAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final ICacheRecord cacheRecord = mBrokerOAuth2TokenCache.load(
                CLIENT_ID,
                TARGET,
                mDefaultAppUidTestBundle.mGeneratedAccount
        );

        assertTrue(
                mBrokerOAuth2TokenCache.removeCredential(
                        mDefaultAppUidTestBundle.mGeneratedAccessToken
                )
        );
    }

    @Test
    public void testRemoveCredentialFociCache() throws ClientException {
        configureMocksForFoci();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final ICacheRecord cacheRecord = mBrokerOAuth2TokenCache.load(
                CLIENT_ID,
                TARGET,
                mDefaultFociTestBundle.mGeneratedAccount
        );

        assertTrue(
                mBrokerOAuth2TokenCache.removeCredential(
                        mDefaultFociTestBundle.mGeneratedAccessToken
                )
        );
    }

    @Test
    public void testRemoveCredentialMiss() {
        assertFalse(
                mBrokerOAuth2TokenCache.removeCredential(
                        mDefaultFociTestBundle.mGeneratedAccessToken
                )
        );
    }

    @Test
    public void testGetAccountAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertNotNull(
                mBrokerOAuth2TokenCache.getAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                )
        );

        assertNull(
                mFociCache.getAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                )
        );
    }

    @Test
    public void testGetAccountFociCache() throws ClientException {
        configureMocksForFoci();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        assertNotNull(
                mBrokerOAuth2TokenCache.getAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                )
        );

        assertNull(
                mAppUidCache.getAccount(
                        ENVIRONMENT,
                        CLIENT_ID,
                        HOME_ACCOUNT_ID,
                        REALM
                )
        );
    }

    @Test
    public void testGetAccountWithLocalAccountIdAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final AccountRecord account = mBrokerOAuth2TokenCache.getAccountWithLocalAccountId(
                ENVIRONMENT,
                CLIENT_ID,
                LOCAL_ACCOUNT_ID
        );

        assertNotNull(account);
    }

    @Test
    public void testGetAccountWithLocalAcocuntIdFociCache() throws ClientException {
        configureMocksForFoci();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final AccountRecord account = mBrokerOAuth2TokenCache.getAccountWithLocalAccountId(
                ENVIRONMENT,
                CLIENT_ID,
                LOCAL_ACCOUNT_ID
        );

        assertNotNull(account);
    }

    @Test
    public void testRemoveAccountFromDevice() throws ClientException {
        // Load up the 'other caches' which a bunch of test credentials, see if we can get them out...
        int ii = 0;
        for (final OAuth2TokenCache cache : mOtherAppTokenCaches) {
            configureMocks(mOtherCacheTestBundles.get(ii++));

            cache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );
        }

        final List<String> clientIds = new ArrayList<>();

        for (final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle : mOtherCacheTestBundles) {
            clientIds.add(
                    testBundle.mGeneratedRefreshToken.getClientId()
            );
        }

        final List<AccountRecord> xAppAccounts = mBrokerOAuth2TokenCache.getAccounts();

        // Deleting one of these AccountRecords should remove all of them...
        final AccountDeletionRecord deletionRecord = mBrokerOAuth2TokenCache.removeAccountFromDevice(
                xAppAccounts.get(0)
        );

        assertEquals(xAppAccounts.size(), deletionRecord.size());
        assertEquals(0, mBrokerOAuth2TokenCache.getAccounts().size());
    }

    @Test
    public void testGetAccountsAdal() throws ClientException {
        // Load up the 'other caches' which a bunch of test credentials, see if we can get them out...
        int ii = 0;
        for (final OAuth2TokenCache cache : mOtherAppTokenCaches) {
            configureMocks(mOtherCacheTestBundles.get(ii++));

            cache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );
        }

        final List<String> clientIds = new ArrayList<>();

        for (final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle : mOtherCacheTestBundles) {
            clientIds.add(
                    testBundle.mGeneratedRefreshToken.getClientId()
            );
        }

        final List<AccountRecord> xAppAccounts = new ArrayList<>();

        for (final String clientId : clientIds) {
            xAppAccounts.addAll(
                    mBrokerOAuth2TokenCache.getAccounts(
                            ENVIRONMENT,
                            clientId
                    )
            );
        }

        assertEquals(
                clientIds.size(),
                xAppAccounts.size()
        );

        final List<AccountRecord> xAppAccountsNoParam = new ArrayList<>(
                mBrokerOAuth2TokenCache.getAccounts()
        );

        assertEquals(xAppAccounts.size(), xAppAccountsNoParam.size());
    }

    @Test
    public void testGetAccountsMsal() {
        // TODO
    }

}
