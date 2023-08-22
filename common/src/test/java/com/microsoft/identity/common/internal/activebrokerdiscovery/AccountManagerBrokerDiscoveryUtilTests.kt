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
package com.microsoft.identity.common.internal.activebrokerdiscovery

import android.accounts.AuthenticatorDescription
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountManagerBrokerDiscoveryUtilTests {

    @Test
    fun testHappyScenario() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator, BrokerData.debugBrokerHost),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(2) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            "com.mock.testhaha",
                            "com.testhaha.someApp",
                            0, 0, 0, 0
                        )
                        1 -> AuthenticatorDescription(
                            BROKER_ACCOUNT_TYPE,
                            BrokerData.debugMicrosoftAuthenticator.packageName,
                            0, 0, 0, 0
                        )
                        2 -> AuthenticatorDescription(
                            "com.contoso.conacct",
                            "com.contoso.anotherapp",
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertEquals(BrokerData.debugMicrosoftAuthenticator, result)
    }

    @Test
    fun testNoBrokerAppInstalled() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator, BrokerData.debugBrokerHost),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(2) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            "com.mock.testhaha",
                            "com.testhaha.someApp",
                            0, 0, 0, 0
                        )
                        1 -> AuthenticatorDescription(
                            "com.contoso.conacct",
                            "com.contoso.anotherapp",
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        Assert.assertNull(acctMgr.getActiveBrokerFromAccountManager())
    }

    @Test
    fun testValidationFailed() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator),
            isSignedByKnownKeys = { app ->
                false
            },
            getAccountManagerApps = {
                Array(1) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            BROKER_ACCOUNT_TYPE,
                            BrokerData.debugMicrosoftAuthenticator.packageName,
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertNull(result)
    }

    @Test
    fun testPackageNameWithCamelCase() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(1) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            BROKER_ACCOUNT_TYPE,
                            "com.Azure.Authenticator",
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertEquals(BrokerData.debugMicrosoftAuthenticator, result)
    }

    @Test
    fun testPackageNameWithIndents() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(1) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            BROKER_ACCOUNT_TYPE,
                            " ${BrokerData.debugMicrosoftAuthenticator.packageName} ",
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertEquals(BrokerData.debugMicrosoftAuthenticator, result)
    }

    @Test
    fun testAccountTypeWithCamelCase() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(1) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            "com.Microsoft.WorkAccount",
                            BrokerData.debugMicrosoftAuthenticator.packageName,
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertEquals(BrokerData.debugMicrosoftAuthenticator, result)
    }

    @Test
    fun testAccountTypeWithIndents() {
        val acctMgr = AccountManagerBrokerDiscoveryUtil(
            knownBrokerApps = setOf(BrokerData.debugMicrosoftAuthenticator),
            isSignedByKnownKeys = { app ->
                app == BrokerData.debugMicrosoftAuthenticator
            },
            getAccountManagerApps = {
                Array(1) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            " $BROKER_ACCOUNT_TYPE   ",
                            BrokerData.debugMicrosoftAuthenticator.packageName,
                            0, 0, 0, 0
                        )
                        else -> throw IndexOutOfBoundsException()
                    }
                }
            }
        )

        val result = acctMgr.getActiveBrokerFromAccountManager()
        Assert.assertEquals(BrokerData.debugMicrosoftAuthenticator, result)
    }
}
