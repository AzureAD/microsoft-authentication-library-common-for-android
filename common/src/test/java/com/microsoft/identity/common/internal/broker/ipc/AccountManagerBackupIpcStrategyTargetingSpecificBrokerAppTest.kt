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
package com.microsoft.identity.common.internal.broker.ipc

import android.accounts.AuthenticatorDescription
import android.accounts.AuthenticatorException
import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.IBrokerValidator
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp.Companion.AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp.Companion.CONTENT_PROVIDER_PATH_KEY
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp.Companion.getAccountTypeForEachPackage
import com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountManagerBackupIpcStrategyTargetingSpecificBrokerAppTest {

    private val alwaysSuccessBrokerValidator = object: IBrokerValidator {
        override fun isValidBrokerPackage(packageName: String): Boolean {
            return true
        }

        override fun isSignedByKnownKeys(brokerData: BrokerData): Boolean {
            return true
        }
    }

    private val properlySetUpAccountManagerApps = {
        Array(3) {
            when (it) {
                0 -> AuthenticatorDescription(
                    "com.mock.testhaha",
                    "com.testhaha.someApp",
                    0, 0, 0, 0
                )
                // Targeted app (in this case, AuthApp)
                // is registered as account type owner.
                1 -> AuthenticatorDescription(
                    AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    0, 0, 0, 0
                )
                // Also registered as broker account type.
                2 -> AuthenticatorDescription(
                    BROKER_ACCOUNT_TYPE,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    0, 0, 0, 0
                )
                3 -> AuthenticatorDescription(
                    "com.contoso.conacct",
                    "com.contoso.anotherapp",
                    0, 0, 0, 0
                )
                else -> throw IndexOutOfBoundsException()
            }
        }
    }

    @Test
    fun testSendingSuccessRequest(){
        val strategy = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
            accountTypeForEachPackage = getAccountTypeForEachPackage(),
            sendRequestViaAccountManager = { accountType, bundle ->
                if (accountType == AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE &&
                    bundle.getString(CONTENT_PROVIDER_PATH_KEY) ==
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL.contentApi.path){
                    return@AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp Bundle().apply {
                        putBoolean("Happy", true)
                    }
                }

                throw IllegalStateException("Should not reach this line!")
            },
            getAccountManagerApps = properlySetUpAccountManagerApps,
            brokerValidator = alwaysSuccessBrokerValidator
        )

        val result = strategy.communicateToBroker(
                BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    null
                )
            )

        Assert.assertTrue(result!!.getBoolean("Happy"))
    }

    @Test
    fun testGettingErrorFromAccountManager(){
        val strategy = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
            accountTypeForEachPackage = getAccountTypeForEachPackage(),
            sendRequestViaAccountManager = { accountType, bundle ->
                throw AuthenticatorException("some error happened!")
            },
            getAccountManagerApps = properlySetUpAccountManagerApps,
            brokerValidator = alwaysSuccessBrokerValidator
        )

        try {
            strategy.communicateToBroker(
                BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    null
                )
            )
        }catch (t: Throwable) {
            Assert.assertEquals(
                BrokerCommunicationException.Category.CONNECTION_ERROR,
                (t as BrokerCommunicationException).category)
            Assert.assertTrue(t.cause is AuthenticatorException)
        }
    }

    // Try sending request to an app that is not in the list.
    @Test
    fun testSendingRequestToNonBrokerApp(){
        val strategy = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
            accountTypeForEachPackage = getAccountTypeForEachPackage(),
            sendRequestViaAccountManager = { _, _ ->
                throw IllegalStateException("Should not reach this layer!")
            },
            getAccountManagerApps = properlySetUpAccountManagerApps,
            brokerValidator = alwaysSuccessBrokerValidator
        )

        try {
            strategy.communicateToBroker(
                BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
                    "com.microsoft.somerandomapp",
                    null
                )
            )
        } catch (t: Throwable) {
            Assert.assertEquals(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                (t as BrokerCommunicationException).category)
        }
    }

    // Targeted app is not properly registered to account manager.
    @Test
    fun testAppNotRegisteredInAccountManager(){
        val strategy = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
            accountTypeForEachPackage = getAccountTypeForEachPackage(),
            sendRequestViaAccountManager = { _, _ ->
                throw IllegalStateException("Should not reach this layer!")
            },
            getAccountManagerApps = {
                Array(2) {
                    when (it) {
                        0 -> AuthenticatorDescription(
                            "com.mock.testhaha",
                            "com.testhaha.someApp",
                            0, 0, 0, 0
                        )
                        // Only registered as broker account type.
                        // We expect AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE to be here
                        // But it's not.... so it would fail.
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
            },
            brokerValidator = alwaysSuccessBrokerValidator
        )

        try {
            strategy.communicateToBroker(
                BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    null
                )
            )
        } catch (t: Throwable) {
            Assert.assertEquals(
                BrokerCommunicationException.Category.CONNECTION_ERROR,
                (t as BrokerCommunicationException).category)
        }
    }

    // Request should fail if the targeted app is not a valid broker package.
    @Test
    fun testInvalidBrokerApp(){
        val strategy = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
            accountTypeForEachPackage = getAccountTypeForEachPackage(),
            sendRequestViaAccountManager = { _, _ ->
                throw IllegalStateException("Should not reach this layer!")
            },
            getAccountManagerApps = properlySetUpAccountManagerApps,
            brokerValidator = object: IBrokerValidator {
                override fun isValidBrokerPackage(packageName: String): Boolean {
                    // Not a valid broker package!
                   return false
                }

                override fun isSignedByKnownKeys(brokerData: BrokerData): Boolean {
                    return false
                }
            }
        )

        try {
            strategy.communicateToBroker(
                BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
                    BrokerData.debugMicrosoftAuthenticator.packageName,
                    null
                )
            )
        } catch (t: Throwable) {
            Assert.assertEquals(
                BrokerCommunicationException.Category.CONNECTION_ERROR,
                (t as BrokerCommunicationException).category)
        }
    }
}