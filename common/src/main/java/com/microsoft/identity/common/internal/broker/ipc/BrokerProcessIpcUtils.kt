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
package com.microsoft.identity.common.internal.broker.ipc

import android.content.Context
import com.microsoft.identity.common.java.interfaces.IPlatformComponents

object BrokerProcessIpcUtils {

    /**
     * Get [IIpcStrategy] for requests between broker (:auth) processes.
     * See [AppSpecificRequestHandler] for the list of supported operations.
     * */
    fun getIpcStrategyForRequestBetweenBrokers(context: Context,
                                               components: IPlatformComponents): IIpcStrategy {
        val accountManagerBackup =
            AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp.tryCreateInstance(context)

        val contentProviderStrategy = ContentProviderStrategy(context, components)

        return if (accountManagerBackup == null){
            contentProviderStrategy
        } else {
            IpcStrategyWithBackup(
                primary = contentProviderStrategy,
                // Note: It's possible that AccountManagerBackup might run into a deadlock (and fail due to timeout).
                // After that, the :auth process might theoretically be alive.
                // We'll try the content provider again.
                backup = listOf(accountManagerBackup, contentProviderStrategy)
            )
        }
    }
}