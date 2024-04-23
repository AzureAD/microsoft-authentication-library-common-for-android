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

import android.os.Bundle
import com.microsoft.identity.common.internal.broker.BrokerData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class IpcStrategyWithBackupTests {

    companion object{
        val mockRequest = BrokerOperationBundle(
            BrokerOperationBundle.Operation.BROKER_DISCOVERY_METADATA_RETRIEVAL,
            BrokerData.prodMicrosoftAuthenticator.packageName,
            Bundle().apply {
                putBoolean("REQUEST", true)
            }
        )

        val mockResultBundle = Bundle().apply {
            putBoolean("RESULT", true)
        }
    }

    class MockIpc(private val type: IIpcStrategy.Type,
                  private val shouldThrowException: Boolean = false,
                  private val resultBundle: Bundle = mockResultBundle) : IIpcStrategy{
        override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
            if (shouldThrowException){
                throw Throwable("${type.name} failed.")
            }
            return resultBundle
        }

        override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
            return true
        }

        override fun getType(): IIpcStrategy.Type {
            return type
        }
    }

    // Verify that the type should be the primary's.
    @Test
    fun testGetType(){
        val strategy = IpcStrategyWithBackup(
            primary = MockIpc(type = IIpcStrategy.Type.CONTENT_PROVIDER,
                shouldThrowException = true),
            backup = listOf(
                MockIpc(type = IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.BOUND_SERVICE,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API,
                    shouldThrowException = true),
            )
        )

        Assert.assertEquals(IIpcStrategy.Type.CONTENT_PROVIDER, strategy.getType())
    }

    // Test with 2 backups. the primary succeeds but all backup fails.
    @Test
    fun testPrimarySucceed_BackupFail(){
        val strategy = IpcStrategyWithBackup(
            primary = MockIpc(type = IIpcStrategy.Type.CONTENT_PROVIDER),
            backup = listOf(
                MockIpc(type = IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API,
                    shouldThrowException = true),
            )
        )

        Assert.assertEquals(mockResultBundle, strategy.communicateToBroker(mockRequest))
    }

    // Test with 3 backups. the primary and the first backup fails.
    @Test
    fun testPrimarySucceed_SomeBackupFail(){
        val strategy = IpcStrategyWithBackup(
            primary = MockIpc(type = IIpcStrategy.Type.CONTENT_PROVIDER,
                shouldThrowException = true),
            backup = listOf(
                MockIpc(type = IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.BOUND_SERVICE),
                MockIpc(type = IIpcStrategy.Type.LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API),
            )
        )

        Assert.assertEquals(mockResultBundle, strategy.communicateToBroker(mockRequest))
    }

    // Test with 2 backups. only the primary fails.
    @Test
    fun testPrimaryFail_BackupSucceed(){
        val strategy = IpcStrategyWithBackup(
            primary = MockIpc(type = IIpcStrategy.Type.CONTENT_PROVIDER,
                shouldThrowException = true),
            backup = listOf(
                MockIpc(type = IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT),
                MockIpc(type = IIpcStrategy.Type.BOUND_SERVICE),
            )
        )

        Assert.assertEquals(mockResultBundle, strategy.communicateToBroker(mockRequest))
    }

    // Test with 3 backups. the primary and all backup fails.
    @Test
    fun testAllFail(){
        val strategy = IpcStrategyWithBackup(
            primary = MockIpc(type = IIpcStrategy.Type.CONTENT_PROVIDER,
                shouldThrowException = true),
            backup = listOf(
                MockIpc(type = IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.BOUND_SERVICE,
                    shouldThrowException = true),
                MockIpc(type = IIpcStrategy.Type.LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API,
                    shouldThrowException = true),
            )
        )

        try {
            strategy.communicateToBroker(mockRequest)
        } catch (t: Throwable) {
            Assert.assertEquals(
                "${IIpcStrategy.Type.CONTENT_PROVIDER.name} failed.",
                t.message
            )
        }
    }
}