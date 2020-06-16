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

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.cache.AccountDeletionRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalCppOAuth2TokenCache;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.microsoft.identity.common.MicrosoftStsAccountCredentialAdapterTest.MOCK_ID_TOKEN_WITH_CLAIMS;
import static com.microsoft.identity.common.MsalOAuth2TokenCacheTest.AccountCredentialTestBundle;
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

@RunWith(AndroidJUnit4.class)
public class MsalCppOAuth2TokenCacheTest extends AndroidSecretKeyEnabledHelper {

    private MsalCppOAuth2TokenCache mCppCache;

    // Test Accounts/Credentials
    private AccountCredentialTestBundle mTestBundle;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Context and related init
        final Context context = InstrumentationRegistry.getTargetContext();
        mCppCache = MsalCppOAuth2TokenCache.create(context);

        // Credentials for testing
        mTestBundle = new AccountCredentialTestBundle(
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
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
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

        final List<AccountRecord> accounts = mCppCache.getAllAccounts();
        final AccountRecord restoredAccount = accounts.get(0);
        Assert.assertEquals(generatedAccount, restoredAccount);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllAccountsResultImmutableTest() {
        final List<AccountRecord> accounts = mCppCache.getAllAccounts();
        accounts.add(mTestBundle.mGeneratedAccount);
    }

    @Test
    public void getAllAccountsEmptyTest() {
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

        // TODO 6/15/20
        // I have added a new api to "force remove" an AccountRecord even if no RT exists to match it
        // Open questions:
        // Is the current behavior of removeAccount() where the AccountRecord is deleted only if
        // there is a matching RT acceptable?
        // A 'backup' API is provided to delete the AccountRecord
    }

    @Test
    public void forceRemoveAccountTest() throws ClientException {
        // Get the generated account
        final AccountRecord generatedAccount = mTestBundle.mGeneratedAccount;

        // Save it to the cache
        mCppCache.saveAccountRecord(generatedAccount);

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
                generatedAccount,
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
        final List<Credential> credentials = mCppCache.getCredentials();
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedAccessToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedIdToken));
        Assert.assertTrue(credentials.contains(mTestBundle.mGeneratedRefreshToken));
    }
}
