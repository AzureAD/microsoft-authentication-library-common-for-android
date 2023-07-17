//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.broker.IBrokerAccount;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import lombok.NonNull;

/**
 * Tests for {@link AccountManagerBrokerAccount} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N})
public class AccountManagerBrokerAccountTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private static final String TEST_ACCOUNT_NAME = "test@microsoft.com";
    private static final String ACCOUNT_TYPE = "com.microsoft.workaccount";

    private final AccountManager ACCOUNT_MANAGER = AccountManager.get(mContext);

    @Before
    public void setup() {
        final Account[] accounts = ACCOUNT_MANAGER.getAccountsByType(ACCOUNT_TYPE);
        Assert.assertNotNull(accounts);
        Assert.assertEquals(0, accounts.length);
    }

    @Test
    public void testCanAdaptAccountManagerAccountWhenAccountProvided() {
        final Account account = new Account(TEST_ACCOUNT_NAME, ACCOUNT_TYPE);
        Assert.assertNotNull(account);

        final AccountManagerBrokerAccount androidBrokerAccount = AccountManagerBrokerAccount.adapt(account);
        Assert.assertNotNull(androidBrokerAccount);
        Assert.assertEquals(TEST_ACCOUNT_NAME, androidBrokerAccount.getUsername());
        Assert.assertEquals(account, androidBrokerAccount.getAccount());
    }

    @Test
    public void testCanAdaptAccountManagerAccountWhenAccountNameAndTypeProvided() {
        final AccountManagerBrokerAccount androidBrokerAccount = AccountManagerBrokerAccount.create(
                ACCOUNT_MANAGER,
                TEST_ACCOUNT_NAME,
                ACCOUNT_TYPE
        );

        Assert.assertNotNull(androidBrokerAccount);
        Assert.assertEquals(TEST_ACCOUNT_NAME, androidBrokerAccount.getUsername());
        Assert.assertEquals(
                new Account(TEST_ACCOUNT_NAME, ACCOUNT_TYPE),
                androidBrokerAccount.getAccount()
        );
    }

    @Test
    public void testCanCastBrokerAccountToAndroidBrokerAccountWhenPossible() {
        final IBrokerAccount brokerAccount = AccountManagerBrokerAccount.create(
                ACCOUNT_MANAGER,
                TEST_ACCOUNT_NAME,
                ACCOUNT_TYPE
        );

        final AccountManagerBrokerAccount androidBrokerAccount = AccountManagerBrokerAccount.cast(ACCOUNT_MANAGER, brokerAccount);
        Assert.assertNotNull(androidBrokerAccount);
        Assert.assertEquals(brokerAccount, androidBrokerAccount);
    }

    @Test
    public void testCastBrokerAccountToAccountManagerBrokerAccount() {
        final IBrokerAccount brokerAccount = new IBrokerAccount() {
            @NonNull
            @Override
            public String getType() {
                return ACCOUNT_TYPE;
            }

            @Override
            public @NonNull String getUsername() {
                return TEST_ACCOUNT_NAME;
            }
        };

        final AccountManagerBrokerAccount androidBrokerAccount = AccountManagerBrokerAccount.cast(ACCOUNT_MANAGER, brokerAccount);
        Assert.assertNotNull(androidBrokerAccount);
        Assert.assertEquals(brokerAccount.getUsername(), androidBrokerAccount.getUsername());
        Assert.assertEquals(brokerAccount.getType(), androidBrokerAccount.getType());
    }
}
