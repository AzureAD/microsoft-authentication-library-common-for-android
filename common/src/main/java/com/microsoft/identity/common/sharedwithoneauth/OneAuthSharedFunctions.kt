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
package com.microsoft.identity.common.sharedwithoneauth

import android.content.Context
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerAddAccountStrategy
import com.microsoft.identity.common.internal.broker.ipc.BoundServiceStrategy
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.util.AccountManagerUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.logging.Logger

// Functions to be invoked by both OneAuth and MSAL Android
// Making a change to any method signature is a breaking change.
class OneAuthSharedFunctions {

    companion object {
        val TAG = OneAuthSharedFunctions::class.java

        /**
         * Constructs a list of [IIpcStrategy] to communicate from
         * OneAuth/MSAL to Broker process.
         *
         * @param context [Context]
         * @param activeBrokerPackageName name of the app hosting the broker process to communicate to.
         **/
        @JvmStatic
        fun getIpcStrategies(
            context: Context,
            activeBrokerPackageName: String,
        ): List<IIpcStrategy> {
            val methodTag = "$TAG:getIpcStrategies"
            val strategies: MutableList<IIpcStrategy> = ArrayList()

            val sb = StringBuilder(100)
            sb.append("Broker Strategies added : ")
            val contentProviderStrategy = ContentProviderStrategy(context)
            if (contentProviderStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
                sb.append("ContentProviderStrategy, ")
                strategies.add(contentProviderStrategy)
            }

            val client = MicrosoftAuthClient(context)
            if (client.isBoundServiceSupported(activeBrokerPackageName)) {
                sb.append("BoundServiceStrategy, ")
                strategies.add(BoundServiceStrategy(client))
            }

            if (AccountManagerUtil.canUseAccountManagerOperation(
                    context, setOf(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)
                )
            ) {
                sb.append("AccountManagerStrategy.")
                strategies.add(AccountManagerAddAccountStrategy(context))
            }

            Logger.info(methodTag, sb.toString())
            return strategies
        }

    }
}
