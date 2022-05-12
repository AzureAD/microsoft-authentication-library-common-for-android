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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.BuildConfig;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.broker.DebugBrokerTrustingApp;
import com.microsoft.identity.common.shadows.ShadowPackageHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.Set;

/**
 * Unit Tests for {@link BrokerValidator}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = ShadowPackageHelper.class
)
public class BrokerValidatorTests {

    private BrokerValidator mBrokerValidator;

    @Before
    public void setup() {
        mBrokerValidator = new BrokerValidator(ApplicationProvider.getApplicationContext());
        ReflectionHelpers.setStaticField(BuildConfig.class, "DEBUG", true);
    }

    @Test
    public void testGetValidBrokersInDebugMode() {
        mBrokerValidator.setShouldTrustDebugBrokers(true);
        final Set<BrokerData> brokerData = mBrokerValidator.getValidBrokers();
        Assert.assertEquals(4, brokerData.size());
        Assert.assertTrue(brokerData.contains(BrokerData.BROKER_HOST));
        Assert.assertTrue(brokerData.contains(BrokerData.COMPANY_PORTAL));
        Assert.assertTrue(brokerData.contains(BrokerData.MICROSOFT_AUTHENTICATOR_DEBUG));
        Assert.assertTrue(brokerData.contains(BrokerData.MICROSOFT_AUTHENTICATOR_PROD));
    }

    @Test
    public void testGetValidBrokersInReleaseMode() {
        mBrokerValidator.setShouldTrustDebugBrokers(false);
        final Set<BrokerData> brokerData = mBrokerValidator.getValidBrokers();
        Assert.assertEquals(2, brokerData.size());
        Assert.assertTrue(brokerData.contains(BrokerData.COMPANY_PORTAL));
        Assert.assertTrue(brokerData.contains(BrokerData.MICROSOFT_AUTHENTICATOR_PROD));
    }

    @Test
    public void testShouldTrustDebugBrokersInDebugMode() {
        mBrokerValidator.setShouldTrustDebugBrokers(true);

        Assert.assertTrue(mBrokerValidator.getShouldTrustDebugBrokers());
    }

    @Test
    public void testShouldTrustDebugBrokersInReleaseMode() {
        ReflectionHelpers.setStaticField(BuildConfig.class, "DEBUG", false);

        Assert.assertThrows("Cannot trust debug brokers in non-debug builds.", RuntimeException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                mBrokerValidator.setShouldTrustDebugBrokers(true);
            }
        });

        // should not throw if setting to false
        mBrokerValidator.setShouldTrustDebugBrokers(false);
        Assert.assertFalse(mBrokerValidator.getShouldTrustDebugBrokers());
    }

    @Test
    @Config(
            packageName = "com.microsoft.identity.client.msal.testapp"
    )
    public void testShouldTrustDebugBrokersForTestApps() {
        ReflectionHelpers.setStaticField(BuildConfig.class, "DEBUG", false);

        ShadowPackageHelper.putSignatureHash(
                DebugBrokerTrustingApp.MSAL_TEST_APP.getPackageName(), DebugBrokerTrustingApp.MSAL_TEST_APP.getSignatureHash()
        );
        mBrokerValidator.setShouldTrustDebugBrokers(true);
    }
}
