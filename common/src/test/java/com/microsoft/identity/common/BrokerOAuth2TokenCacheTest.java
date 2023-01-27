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
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.BEARER_AUTHENTICATION_SCHEME;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CACHED_AT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.EXPIRES_ON;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.LOCAL_ACCOUNT_ID;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
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
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.shadows.ShadowAndroidSdkStorageEncryptionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private Context mContext;
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

        mContext = ApplicationProvider.getApplicationContext();
        mPlatformComponents = AndroidPlatformComponentsFactory.createFromContext(mContext);

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
        return components.getEncryptedNameValueStore(
                getBrokerUidSequesteredFilename(appUid),
                components.getStorageEncryptionManager(),
                String.class);
    }

    private INameValueStorage<String> getFociFileManager(final IPlatformComponents components) {
        return components.getEncryptedNameValueStore(
                BROKER_FOCI_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                components.getStorageEncryptionManager(),
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
                    AndroidPlatformComponentsFactory.createFromContext(mContext),
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
                    AndroidPlatformComponentsFactory.createFromContext(mContext),
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
    public void testWPJSaveNonFoci_deprecated() throws ClientException {
        final ICacheRecord saveResult = mBrokerOAuth2TokenCache.save(
                mDefaultAppUidTestBundle.mGeneratedAccount,
                mDefaultAppUidTestBundle.mGeneratedIdToken,
                mDefaultAppUidTestBundle.mGeneratedAccessToken,
                null
        );

        assertNotNull(saveResult);
        assertNotNull(saveResult.getAccount());
        assertNotNull(saveResult.getIdToken());
        assertNotNull(saveResult.getAccessToken());
        assertNull(saveResult.getRefreshToken());

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

        final ICacheRecord retrievedResult = mBrokerOAuth2TokenCache.load(
                mDefaultAppUidTestBundle.mGeneratedIdToken.getClientId(),
                mDefaultAppUidTestBundle.mGeneratedAccessToken.getTarget(),
                mDefaultAppUidTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(retrievedResult);
        assertNotNull(retrievedResult.getAccount());
        assertNotNull(retrievedResult.getIdToken());
        assertNotNull(retrievedResult.getAccessToken());
        assertNull(retrievedResult.getRefreshToken());

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
    public void testWPJSaveFoci_deprecated() throws ClientException {
        final ICacheRecord saveResult = mBrokerOAuth2TokenCache.save(
                mDefaultFociTestBundle.mGeneratedAccount,
                mDefaultFociTestBundle.mGeneratedIdToken,
                mDefaultFociTestBundle.mGeneratedAccessToken,
                "1"
        );

        assertNotNull(saveResult);
        assertNotNull(saveResult.getAccount());
        assertNotNull(saveResult.getIdToken());
        assertNotNull(saveResult.getAccessToken());
        assertNull(saveResult.getRefreshToken());

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


        final ICacheRecord retrievedResult = mBrokerOAuth2TokenCache.load(
                mDefaultFociTestBundle.mGeneratedIdToken.getClientId(),
                mDefaultFociTestBundle.mGeneratedAccessToken.getTarget(),
                mDefaultFociTestBundle.mGeneratedAccount,
                BEARER_AUTHENTICATION_SCHEME
        );

        assertNotNull(retrievedResult);
        assertNotNull(retrievedResult.getAccount());
        assertNotNull(retrievedResult.getIdToken());
        assertNotNull(retrievedResult.getAccessToken());
        assertNull(retrievedResult.getRefreshToken());

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
}
