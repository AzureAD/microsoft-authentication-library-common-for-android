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
import com.microsoft.identity.common.internal.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

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

    OAuth2TokenCache mFociCache;
    IAccountCredentialCache mFociCredentialCache;

    OAuth2TokenCache mAppUidCache;
    IAccountCredentialCache mAppUidCredentialCache;

    List<OAuth2TokenCache> mOtherAppTokenCaches;
    List<IAccountCredentialCache> mOtherAppCredentialCaches;

    BrokerOAuth2TokenCache mBrokerOAuth2TokenCache;

    MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultFociTestBundle;
    MsalOAuth2TokenCacheTest.AccountCredentialTestBundle mDefaultAppUidTestBundle;

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
                mBrokerOAuth2TokenCache,
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
                            cache
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
    private MsalOAuth2TokenCache getTokenCache(final Context context,
                                               final IAccountCredentialCache cache) {
        return new MsalOAuth2TokenCache(
                context,
                cache,
                mMockCredentialAdapter
        );
    }


    private void initAppUidCache(final Context context) {
        final ISharedPreferencesFileManager appUidCacheFileManager = getAppUidFileManager(
                context,
                TEST_APP_UID
        );

        mAppUidCredentialCache = getAccountCredentialCache(appUidCacheFileManager);

        mAppUidCache = getTokenCache(context, mAppUidCredentialCache);
    }

    private void initFociCache(final Context context) {
        final ISharedPreferencesFileManager fociCacheFileManager = getFociFileManager(context);

        mFociCredentialCache = getAccountCredentialCache(fociCacheFileManager);

        mFociCache = getTokenCache(context, mFociCredentialCache);
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
    }

    private void configureMocksForAppUid() {
        configureMocks(mDefaultAppUidTestBundle);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanSaveIntoAppUidCache() throws ClientException {
        configureMocksForAppUid();

        mFociCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<AccountRecord> accounts = mFociCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(mDefaultAppUidTestBundle.mGeneratedAccount, accounts.get(0));

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

        assertEquals(mDefaultAppUidTestBundle.mGeneratedAccessToken, ats.get(0));
        assertEquals(mDefaultAppUidTestBundle.mGeneratedRefreshToken, rts.get(0));
        assertEquals(mDefaultAppUidTestBundle.mGeneratedIdToken, ids.get(0));
    }

    @Test
    public void testCanSaveIntoFociCache() {
        // TODO
        configureMocksForFoci();
    }

    @Test
    public void testCacheMiss() {
        // TODO
    }

    @Test
    public void testRemoveCredentialAppUidCache() {
        // TODO
    }

    @Test
    public void testRemoveCredentialFociCache() {
        // TODO
    }

    @Test
    public void testRemoveCredentialMiss() {
        // TODO
    }

    @Test
    public void testGetAccountAppUidCache() {
        // TODO
    }

    @Test
    public void testGetAccountFociCache() {
        // TODO
    }

    @Test
    public void testGetAccountWithLocalAccountIdAppUidCache() {
        // TODO
    }

    @Test
    public void testGetAccountWithLocalAcocuntIdFociCache() {
        // TODO
    }

    @Test
    public void testGetAccountsAdal() {
        // TODO
    }

    @Test
    public void testGetAccountsMsal() {
        // TODO
    }

}
