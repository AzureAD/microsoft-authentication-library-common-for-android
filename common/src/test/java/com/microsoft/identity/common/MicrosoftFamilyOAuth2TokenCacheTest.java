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
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.APPLICATION_IDENTIFIER;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.AUTHORITY_TYPE;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CACHED_AT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.EXPIRES_ON;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.REALM;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SECRET;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.SESSION_KEY;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.TARGET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MicrosoftFamilyOAuth2TokenCache;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.shadows.ShadowAndroidSdkStorageEncryptionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class MicrosoftFamilyOAuth2TokenCacheTest extends MsalOAuth2TokenCacheTest {

    private static final AbstractAuthenticationScheme BEARER_SCHEME = new BearerAuthenticationSchemeInternal();

    private MicrosoftFamilyOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mOauth2TokenCache;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mOauth2TokenCache = new MicrosoftFamilyOAuth2TokenCache<>(
                AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()),
                accountCredentialCache,
                mockCredentialAdapter
        );
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testRetrieveFrt() throws ClientException {
        final String randomHomeAccountId = UUID.randomUUID().toString();

        final AccountCredentialTestBundle frtTestBundle = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
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
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.IdToken
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
                TARGET,
                frtTestBundle.mGeneratedAccount,
                BEARER_SCHEME
        );

        assertNotNull(familyCacheRecord);
        assertNotNull(familyCacheRecord.getAccount());
        assertNotNull(familyCacheRecord.getRefreshToken());
        assertNull(familyCacheRecord.getIdToken());
        assertNull(familyCacheRecord.getAccessToken());

        final ICacheRecord familyCacheRecordWithClientId = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID,
                TARGET,
                frtTestBundle.mGeneratedAccount,
                BEARER_SCHEME
        );

        assertNotNull(familyCacheRecordWithClientId);
        assertNotNull(familyCacheRecordWithClientId.getAccount());
        assertNotNull(familyCacheRecordWithClientId.getRefreshToken());
        assertNotNull(familyCacheRecordWithClientId.getIdToken());
        assertNotNull(familyCacheRecordWithClientId.getAccessToken());

        final ICacheRecord familyCacheRecordWithClientIdButNonMatchingTarget =
                mOauth2TokenCache.loadByFamilyId(
                        CLIENT_ID,
                        TARGET,
                        frtTestBundle.mGeneratedAccount,
                        BEARER_SCHEME
                );

        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget);
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getAccount());
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getRefreshToken());
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getIdToken());
        assertNotNull(familyCacheRecordWithClientIdButNonMatchingTarget.getAccessToken());

        final ICacheRecord wrongClientIdResult =
                mOauth2TokenCache.loadByFamilyId(
                        "12345",
                        TARGET,
                        frtTestBundle.mGeneratedAccount,
                        BEARER_SCHEME
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
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
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
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.IdToken
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
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
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
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.IdToken
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
                TARGET,
                frtTestBundle2.mGeneratedAccount,
                BEARER_SCHEME
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
                TARGET,
                frtTestBundle2.mGeneratedAccount,
                BEARER_SCHEME
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
                TARGET,
                randomAcct,
                BEARER_SCHEME
        );

        assertNotNull(cacheRecord3);
        assertNotNull(cacheRecord3.getAccount());
        assertNull(cacheRecord3.getRefreshToken());
        assertNull(cacheRecord3.getAccessToken());
        assertNull(cacheRecord3.getIdToken());
    }

    @Test
    public void testDualStackIdTokenRetrieval() throws ClientException {
        final String randomHomeAccountId = UUID.randomUUID().toString();
        final String randomRealm = UUID.randomUUID().toString();

        final AccountCredentialTestBundle frtTestBundleV2 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                UUID.randomUUID().toString(),
                "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId,
                ENVIRONMENT,
                randomRealm,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.IdToken
        );

        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV2.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV2.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV2.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV2.mGeneratedIdToken);

        // Save the family token data
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final AccountCredentialTestBundle frtTestBundleV1 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS,
                UUID.randomUUID().toString(),
                "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId,
                ENVIRONMENT,
                randomRealm,
                TARGET,
                CACHED_AT,
                EXPIRES_ON,
                SECRET,
                CLIENT_ID,
                APPLICATION_IDENTIFIER,
                SECRET,
                MOCK_ID_TOKEN_WITH_CLAIMS,
                "1",
                SESSION_KEY,
                CredentialType.V1IdToken
        );

        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV1.mGeneratedAccount);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV1.mGeneratedAccessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV1.mGeneratedRefreshToken);

        when(
                mockCredentialAdapter.createIdToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(frtTestBundleV1.mGeneratedIdToken);

        // Save the family token data
        mOauth2TokenCache.save(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final ICacheRecord familyCacheRecord = mOauth2TokenCache.loadByFamilyId(
                CLIENT_ID,
                TARGET,
                frtTestBundleV1.mGeneratedAccount,
                BEARER_SCHEME
        );

        assertNotNull(familyCacheRecord);
        assertNotNull(familyCacheRecord.getAccount());
        assertNotNull(familyCacheRecord.getRefreshToken());
        assertNotNull(familyCacheRecord.getIdToken());
        assertNotNull(familyCacheRecord.getV1IdToken());
        assertNotNull(familyCacheRecord.getAccessToken());
    }

    @Test
    public void testMultipleClientsUpdatingFociCacheConcurrently() throws ClientException, InterruptedException {
        final String localAccountId = UUID.randomUUID().toString();
        final String realm = UUID.randomUUID().toString();
        final String randomHomeAccountId = localAccountId + "." + realm;
        final String[] thread1 = {""};
        final String[] thread2 = {""};
        final AccountCredentialTestBundle frtTestBundle = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS, localAccountId, "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId, ENVIRONMENT, realm, TARGET, CACHED_AT, EXPIRES_ON, SECRET, CLIENT_ID,
                SECRET, MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS, "1",
                SESSION_KEY, CredentialType.IdToken);

        when(mockCredentialAdapter.createAccount(mockStrategy, mockRequest,mockResponse)).thenReturn(frtTestBundle.mGeneratedAccount);
        when(mockCredentialAdapter.createAccessToken(mockStrategy, mockRequest, mockResponse)).thenReturn(frtTestBundle.mGeneratedAccessToken);
        when(mockCredentialAdapter.createRefreshToken(mockStrategy, mockRequest, mockResponse)).thenReturn(frtTestBundle.mGeneratedRefreshToken);
        when(mockCredentialAdapter.createIdToken(mockStrategy, mockRequest, mockResponse)).thenReturn(frtTestBundle.mGeneratedIdToken);

        // Save tokens in cache for a Foci client app, as mocked in frtTestBundle
        mOauth2TokenCache.save(mockStrategy, mockRequest, mockResponse);

        // Setup mocks for new tokens to be returned for two different foci client apps client_1 and client_2
        // The idea is that these two applications will be running and saving tokens concurrently from two separate threads.
        // The mocks return different tokens based on the thread names, to mock having two separte Refresh tokens
        final AccountCredentialTestBundle frtTestBundle1 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS, localAccountId, "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId, ENVIRONMENT, realm, TARGET + " client1_scope", CACHED_AT, EXPIRES_ON, SECRET, "client_1",
                SECRET, MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS, "1",
                SESSION_KEY, CredentialType.IdToken);

        final AccountCredentialTestBundle frtTestBundle2 = new AccountCredentialTestBundle(
                MicrosoftAccount.AUTHORITY_TYPE_MS_STS, localAccountId, "test.user@tenant.onmicrosoft.com",
                randomHomeAccountId, ENVIRONMENT, realm, TARGET + " client2_scope", CACHED_AT, EXPIRES_ON, SECRET, "client_2",
                SECRET, MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS, "1", SESSION_KEY,
                CredentialType.IdToken);

        when(mockCredentialAdapter.createAccount(mockStrategy, mockRequest, mockResponse)).thenAnswer(new Answer<AccountRecord>() {
            @Override
            public AccountRecord answer(InvocationOnMock invocation) throws Throwable {
                final String currentThread = Thread.currentThread().getName();
                if(currentThread == thread1[0]) {
                    return frtTestBundle1.mGeneratedAccount;
                } else if(currentThread == thread2[0]) {
                    return frtTestBundle2.mGeneratedAccount;
                }
                return null;
            }
        });
        when(mockCredentialAdapter.createAccessToken(mockStrategy, mockRequest, mockResponse)).thenAnswer(new Answer<AccessTokenRecord>() {
            @Override
            public AccessTokenRecord answer(InvocationOnMock invocation) throws Throwable {
                final String currentThread = Thread.currentThread().getName();
                if(currentThread == thread1[0]) {
                    frtTestBundle1.mGeneratedAccessToken.setSecret(SECRET + "AT2" + currentThread);
                    return frtTestBundle1.mGeneratedAccessToken;
                } else if(currentThread == thread2[0]) {
                    frtTestBundle2.mGeneratedAccessToken.setSecret(SECRET + "AT2" + currentThread);
                    return frtTestBundle2.mGeneratedAccessToken;
                }
                return null;
            }
        });
        when(mockCredentialAdapter.createRefreshToken(mockStrategy, mockRequest, mockResponse)).thenAnswer(new Answer<RefreshTokenRecord>() {
            @Override
            public RefreshTokenRecord answer(InvocationOnMock invocation) throws Throwable {
                final String currentThread = Thread.currentThread().getName();
                if(currentThread == thread1[0]) {
                    frtTestBundle1.mGeneratedRefreshToken.setSecret(SECRET + "RT2" + Thread.currentThread().getName());
                    return frtTestBundle1.mGeneratedRefreshToken;
                } else if(currentThread == thread2[0]) {
                    frtTestBundle2.mGeneratedRefreshToken.setSecret(SECRET + "RT2" + Thread.currentThread().getName());
                    return frtTestBundle2.mGeneratedRefreshToken;
                }
                return null;
            }
        });
        when(mockCredentialAdapter.createIdToken(mockStrategy, mockRequest, mockResponse)).thenAnswer(new Answer<IdTokenRecord>() {
            @Override
            public IdTokenRecord answer(InvocationOnMock invocation) throws Throwable {
                final String currentThread = Thread.currentThread().getName();
                if(currentThread == thread1[0]) {
                    return frtTestBundle1.mGeneratedIdToken;
                } else if(currentThread == thread2[0]) {
                    return frtTestBundle2.mGeneratedIdToken;
                }
                return null;
            }
        });

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        // Submit runnable tasks to save tokens from two different threads concurrently
        final Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                try {
                    thread1[0] = Thread.currentThread().getName();
                    countDownLatch.await();
                    mOauth2TokenCache.save(mockStrategy, mockRequest, mockResponse);
                } catch (ClientException | InterruptedException e) {
                    throw new AssertionError(e.getMessage());
                }
            }
        };

        final Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                try {
                    thread2[0] = Thread.currentThread().getName();
                    countDownLatch.await();
                    mOauth2TokenCache.save(mockStrategy, mockRequest, mockResponse);
                } catch (ClientException | InterruptedException e) {
                    throw new AssertionError(e.getMessage());
                }
            }
        };

        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        final Future runnableTask1 = executorService.submit(runnable1);
        final Future runnableTask2 = executorService.submit(runnable2);
        countDownLatch.countDown();
        try{
            runnableTask1.get();
            runnableTask2.get();
        } catch (ExecutionException e) {
            throw new AssertionError(e.getMessage());
        } finally {
            executorService.shutdown();
        }

        // verify we are able to get Account, RT, AT and IdToken from the cache after both updates are done
        final ICacheRecord cacheRecord1 = mOauth2TokenCache.loadByFamilyId("client_1", TARGET, frtTestBundle2.mGeneratedAccount, BEARER_SCHEME);

        assertNotNull(cacheRecord1);
        assertNotNull(cacheRecord1.getRefreshToken());
        assertNotNull(cacheRecord1.getAccessToken());
        assertNotNull(cacheRecord1.getIdToken());

        final ICacheRecord cacheRecord2 = mOauth2TokenCache.loadByFamilyId("client_2", TARGET, frtTestBundle2.mGeneratedAccount, BEARER_SCHEME);

        assertNotNull(cacheRecord2);
        assertNotNull(cacheRecord2.getRefreshToken());
        assertNotNull(cacheRecord2.getAccessToken());
        assertNotNull(cacheRecord2.getIdToken());
    }

}
