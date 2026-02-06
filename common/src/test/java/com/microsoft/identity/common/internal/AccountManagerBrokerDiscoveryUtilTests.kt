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
package com.microsoft.identity.common.internal

import android.accounts.AccountManager
import android.accounts.AuthenticatorDescription
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.activebrokerdiscovery.AccountManagerBrokerDiscoveryUtil
import com.microsoft.identity.common.internal.broker.BrokerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAccountManager

/**
 * Unit Tests for [AccountManagerBrokerDiscoveryUtil].
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAccountManager::class])
class AccountManagerBrokerDiscoveryUtilTests {

    @Test
    fun testGetCurrentActiveBrokerExactMatch() {
        val accountManager = AccountManager.get(ApplicationProvider.getApplicationContext())
        Shadows.shadowOf(accountManager).addAuthenticator("com.microsoft.workaccount")
        val util = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = BrokerData.prodBrokers,
            isSignedByKnownKeys = { brokerData ->
                // Mock validation.
                brokerData == BrokerData.prodMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                accountManager.authenticatorTypes
            })
        Assert.assertNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerWithDebugAuthApp() {
        val util = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = BrokerData.allBrokers,
            isSignedByKnownKeys = { brokerData ->
                // Mock validation.
                brokerData == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                getMockedAccountManager()?.authenticatorTypes!!
            })
        Assert.assertNotNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerWithReleaseAuthApp() {
        val util = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = BrokerData.prodBrokers,
            isSignedByKnownKeys = { brokerData ->
                // Mock validation.
                brokerData == BrokerData.prodMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                getMockedAccountManager()?.authenticatorTypes!!
            })
        Assert.assertNotNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerExactMatch_CannotValidateSignature() {
        val accountManager = AccountManager.get(ApplicationProvider.getApplicationContext())
        Shadows.shadowOf(accountManager).addAuthenticator("com.microsoft.workaccount")

        // Means we have a match but we couldn't verify the signature.
        val util = AccountManagerBrokerDiscoveryUtil(ApplicationProvider.getApplicationContext())
        Assert.assertNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerDirtyString() {
        val accountManager = AccountManager.get(ApplicationProvider.getApplicationContext())
        Shadows.shadowOf(accountManager).addAuthenticator("  COM.MICROSOFT.WORKACCOUNT  ")

        val util = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = BrokerData.prodBrokers,
            isSignedByKnownKeys = { brokerData ->
                // Mock validation.
                brokerData == BrokerData.prodMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                accountManager.authenticatorTypes
            })
        Assert.assertNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerNoMatch() {
        val accountManager = AccountManager.get(ApplicationProvider.getApplicationContext())
        Shadows.shadowOf(accountManager).addAuthenticator("com.hello.world")

        val util = AccountManagerBrokerDiscoveryUtil(ApplicationProvider.getApplicationContext())
        Assert.assertNull(util.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testGetCurrentActiveBrokerNoBrokeRegistered() {
        val util = AccountManagerBrokerDiscoveryUtil(ApplicationProvider.getApplicationContext())
        Assert.assertNull(util.getActiveBrokerFromAccountManager())
    }

    private fun getMockedAccountManager(): AccountManager? {
        val mockedAccountManager = Mockito.mock(AccountManager::class.java)
        val authenticatorDescription = AuthenticatorDescription(
            "com.microsoft.workaccount",
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            0,  // label id
            0,  // icon id
            0,  // small icon id
            0 // pref id
        )
        val mockedAuthenticator = Mockito.spy(authenticatorDescription)
        val mockedAuthenticatorTypes = arrayOf(mockedAuthenticator)
        Mockito.`when`(mockedAccountManager.authenticatorTypes).thenReturn(mockedAuthenticatorTypes)
        return mockedAccountManager
    }
}