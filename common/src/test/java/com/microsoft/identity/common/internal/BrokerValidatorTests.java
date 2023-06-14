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
package com.microsoft.identity.common.internal;

import static com.microsoft.identity.common.java.exception.ClientException.BROKER_VERIFICATION_FAILED_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.NOT_VALID_BROKER_FOUND;
import static org.robolectric.Shadows.shadowOf;

import android.accounts.AccountManager;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAccountManager;

import java.util.Set;

/**
 * Unit Tests for {@link BrokerValidator}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class})
public class BrokerValidatorTests {

    private BrokerValidator mBrokerValidator;

    @Before
    public void setup() {
        mBrokerValidator = new BrokerValidator(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testGetValidBrokersInDebugMode() {
        BrokerValidator.setShouldTrustDebugBrokers(true);
        final Set<BrokerData> brokerData = mBrokerValidator.getValidBrokers();
        Assert.assertEquals(8, brokerData.size());
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugBrokerHost()));
        Assert.assertTrue(brokerData.contains(BrokerData.getProdCompanyPortal()));
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugMicrosoftAuthenticator()));
        Assert.assertTrue(brokerData.contains(BrokerData.getProdMicrosoftAuthenticator()));
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugLTW()));
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugMockLtw()));
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugMockCp()));
        Assert.assertTrue(brokerData.contains(BrokerData.getDebugMockAuthApp()));
    }

    @Test
    public void testGetValidBrokersInReleaseMode() {
        BrokerValidator.setShouldTrustDebugBrokers(false);
        final Set<BrokerData> brokerData = mBrokerValidator.getValidBrokers();
        Assert.assertEquals(2, brokerData.size());
        Assert.assertTrue(brokerData.contains(BrokerData.getProdCompanyPortal()));
        Assert.assertTrue(brokerData.contains(BrokerData.getProdMicrosoftAuthenticator()));
    }

    @Test
    public void testGetCurrentActiveBrokerExactMatch() {
        final AccountManager accountManager = AccountManager.get(ApplicationProvider.getApplicationContext());
        shadowOf(accountManager).addAuthenticator("com.microsoft.workaccount");
        final ClientException exception = Assert.assertThrows(
                ClientException.class,
                () -> mBrokerValidator.getCurrentActiveBrokerPackageName()
        );
        // Means we have a match but we couldn't verify the signature.
        Assert.assertEquals(BROKER_VERIFICATION_FAILED_ERROR, exception.getErrorCode());
    }

    @Test
    public void testGetCurrentActiveBrokerDirtyString() {
        final AccountManager accountManager = AccountManager.get(ApplicationProvider.getApplicationContext());
        shadowOf(accountManager).addAuthenticator("  COM.MICROSOFT.WORKACCOUNT  ");
        final ClientException exception = Assert.assertThrows(
                ClientException.class,
                () -> mBrokerValidator.getCurrentActiveBrokerPackageName()
        );
        // Means we have a match but we couldn't verify the signature.
        Assert.assertEquals(BROKER_VERIFICATION_FAILED_ERROR, exception.getErrorCode());
    }

    @Test
    public void testGetCurrentActiveBrokerNoMatch() {
        final AccountManager accountManager = AccountManager.get(ApplicationProvider.getApplicationContext());
        shadowOf(accountManager).addAuthenticator("com.hello.world");
        final ClientException exception = Assert.assertThrows(
                ClientException.class,
                () -> mBrokerValidator.getCurrentActiveBrokerPackageName()
        );
        Assert.assertEquals(NOT_VALID_BROKER_FOUND, exception.getErrorCode());
    }

    @Test
    public void testGetCurrentActiveBrokerNoBrokeRegistered() {
        final ClientException exception = Assert.assertThrows(
                ClientException.class,
                () -> mBrokerValidator.getCurrentActiveBrokerPackageName()
        );
        Assert.assertEquals(NOT_VALID_BROKER_FOUND, exception.getErrorCode());
    }
}
