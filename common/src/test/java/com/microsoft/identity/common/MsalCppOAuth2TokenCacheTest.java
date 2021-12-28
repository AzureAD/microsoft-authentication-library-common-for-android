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

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.AccountDeletionRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalCppOAuth2TokenCache;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import static com.microsoft.identity.common.MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS;
import static com.microsoft.identity.common.MsalOAuth2TokenCacheTest.AccountCredentialTestBundle;
import static com.microsoft.identity.common.SharedPreferencesAccountCredentialCacheTest.APPLICATION_IDENTIFIER;
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

@RunWith(RobolectricTestRunner.class)
public class MsalCppOAuth2TokenCacheTest {

    private MsalCppOAuth2TokenCache mCppCache;

    // Test Accounts/Credentials
    private AccountCredentialTestBundle mTestBundle;

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        // Context and related init
        mContext = ApplicationProvider.getApplicationContext();
        mCppCache = MsalCppOAuth2TokenCache.create(AndroidPlatformComponentsFactory.createFromContext(mContext));

        // Credentials for testing
        mTestBundle = new AccountCredentialTestBundle(
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
                CredentialType.V1IdToken
        );
    }

    @After
    public void tearDown() {
        mCppCache.clearCache();
    }

    @Test
    public void saveAndGetAccountTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save the the cache
        mCppCache.saveAccountRecord(generatedAccount);

        // Restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        Assert.assertNotNull(restoredAccount);
        Assert.assertEquals(generatedAccount, restoredAccount);
    }

    @Test
    public void getAccountNullTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void getAllAccountsTest() {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;
        mCppCache.saveAccountRecord(generatedAccount);

        @SuppressWarnings("unchecked")
        final List<AccountRecord> accounts = mCppCache.getAllAccounts();
        final AccountRecord restoredAccount = accounts.get(0);
        Assert.assertEquals(generatedAccount, restoredAccount);
        Assert.assertEquals(1, accounts.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllAccountsResultImmutableTest() {
        @SuppressWarnings("unchecked")
        final List<AccountRecord> accounts = mCppCache.getAllAccounts();
        accounts.add(mTestBundle.mGeneratedAccount);
    }

    @Test
    public void getAllAccountsEmptyTest() {
        @SuppressWarnings("unchecked")
        final List<AccountRecord> accounts = mCppCache.getAllAccounts();
        Assert.assertTrue(accounts.isEmpty());
    }

    @Test
    public void removeAccountTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);
        mCppCache.saveCredentials(null, mTestBundle.mGeneratedRefreshToken);

        // Call remove
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Check the receipt
        Assert.assertEquals(generatedAccount, deletionRecord.get(0));

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it doesn't exist....
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void removeNoMatchingAccount() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);
        mCppCache.saveCredentials(null, mTestBundle.mGeneratedRefreshToken);

        // Call remove with a different realm
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                REALM2
        );

        // Check the receipt, should remove nothing
        Assert.assertEquals(0, deletionRecord.size());

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it still exists....
        Assert.assertNotNull(restoredAccount);
    }

    @Test
    public void removeAccountWithHomeAccountIdTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);
        mCppCache.saveCredentials(null, mTestBundle.mGeneratedRefreshToken);

        // Call remove
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                "",
                ""
        );

        // Check the receipt
        Assert.assertEquals(generatedAccount, deletionRecord.get(0));

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it doesn't exist....
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void removeTwoAccountsWithDifferentRealmsAndEnvironmentsTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);
        mCppCache.saveCredentials(null, mTestBundle.mGeneratedRefreshToken);

        // Save the second account with a different realm and environment
        generatedAccount.setEnvironment("login.chinacloudapi.cn");
        generatedAccount.setRealm(REALM2);
        mCppCache.saveAccountRecord(generatedAccount);

        // Call remove
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                "",
                ""
        );

        // Check the receipt, should delete both
        Assert.assertEquals(2, deletionRecord.size());

        // Try to restore them
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                REALM
        );

        final AccountRecord restoredAccount2 = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                "login.chinacloudapi.cn",
                REALM2
        );

        // Make sure they don't exist....
        Assert.assertNull(restoredAccount);
        Assert.assertNull(restoredAccount2);
    }

    @Test
    public void removeAccountNoRTTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);

        // Call remove
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Check the receipt
        Assert.assertEquals(generatedAccount, deletionRecord.get(0));

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it doesn't exist....
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void forceRemoveAccountTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);

        // Do not save any credentials for this account...

        final AccountDeletionRecord deletionRecord = mCppCache.forceRemoveAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        Assert.assertEquals(1, deletionRecord.size());

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it doesn't exist....
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void forceRemoveAccountWithHomeAccountIdTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);

        // Do not save any credentials for this account...

        final AccountDeletionRecord deletionRecord = mCppCache.forceRemoveAccount(
                generatedAccount.getHomeAccountId(),
                "",
                ""
        );

        Assert.assertEquals(1, deletionRecord.size());

        // Try to restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Make sure it doesn't exist....
        Assert.assertNull(restoredAccount);
    }

    @Test
    public void removeNonexistentAccountTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;
        final AccountDeletionRecord deletionRecord = mCppCache.removeAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );
        Assert.assertTrue(deletionRecord.isEmpty());
    }

    @Test
    public void saveCredentialsWithAccountTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        mCppCache.saveAccountRecord(generatedAccount);

        mCppCache.saveCredentials(
                mTestBundle.mGeneratedAccessToken,
                mTestBundle.mGeneratedIdToken,
                mTestBundle.mGeneratedRefreshToken
        );

        // Restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        Assert.assertNotNull(restoredAccount);
        Assert.assertEquals(generatedAccount, restoredAccount);

        final ICacheRecord cacheRecord = mCppCache.load(
                mTestBundle.mGeneratedIdToken.getClientId(),
                mTestBundle.mGeneratedAccessToken.getApplicationIdentifier(),
                mTestBundle.mGeneratedAccessToken.getTarget(),
                generatedAccount,
                new BearerAuthenticationSchemeInternal()
        );

        Assert.assertEquals(
                mTestBundle.mGeneratedAccessToken,
                cacheRecord.getAccessToken()
        );
    }

    @Test
    public void saveCredentialsWithAccountForPRTTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        mCppCache.saveAccountRecord(generatedAccount);

        mCppCache.saveCredentials(
                mTestBundle.mGeneratedAccessToken,
                mTestBundle.mGeneratedIdToken,
                mTestBundle.mGeneratedRefreshToken,
                mTestBundle.mGeneratedPrimaryRefreshToken
        );

        // Restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        Assert.assertNotNull(restoredAccount);
        Assert.assertEquals(generatedAccount, restoredAccount);

        final ICacheRecord cacheRecord = mCppCache.load(
                mTestBundle.mGeneratedIdToken.getClientId(),
                mTestBundle.mGeneratedAccessToken.getApplicationIdentifier(),
                mTestBundle.mGeneratedAccessToken.getTarget(),
                generatedAccount,
                new BearerAuthenticationSchemeInternal()
        );

        Assert.assertEquals(
                mTestBundle.mGeneratedAccessToken,
                cacheRecord.getAccessToken()
        );
    }

    @Test
    public void saveCredentialsWithoutAccountTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        mCppCache.saveCredentials(
                null,
                mTestBundle.mGeneratedAccessToken,
                mTestBundle.mGeneratedIdToken,
                mTestBundle.mGeneratedRefreshToken
        );

        // Restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Account doesn't exist
        Assert.assertNull(restoredAccount);

        // Inspect the contents of the cache
        @SuppressWarnings("unchecked")
        final List<Credential> credentials = mCppCache.getCredentials();
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedAccessToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedIdToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedRefreshToken));
    }

    @Test
    public void saveCredentialsWithoutAccountForPRTTest() throws ClientException {
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        mCppCache.saveCredentials(
                null,
                mTestBundle.mGeneratedAccessToken,
                mTestBundle.mGeneratedIdToken,
                mTestBundle.mGeneratedPrimaryRefreshToken
        );

        // Restore it
        final AccountRecord restoredAccount = mCppCache.getAccount(
                generatedAccount.getHomeAccountId(),
                generatedAccount.getEnvironment(),
                generatedAccount.getRealm()
        );

        // Account doesn't exist
        Assert.assertNull(restoredAccount);

        // Inspect the contents of the cache
        @SuppressWarnings("unchecked")
        final List<Credential> credentials = mCppCache.getCredentials();
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedAccessToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedIdToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedPrimaryRefreshToken));
    }

    @Test(expected = ClientException.class)
    public void saveATSansTargetThrowsException() throws ClientException {
        mTestBundle.mGeneratedAccessToken.setTarget(null);
        mCppCache.saveCredentials(
                null,
                mTestBundle.mGeneratedAccessToken,
                mTestBundle.mGeneratedIdToken,
                mTestBundle.mGeneratedRefreshToken
        );
    }
}
