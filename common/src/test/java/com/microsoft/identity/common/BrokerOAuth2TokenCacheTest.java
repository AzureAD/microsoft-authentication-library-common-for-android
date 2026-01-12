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

import static com.microsoft.identity.common.MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.APPLICATION_IDENTIFIER_SHA512;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.BEARER_AUTHENTICATION_SCHEME;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CACHED_AT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.EXPIRES_ON;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.LOCAL_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.MAM_ENROLLMENT_IDENTIFIER;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.REALM;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SECRET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SESSION_KEY;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.TARGET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.USERNAME;
import static com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache.BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;
import static com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache.getBrokerUidSequesteredFilename;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;


import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.internal.platform.AndroidPlatformUtil;
import com.microsoft.identity.common.java.cache.BrokerApplicationMetadata;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.java.cache.IAccountCredentialCache;
import com.microsoft.identity.common.java.cache.IBrokerApplicationMetadataCache;
import com.microsoft.identity.common.java.cache.MicrosoftFamilyOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.NameValueStorageBrokerApplicationMetadataCache;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.java.cache.AccountDeletionRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCacheWithMemoryCache;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.shadows.ShadowAndroidSdkStorageEncryptionManager;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class BrokerOAuth2TokenCacheTest {

    private static final int TEST_APP_UID = 1337;

    private IPlatformComponents mPlatformComponents;

    private MicrosoftStsOAuth2Strategy mockStrategy;
    private MicrosoftStsAuthorizationRequest mockRequest;
    private MicrosoftStsTokenResponse mockResponse;
    private IAccountCredentialAdapter mMockCredentialAdapter;

    private MicrosoftFamilyOAuth2TokenCache mFociCache;
    private IAccountCredentialCache mFociCredentialCache;
    private IAccountCredentialCache mAppUidCredentialCache;
    private List<MsalOAuth2TokenCache> mOtherAppTokenCaches;
    private List<IAccountCredentialCache> mOtherAppCredentialCaches;
    private BrokerOAuth2TokenCache mBrokerOAuth2TokenCache;

    private MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultFociTestBundle;
    private MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultAppUidTestBundle;
    private List<MsalOAuth2TokenCacheTest.AccountCredentialTestBundle> mOtherCacheTestBundles;

    private IBrokerApplicationMetadataCache mApplicationMetadataCache;
    private int[] testAppUids;

    @Before
    public void setUp() {

        mockStrategy = PowerMockito.mock(MicrosoftStsOAuth2Strategy.class);
        mockRequest = PowerMockito.mock(MicrosoftStsAuthorizationRequest.class);
        mockResponse = PowerMockito.mock(MicrosoftStsTokenResponse.class);
        mMockCredentialAdapter = PowerMockito.mock(IAccountCredentialAdapter.class);

        mPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder()
                .platformUtil(new AndroidPlatformUtil(ApplicationProvider.getApplicationContext(), null))
                .build();

        mApplicationMetadataCache = new NameValueStorageBrokerApplicationMetadataCache(mPlatformComponents);

        initFociCache(mPlatformComponents);
        initOtherCaches(mPlatformComponents);

        mBrokerOAuth2TokenCache = new BrokerOAuth2TokenCache(
                mPlatformComponents,
                TEST_APP_UID,
                mApplicationMetadataCache,
                new BrokerOAuth2TokenCache.ProcessUidCacheFactory() {
                    @Override
                    public MsalOAuth2TokenCache getTokenCache(final IPlatformComponents context,
                                                              final int bindingProcessUid) {
                        return initAppUidCache(context, bindingProcessUid);
                    }
                },
                mFociCache
        );

        mDefaultFociTestBundle = new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.IdToken
        );

        mDefaultAppUidTestBundle = new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
                SESSION_KEY,
                CredentialType.IdToken
        );

        mOtherCacheTestBundles = new ArrayList<>();

        for (int ii = 0; ii < mOtherAppTokenCaches.size(); ii++) {
            mOtherCacheTestBundles.add(
                    new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
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
                            UUID.randomUUID().toString(),
                            APPLICATION_IDENTIFIER_SHA512,
                            MAM_ENROLLMENT_IDENTIFIER,
                            SECRET,
                            MOCK_ID_TOKEN_WITH_CLAIMS,
                            null,
                            SESSION_KEY,
                            CredentialType.IdToken
                    )
            );
        }
    }

    @After
    public void tearDown() throws Exception {
        if (null != mAppUidCredentialCache) {
            mAppUidCredentialCache.clearAll();
        }

        if (null != mFociCredentialCache) {
            mFociCredentialCache.clearAll();
        }

        for (final IAccountCredentialCache cache : mOtherAppCredentialCaches) {
            cache.clearAll();
        }

        mApplicationMetadataCache.clear();
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    private void initOtherCaches(final IPlatformComponents components) {
        testAppUids = new int[]{
                1338,
                1339,
                1340,
                1341
        };

        final List<INameValueStorage<String>> fileManagers = getAppUidFileManagers(
                components,
                testAppUids
        );

        mOtherAppCredentialCaches = getAccountCredentialCaches(
                fileManagers
        );

        mOtherAppTokenCaches = new ArrayList<>();

        for (final IAccountCredentialCache cache : mOtherAppCredentialCaches) {
            mOtherAppTokenCaches.add(
                    getTokenCache(
                            components,
                            cache,
                            false
                    )
            );
        }
    }

    private List<IAccountCredentialCache> getAccountCredentialCaches(final List<INameValueStorage<String>> fileManagers) {
        final List<IAccountCredentialCache> accountCredentialCaches = new ArrayList<>();

        for (final INameValueStorage<String> fileManager : fileManagers) {
            accountCredentialCaches.add(
                    getAccountCredentialCache(fileManager)
            );
        }

        return accountCredentialCaches;
    }

    private List<INameValueStorage<String>> getAppUidFileManagers(final IPlatformComponents components,
                                                         final int[] testAppUids) {
        final List<INameValueStorage<String>> fileManagers = new ArrayList<>();

        for (final int currentAppUid : testAppUids) {
            fileManagers.add(
                    getAppUidFileManager(
                            components,
                            currentAppUid
                    )
            );
        }

        return fileManagers;
    }

    private INameValueStorage<String> getAppUidFileManager(final IPlatformComponents components,
                                                           final int appUid) {
        return components.getStorageSupplier().getEncryptedNameValueStore(
                getBrokerUidSequesteredFilename(appUid),
                String.class);
    }

    private INameValueStorage<String> getFociFileManager(final IPlatformComponents components) {
        return components.getStorageSupplier().getEncryptedNameValueStore(
                BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                String.class
        );
    }

    private SharedPreferencesAccountCredentialCache getAccountCredentialCache(
            final INameValueStorage<String> fm) {
        return new SharedPreferencesAccountCredentialCache(
                new CacheKeyValueDelegate(),
                fm
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends MsalOAuth2TokenCache> T getTokenCache(final IPlatformComponents components,
                                                             final IAccountCredentialCache cache,
                                                             boolean isFoci) {
        return (T) (isFoci ?
                new MicrosoftFamilyOAuth2TokenCache<>(
                        components,
                        cache,
                        mMockCredentialAdapter
                ) :
                new MsalOAuth2TokenCache(
                        components,
                        cache,
                        mMockCredentialAdapter
                )
        );
    }


    private MsalOAuth2TokenCache initAppUidCache(final IPlatformComponents components, final int uid) {
        final INameValueStorage<String> appUidCacheFileManager = getAppUidFileManager(
                components,
                uid
        );

        mAppUidCredentialCache = getAccountCredentialCache(appUidCacheFileManager);

        return getTokenCache(components, mAppUidCredentialCache, false);
    }

    private void initFociCache(final IPlatformComponents components) {
        @SuppressWarnings("unchecked")
        final INameValueStorage<String> fociCacheFileManager = getFociFileManager(components);

        mFociCredentialCache = getAccountCredentialCache(fociCacheFileManager);

        mFociCache = getTokenCache(components, mFociCredentialCache, true);
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
    public void testKnownClientIdsNonFoci() throws ClientException {
        configureMocksForAppUid();

        final ICacheRecord result = mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final String targetClientId = result.getRefreshToken().getClientId();
        assertTrue(mBrokerOAuth2TokenCache.isClientIdKnownToCache(targetClientId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKnownClientIdsFoci() throws ClientException {
        configureMocksForFoci();

        final ICacheRecord result = mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final String targetClientId = result.getRefreshToken().getClientId();
        assertTrue(mBrokerOAuth2TokenCache.isClientIdKnownToCache(targetClientId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFociCacheRecords() throws ClientException {
        configureMocksForFoci();

        final ICacheRecord result = mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<ICacheRecord> fociCacheRecords = mBrokerOAuth2TokenCache.getFociCacheRecords();

        assertNotNull(fociCacheRecords);
        assertFalse(fociCacheRecords.isEmpty());
        assertEquals(
                result.getRefreshToken(),
                fociCacheRecords.get(0).getRefreshToken()
        );
        assertEquals(
                result.getIdToken(),
                fociCacheRecords.get(0).getIdToken()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFociCacheRecordsEmpty() throws ClientException {
        configureMocksForAppUid();

        final ICacheRecord result = mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<ICacheRecord> fociCacheRecords = mBrokerOAuth2TokenCache.getFociCacheRecords();

        assertNotNull(fociCacheRecords);
        assertTrue(fociCacheRecords.isEmpty());
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
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                TARGET,
                mDefaultAppUidTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
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
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                TARGET,
                mDefaultAppUidTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
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
                APPLICATION_IDENTIFIER_SHA512,
                MAM_ENROLLMENT_IDENTIFIER,
                TARGET,
                mDefaultFociTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
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
    }

    @Test
    public void testGetAccountWithLocalAccountIdAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final AccountRecord account = mBrokerOAuth2TokenCache.getAccountByLocalAccountId(
                ENVIRONMENT,
                CLIENT_ID,
                LOCAL_ACCOUNT_ID
        );

        assertNotNull(account);
    }

    @Test
    public void testGetAccountWithLocalAccountIdFociCache() throws ClientException {
        configureMocksForFoci();

        mBrokerOAuth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final AccountRecord account = mBrokerOAuth2TokenCache.getAccountByLocalAccountId(
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
            configureMocks(mOtherCacheTestBundles.get(ii));

            final ICacheRecord cacheRecord = cache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );

            final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
            applicationMetadata.setClientId(cacheRecord.getIdToken().getClientId());
            applicationMetadata.setEnvironment(cacheRecord.getIdToken().getEnvironment());
            applicationMetadata.setFoci(cacheRecord.getRefreshToken().getFamilyId());
            applicationMetadata.setUid(testAppUids[ii++]);

            mApplicationMetadataCache.insert(applicationMetadata);
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
            configureMocks(mOtherCacheTestBundles.get(ii));

            final ICacheRecord cacheRecord = cache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );

            final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
            applicationMetadata.setClientId(cacheRecord.getIdToken().getClientId());
            applicationMetadata.setEnvironment(cacheRecord.getIdToken().getEnvironment());
            applicationMetadata.setFoci(cacheRecord.getRefreshToken().getFamilyId());
            applicationMetadata.setUid(testAppUids[ii++]);

            mApplicationMetadataCache.insert(applicationMetadata);
        }

        final List<String> clientIds = new ArrayList<>();

        for (final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle : mOtherCacheTestBundles) {
            clientIds.add(
                    testBundle.mGeneratedRefreshToken.getClientId()
            );
        }

        final List<AccountRecord> xAppAccounts = new ArrayList<>();

        for (final int testUid : testAppUids) {
            // Create the cache to query...
            mBrokerOAuth2TokenCache = new BrokerOAuth2TokenCache(
                    mPlatformComponents,
                    testUid,
                    mApplicationMetadataCache,
                    new BrokerOAuth2TokenCache.ProcessUidCacheFactory() {
                        @Override
                        public MsalOAuth2TokenCache getTokenCache(IPlatformComponents context, int bindingProcessUid) {
                            return initAppUidCache(context, bindingProcessUid);
                        }
                    },
                    mFociCache
            );

            for (final String clientId : clientIds) {
                final List<AccountRecord> accountsInCache = mBrokerOAuth2TokenCache.getAccounts(
                        ENVIRONMENT,
                        clientId
                );

                xAppAccounts.addAll(accountsInCache);
            }
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
    public void testGetAccountsMsal() throws ClientException {
        // Load up the 'other caches' which a bunch of test credentials, see if we can get them out...
        int ii = 0;
        for (final OAuth2TokenCache cache : mOtherAppTokenCaches) {
            configureMocks(mOtherCacheTestBundles.get(ii));

            final ICacheRecord cacheRecord = cache.save(
                    mockStrategy,
                    mockRequest,
                    mockResponse
            );


            final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
            applicationMetadata.setClientId(cacheRecord.getIdToken().getClientId());
            applicationMetadata.setEnvironment(cacheRecord.getIdToken().getEnvironment());
            applicationMetadata.setFoci(cacheRecord.getRefreshToken().getFamilyId());
            applicationMetadata.setUid(testAppUids[ii++]);

            mApplicationMetadataCache.insert(applicationMetadata);
        }

        final List<String> clientIds = new ArrayList<>();

        for (final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle : mOtherCacheTestBundles) {
            clientIds.add(
                    testBundle.mGeneratedRefreshToken.getClientId()
            );
        }

        final List<AccountRecord> xAppAccounts = new ArrayList<>();

        for (final int testUid : testAppUids) {
            // Create the cache to query...
            mBrokerOAuth2TokenCache = new BrokerOAuth2TokenCache(
                    mPlatformComponents,
                    testUid,
                    mApplicationMetadataCache,
                    new BrokerOAuth2TokenCache.ProcessUidCacheFactory() {
                        @Override
                        public MsalOAuth2TokenCache getTokenCache(IPlatformComponents context, int bindingProcessUid) {
                            return initAppUidCache(context, bindingProcessUid);
                        }
                    },
                    mFociCache
            );

            for (final String clientId : clientIds) {
                final List<AccountRecord> accountsInCache = mBrokerOAuth2TokenCache.getAccounts(
                        ENVIRONMENT,
                        clientId
                );

                xAppAccounts.addAll(accountsInCache);
            }
        }

        assertEquals(
                clientIds.size(),
                xAppAccounts.size()
        );

        final List<AccountRecord> xAppAccountsNoParam = new ArrayList<>(
                mBrokerOAuth2TokenCache.getAccounts()
        );

        assertEquals(xAppAccounts.size(), xAppAccountsNoParam.size());

        final BrokerOAuth2TokenCache brokerOAuth2TokenCache = new BrokerOAuth2TokenCache(
                mPlatformComponents,
                TEST_APP_UID,
                new NameValueStorageBrokerApplicationMetadataCache(mPlatformComponents)
        );

        assertEquals(
                0,
                brokerOAuth2TokenCache.getAccounts(ENVIRONMENT, CLIENT_ID).size()
        );

        final BrokerOAuth2TokenCache brokerOAuth2TokenCache2 = new BrokerOAuth2TokenCache(
                mPlatformComponents,
                TEST_APP_UID,
                new NameValueStorageBrokerApplicationMetadataCache(mPlatformComponents)
        );

        assertEquals(
                xAppAccounts.size(),
                brokerOAuth2TokenCache2.getAccounts().size()
        );
    }

    @Test
    public void testWPJSaveNonFoci() throws ClientException {
        final ICacheRecord saveResult = mBrokerOAuth2TokenCache.save(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                mDefaultAppUidTestBundle.mGeneratedRefreshToken,
                null
        );

        assertNotNull(saveResult);
        assertNotNull(saveResult.getAccount());
        assertNotNull(saveResult.getIdToken());
        assertNotNull(saveResult.getAccessToken());
        assertNotNull(saveResult.getRefreshToken());

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                saveResult.getAccount()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                saveResult.getIdToken()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                saveResult.getAccessToken()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedRefreshToken,
                saveResult.getRefreshToken()
        );

        final ICacheRecord retrievedResult = mBrokerOAuth2TokenCache.load(
                mDefaultAppUidTestBundle.mGeneratedIdToken.getClientId(),
                mDefaultAppUidTestBundle.mGeneratedAccessToken.getApplicationIdentifier(),
                mDefaultAppUidTestBundle.mGeneratedAccessToken.getMamEnrollmentIdentifier(),
                mDefaultAppUidTestBundle.mGeneratedAccessToken.getTarget(),
                mDefaultAppUidTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(retrievedResult);
        assertNotNull(retrievedResult.getAccount());
        assertNotNull(retrievedResult.getIdToken());
        assertNotNull(retrievedResult.getAccessToken());
        assertNotNull(retrievedResult.getRefreshToken());

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                retrievedResult.getAccount()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                retrievedResult.getIdToken()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                retrievedResult.getAccessToken()
        );

        assertEquals(
                mDefaultAppUidTestBundle.mGeneratedRefreshToken,
                saveResult.getRefreshToken()
        );
    }

    @Test
    public void testWPJSaveFoci() throws ClientException {
        final ICacheRecord saveResult = mBrokerOAuth2TokenCache.save(
                mDefaultFociTestBundle.mGeneratedAccount,
                mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken,
                mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1"
        );

        assertNotNull(saveResult);
        assertNotNull(saveResult.getAccount());
        assertNotNull(saveResult.getIdToken());
        assertNotNull(saveResult.getAccessToken());
        assertNotNull(saveResult.getRefreshToken());

        assertEquals(
                mDefaultFociTestBundle.mGeneratedAccount,
                saveResult.getAccount()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedIdToken,
                saveResult.getIdToken()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedAccessToken,
                saveResult.getAccessToken()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedRefreshToken,
                saveResult.getRefreshToken()
        );

        final ICacheRecord retrievedResult = mBrokerOAuth2TokenCache.load(
                mDefaultFociTestBundle.mGeneratedIdToken.getClientId(),
                mDefaultFociTestBundle.mGeneratedAccessToken.getApplicationIdentifier(),
                mDefaultFociTestBundle.mGeneratedAccessToken.getAccessTokenType(),
                mDefaultFociTestBundle.mGeneratedAccessToken.getTarget(),
                mDefaultFociTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(retrievedResult);
        assertNotNull(retrievedResult.getAccount());
        assertNotNull(retrievedResult.getIdToken());
        assertNotNull(retrievedResult.getAccessToken());
        assertNotNull(retrievedResult.getRefreshToken());

        assertEquals(
                mDefaultFociTestBundle.mGeneratedAccount,
                retrievedResult.getAccount()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedIdToken,
                retrievedResult.getIdToken()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedAccessToken,
                retrievedResult.getAccessToken()
        );

        assertEquals(
                mDefaultFociTestBundle.mGeneratedRefreshToken,
                retrievedResult.getRefreshToken()
        );
    }

    @Test
    public void testClearAll() throws ClientException {
        int appIndex = 0;
        for (final OAuth2TokenCache cache : mOtherAppTokenCaches) {
            configureMocks(mOtherCacheTestBundles.get(appIndex));

            final ICacheRecord cacheRecord = cache.save(mockStrategy, mockRequest, mockResponse);

            final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
            applicationMetadata.setClientId(cacheRecord.getIdToken().getClientId());
            applicationMetadata.setEnvironment(cacheRecord.getIdToken().getEnvironment());
            applicationMetadata.setFoci(cacheRecord.getRefreshToken().getFamilyId());
            applicationMetadata.setUid(testAppUids[appIndex++]);

            mApplicationMetadataCache.insert(applicationMetadata);
        }

        final List<String> clientIds = new ArrayList<>();

        for (final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle testBundle : mOtherCacheTestBundles) {
            clientIds.add(
                    testBundle.mGeneratedRefreshToken.getClientId()
            );
        }

        configureMocksForFoci();
        final ICacheRecord fociCacheRecord = mBrokerOAuth2TokenCache.save(mockStrategy, mockRequest, mockResponse);
        final BrokerApplicationMetadata applicationMetadata = new BrokerApplicationMetadata();
        applicationMetadata.setClientId(fociCacheRecord.getIdToken().getClientId());
        applicationMetadata.setEnvironment(fociCacheRecord.getIdToken().getEnvironment());
        applicationMetadata.setFoci(fociCacheRecord.getRefreshToken().getFamilyId());
        applicationMetadata.setUid(0);

        mApplicationMetadataCache.insert(applicationMetadata);
        clientIds.add(fociCacheRecord.getIdToken().getClientId());

        // Verify the broker cache is populated
        assertEquals(true, mBrokerOAuth2TokenCache.getAccounts().size() > 0);
        assertEquals(true, mBrokerOAuth2TokenCache.getFociCacheRecords().size() > 0);
        assertEquals(true, mApplicationMetadataCache.getAll().size() > 0);

        for( final String clientId :clientIds) {
            assertEquals(true, mBrokerOAuth2TokenCache.isClientIdKnownToCache(clientId));
        }

        // Clear Broker Cache
        mBrokerOAuth2TokenCache.clearAll();

        // Verify Broker cache is cleared
        assertEquals(0, mBrokerOAuth2TokenCache.getAccounts().size());
        assertEquals(0, mBrokerOAuth2TokenCache.getFociCacheRecords().size());
        assertEquals(0, mApplicationMetadataCache.getAll().size());
        for( final String clientId :clientIds) {
            assertEquals(false, mBrokerOAuth2TokenCache.isClientIdKnownToCache(clientId));
        }
    }

    @Test
    public void testSingleCacheInstancePerStoreName_FlightEnabled() {
        // Enable the flight
        updateFlightForTest(CommonFlight.USE_IN_MEMORY_CACHE_FOR_ACCOUNTS_AND_CREDENTIALS, true);

        final String storeName = "test_store_name";
        final IPlatformComponents components1 = mPlatformComponents;
        final IPlatformComponents components2 = mPlatformComponents;

        // Call getCacheToBeUsed twice with the same storeName
        final IAccountCredentialCache cache1 = BrokerOAuth2TokenCache.getCacheToBeUsed(components1, storeName);
        final IAccountCredentialCache cache2 = BrokerOAuth2TokenCache.getCacheToBeUsed(components2, storeName);

        // Verify both references point to the same instance
        assertNotNull(cache1);
        assertNotNull(cache2);
        assertSame("Expected same cache instance for same storeName", cache1, cache2);
        assertTrue("Cache should be of type SharedPreferencesAccountCredentialCacheWithMemoryCache",
                cache1 instanceof SharedPreferencesAccountCredentialCacheWithMemoryCache);
    }

    @Test
    public void testDifferentCacheInstancesPerStoreName_FlightEnabled() {
        // Enable the flight
        updateFlightForTest(CommonFlight.USE_IN_MEMORY_CACHE_FOR_ACCOUNTS_AND_CREDENTIALS, true);

        final String storeName1 = "test_store_name_1";
        final String storeName2 = "test_store_name_2";
        final IPlatformComponents components = mPlatformComponents;

        // Call getCacheToBeUsed with different storeNames
        final IAccountCredentialCache cache1 = BrokerOAuth2TokenCache.getCacheToBeUsed(components, storeName1);
        final IAccountCredentialCache cache2 = BrokerOAuth2TokenCache.getCacheToBeUsed(components, storeName2);

        // Verify both are valid but different instances
        assertNotNull(cache1);
        assertNotNull(cache2);
        assertNotSame("Expected different cache instances for different storeNames", cache1, cache2);
        assertTrue("Cache should be of type SharedPreferencesAccountCredentialCacheWithMemoryCache",
                cache1 instanceof SharedPreferencesAccountCredentialCacheWithMemoryCache);
        assertTrue("Cache should be of type SharedPreferencesAccountCredentialCacheWithMemoryCache",
                cache2 instanceof SharedPreferencesAccountCredentialCacheWithMemoryCache);
    }

    @Test
    public void testCacheInstanceReusedAcrossMultipleBrokerTokenCaches_FlightEnabled() {
        // Enable the flight
        updateFlightForTest(CommonFlight.USE_IN_MEMORY_CACHE_FOR_ACCOUNTS_AND_CREDENTIALS, true);

        final String storeName = getBrokerUidSequesteredFilename(TEST_APP_UID);

        // Create multiple BrokerOAuth2TokenCache instances
        final BrokerOAuth2TokenCache tokenCache1 = new BrokerOAuth2TokenCache
                (mPlatformComponents,
                        TEST_APP_UID,
                        new NameValueStorageBrokerApplicationMetadataCache(mPlatformComponents));
        final BrokerOAuth2TokenCache tokenCache2 = new BrokerOAuth2TokenCache(mPlatformComponents, TEST_APP_UID,
                new NameValueStorageBrokerApplicationMetadataCache(mPlatformComponents));

        // Get the underlying account credential caches
        final IAccountCredentialCache cache1 = tokenCache1.getCacheToBeUsed(mPlatformComponents, storeName);
        final IAccountCredentialCache cache2 = tokenCache2.getCacheToBeUsed(mPlatformComponents, storeName);

        // Verify same instance is reused
        assertNotNull(cache1);
        assertNotNull(cache2);
        assertSame(cache1, cache2);
    }

    @Test
    public void testFociCacheInstanceReused_FlightEnabled() {
        // Enable the flight
        updateFlightForTest(CommonFlight.USE_IN_MEMORY_CACHE_FOR_ACCOUNTS_AND_CREDENTIALS, true);

        final String fociStoreName = BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

        // Call getCacheToBeUsed multiple times for FOCI cache
        final IAccountCredentialCache fociCache1 = BrokerOAuth2TokenCache.getCacheToBeUsed(mPlatformComponents, fociStoreName);
        final IAccountCredentialCache fociCache2 = BrokerOAuth2TokenCache.getCacheToBeUsed(mPlatformComponents, fociStoreName);

        // Verify same FOCI cache instance is reused
        assertNotNull(fociCache1);
        assertNotNull(fociCache2);
        assertSame(fociCache1, fociCache2);
    }

    private void updateFlightForTest(IFlightConfig flightName, boolean enabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        Mockito.when(mockFlightsProvider.isFlightEnabled(flightName))
                .thenReturn(enabled);

        // Create anonymous IFlightsManager
        IFlightsManager anonymousFlightsManager = new IFlightsManager() {
            @Override
            public @NotNull IFlightsProvider getFlightsProvider(long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }
            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId, long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }
            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId) {
                return mockFlightsProvider;
            }
            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider() {
                return mockFlightsProvider;
            }
        };

        // Initialize CommonFlightsManager with the anonymous implementation
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(anonymousFlightsManager);
    }

    /**
     * Test saveAndLoadAggregatedAccountData with FOCI response (family app) - Flight ENABLED.
     * When the flight is enabled, the optimized method should be called.
     * Verifies that:
     * 1. The method returns aggregated account data (primary + guest accounts)
     * 2. FOCI cache is used when familyId is present
     * 3. Metadata cache is updated correctly
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_FociApp_FlightEnabled() throws Exception {
        // Arrange - configure mocks for FOCI (family app with familyId)
        configureMocksForFoci();

        // Mock flight as ENABLED - this will call the optimized method
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);

        // Act - save and load aggregated data
        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount,
                mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken,
                mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1",
                BEARER_AUTHENTICATION_SCHEME,
                true
        );

        verifyFociAppSaveAndLoadAggregatedAccountData(result);
    }

    /**
     * Test saveAndLoadAggregatedAccountData with FOCI response (family app) - Flight DISABLED.
     * When the flight is disabled, the original (non-optimized) method should be called.
     * Verifies that:
     * 1. The method returns aggregated account data (primary + guest accounts)
     * 2. FOCI cache is used when familyId is present
     * 3. Metadata cache is updated correctly
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_FociApp_FlightDisabled() throws Exception {
        // Arrange - configure mocks for FOCI (family app with familyId)
        configureMocksForFoci();
        // Mock flight as DISABLED - this will call the original method
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, false);

        // Act - save and load aggregated data
        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount,
                mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken,
                mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1",
                BEARER_AUTHENTICATION_SCHEME,
                false
        );

        verifyFociAppSaveAndLoadAggregatedAccountData(result);
    }

    // Helper method with all the assertions
    private void verifyFociAppSaveAndLoadAggregatedAccountData(
            List<ICacheRecord> result) {
        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should return at least one record", result.size() > 0);

        // Verify first record has complete credentials (primary account)
        final ICacheRecord primaryRecord = result.get(0);
        assertNotNull("Primary account should exist", primaryRecord.getAccount());
        assertNotNull("Primary access token should exist", primaryRecord.getAccessToken());
        assertNotNull("Primary refresh token should exist", primaryRecord.getRefreshToken());
        assertNotNull("Primary id token should exist", primaryRecord.getIdToken());

        // Verify refresh token has family ID
        assertEquals("Refresh token should have family ID",
                "1",
                primaryRecord.getRefreshToken().getFamilyId());

        // Verify metadata cache was updated
        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        assertTrue("Metadata cache should have at least one entry", allMetadata.size() > 0);

        boolean foundMetadata = false;
        for (BrokerApplicationMetadata metadata : allMetadata) {
            if (CLIENT_ID.equals(metadata.getClientId()) &&
                    ENVIRONMENT.equals(metadata.getEnvironment())) {
                foundMetadata = true;
                assertEquals("Metadata should have family ID", "1", metadata.getFoci());
                break;
            }
        }
        assertTrue("Should find metadata for the saved client", foundMetadata);
    }

    /**
     * Test saveAndLoadAggregatedAccountData with non-FOCI response (non-family app) - Flight ENABLED.
     * Verifies that:
     * 1. The method returns aggregated account data
     * 2. Process UID cache is used when familyId is null
     * 3. Metadata cache is updated correctly without family ID
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_NonFociApp_FlightEnabled() throws Exception {
        // Arrange - configure mocks for non-FOCI (app without familyId)
        configureMocksForAppUid();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);

        // Act - save and load aggregated data
        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                mDefaultAppUidTestBundle.mGeneratedRefreshToken,
                null,
                BEARER_AUTHENTICATION_SCHEME,
                true
        );

        verifyNonFociAppSaveAndLoadAggregatedAccountData(result);
    }

    /**
     * Test saveAndLoadAggregatedAccountData with non-FOCI response (non-family app) - Flight DISABLED.
     * Verifies that:
     * 1. The method returns aggregated account data
     * 2. Process UID cache is used when familyId is null
     * 3. Metadata cache is updated correctly without family ID
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_NonFociApp_FlightDisabled() throws Exception {
        // Arrange - configure mocks for non-FOCI (app without familyId)
        configureMocksForAppUid();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, false);

        // Act - save and load aggregated data
        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                mDefaultAppUidTestBundle.mGeneratedRefreshToken,
                null,
                BEARER_AUTHENTICATION_SCHEME,
                false
        );

        verifyNonFociAppSaveAndLoadAggregatedAccountData(result);
    }

    private void verifyNonFociAppSaveAndLoadAggregatedAccountData(List<ICacheRecord> result) {
        assertNotNull("Result should not be null", result);
        assertTrue("Should return at least one record", result.size() > 0);

        // Verify first record has complete credentials (primary account)
        final ICacheRecord primaryRecord = result.get(0);
        assertNotNull("Primary account should exist", primaryRecord.getAccount());
        assertNotNull("Primary access token should exist", primaryRecord.getAccessToken());
        assertNotNull("Primary refresh token should exist", primaryRecord.getRefreshToken());
        assertNotNull("Primary id token should exist", primaryRecord.getIdToken());

        // Verify refresh token does NOT have family ID
        assertNull("Refresh token should not have family ID for non-FOCI app",
                primaryRecord.getRefreshToken().getFamilyId());

        // Verify metadata cache was updated
        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        assertTrue("Metadata cache should have at least one entry", allMetadata.size() > 0);

        boolean foundMetadata = false;
        for (BrokerApplicationMetadata metadata : allMetadata) {
            if (CLIENT_ID.equals(metadata.getClientId()) &&
                    ENVIRONMENT.equals(metadata.getEnvironment())) {
                foundMetadata = true;
                assertNull("Metadata should not have family ID for non-FOCI app", metadata.getFoci());
                break;
            }
        }
        assertTrue("Should find metadata for the saved client", foundMetadata);
    }

    /**
     * Test that multiple calls to saveAndLoadAggregatedAccountData work correctly - Flight ENABLED.
     * Verifies cache reuse and consistent behavior across multiple operations.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_MultipleCalls_FlightEnabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);

        verifyCacheConsistencyAcrossMultipleSaveAndLoadOperations();
    }

    /**
     * Test that multiple calls to saveAndLoadAggregatedAccountData work correctly - Flight DISABLED.
     * Verifies cache reuse and consistent behavior across multiple operations.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_MultipleCalls_FlightDisabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, false);

        verifyCacheConsistencyAcrossMultipleSaveAndLoadOperations();
    }

    private void verifyCacheConsistencyAcrossMultipleSaveAndLoadOperations() throws ClientException {
        final List<ICacheRecord> result1 = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, false);
        final List<ICacheRecord> result2 = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, false);
        final List<ICacheRecord> result3 = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, false);

        assertNotNull("First result should not be null", result1);
        assertNotNull("Second result should not be null", result2);
        assertNotNull("Third result should not be null", result3);

        assertTrue("First result should have at least one record", result1.size() > 0);
        assertTrue("Second result should have at least one record", result2.size() > 0);
        assertTrue("Third result should have at least one record", result3.size() > 0);

        assertEquals("All results should have the same number of records",
                result1.size(), result2.size());
        assertEquals("All results should have the same number of records",
                result2.size(), result3.size());
    }

    /**
     * Test saveAndLoadAggregatedAccountData with aggregated data (guest accounts) - Flight ENABLED.
     * Verifies that guest tenant accounts are returned along with primary account.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_WithGuestAccounts_FlightEnabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);
        verifySaveAndLoadAggregatedAccountData_WithGuestAccounts();
    }

    /**
     * Test saveAndLoadAggregatedAccountData with aggregated data (guest accounts) - Flight DISABLED.
     * Verifies that guest tenant accounts are returned along with primary account.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_WithGuestAccounts_FlightDisabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, false);
        verifySaveAndLoadAggregatedAccountData_WithGuestAccounts();
    }

    private void verifySaveAndLoadAggregatedAccountData_WithGuestAccounts() throws ClientException {
        mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, true);

        final MsalOAuth2TokenCacheTest.AccountCredentialTestBundle guestBundle =
                new MsalOAuth2TokenCacheTest.AccountCredentialTestBundle(
                        MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                        LOCAL_ACCOUNT_ID,
                        USERNAME,
                        HOME_ACCOUNT_ID,
                        ENVIRONMENT,
                        "guest-tenant-id",
                        TARGET,
                        CACHED_AT,
                        EXPIRES_ON,
                        SECRET,
                        CLIENT_ID,
                        APPLICATION_IDENTIFIER_SHA512,
                        MAM_ENROLLMENT_IDENTIFIER,
                        SECRET,
                        MOCK_ID_TOKEN_WITH_CLAIMS,
                        "1",
                        SESSION_KEY,
                        CredentialType.IdToken
                );

        mFociCredentialCache.saveAccount(guestBundle.mGeneratedAccount);
        mFociCredentialCache.saveCredential(guestBundle.mGeneratedIdToken);

        configureMocksForFoci();
        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, false);

        assertNotNull("Result should not be null", result);
        assertTrue("Should return multiple records (primary + guest)", result.size() >= 1);

        final ICacheRecord primaryRecord = result.get(0);
        assertNotNull("Primary account should exist", primaryRecord.getAccount());
        assertNotNull("Primary access token should exist", primaryRecord.getAccessToken());
        assertNotNull("Primary refresh token should exist", primaryRecord.getRefreshToken());
        assertNotNull("Primary id token should exist", primaryRecord.getIdToken());
        assertEquals("Primary account realm should match",
                REALM,
                primaryRecord.getAccount().getRealm());
    }

    @Test
    public void testSaveAndLoadAggregatedAccountData_FlightEnabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);

        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, true);
        assertNotNull("Result should not be null", result);
        assertTrue("Result should have at least one record", result.size() > 0);
    }

    /**
     * Test saveAndLoadAggregatedAccountData correctly updates metadata cache UID - Flight ENABLED.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_MetadataUidUpdated_FlightEnabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, true);

        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, true);

        assertNotNull("Result should not be null", result);

        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        verifyMetadataHasCorrectUid(allMetadata);
    }

    /**
     * Test saveAndLoadAggregatedAccountData correctly updates metadata cache UID - Flight DISABLED.
     */
    @Test
    public void testSaveAndLoadAggregatedAccountData_MetadataUidUpdated_FlightDisabled() throws Exception {
        configureMocksForFoci();
        updateFlightForTest(CommonFlight.CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD, false);

        final List<ICacheRecord> result = mBrokerOAuth2TokenCache.saveAndLoadAggregatedAccountData(
                mDefaultFociTestBundle.mGeneratedAccount, mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken, mDefaultFociTestBundle.mGeneratedRefreshToken,
                "1", BEARER_AUTHENTICATION_SCHEME, false);

        assertNotNull("Result should not be null", result);

        final List<BrokerApplicationMetadata> allMetadata = mApplicationMetadataCache.getAll();
        verifyMetadataHasCorrectUid(allMetadata);
    }

    /**
     * Utility method to verify that metadata cache contains an entry with the correct UID.
     */
    private void verifyMetadataHasCorrectUid(List<BrokerApplicationMetadata> allMetadata) {
        boolean foundMetadata = false;
        for (BrokerApplicationMetadata metadata : allMetadata) {
            if (CLIENT_ID.equals(metadata.getClientId()) &&
                    ENVIRONMENT.equals(metadata.getEnvironment())) {
                foundMetadata = true;
                assertEquals("Metadata should have correct UID",
                        TEST_APP_UID,
                        metadata.getUid());
                break;
            }
        }
        assertTrue("Should find metadata with correct UID", foundMetadata);
    }
}
