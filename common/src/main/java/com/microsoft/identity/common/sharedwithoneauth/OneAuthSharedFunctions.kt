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
import androidx.annotation.WorkerThread
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerAddAccountStrategy
import com.microsoft.identity.common.internal.broker.ipc.BoundServiceStrategy
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.broker.ipc.IpcStrategyWithRetry
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
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
            return getIpcStrategies(context,
                AndroidPlatformComponentsFactory.createFromContext(context),
                activeBrokerPackageName)
        }

        /**
         * Constructs a list of [IIpcStrategy] to communicate from
         * OneAuth/MSAL to Broker process.
         *
         * @param context [Context]
         * @param components [IPlatformComponents]
         * @param activeBrokerPackageName name of the app hosting the broker process to communicate to.
         **/
        @JvmStatic
        fun getIpcStrategies(
            context: Context,
            components: IPlatformComponents,
            activeBrokerPackageName: String,
        ): List<IIpcStrategy> {
            val methodTag = "$TAG:getIpcStrategies"
            val strategies: MutableList<IIpcStrategy> = ArrayList()

            val sb = StringBuilder(100)
            sb.append("Broker Strategies added : ")
            val contentProviderStrategy = ContentProviderStrategy(context, components)
            if (contentProviderStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
                sb.append("ContentProviderStrategy, ")
                strategies.add(maybeWrapWithRetry(methodTag, contentProviderStrategy))
            }

            val boundServiceStrategy = BoundServiceStrategy(MicrosoftAuthClient(context))
            if (boundServiceStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
                sb.append("BoundServiceStrategy, ")
                strategies.add(maybeWrapWithRetry(methodTag, boundServiceStrategy))
            }

            val accountManagerStrategy = AccountManagerAddAccountStrategy(context)
            if (accountManagerStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
                sb.append("AccountManagerStrategy.")
                strategies.add(maybeWrapWithRetry(methodTag, accountManagerStrategy))
            }

            Logger.info(methodTag, sb.toString())
            return strategies
        }

        /**
         * Wraps the given [IIpcStrategy] with [IpcStrategyWithRetry] if the
         * [CommonFlight.IPC_RETRY_ENABLED] flight is enabled; otherwise returns the strategy as-is.
         */
        internal fun maybeWrapWithRetry(methodTag: String, strategy: IIpcStrategy): IIpcStrategy {
            val flightsProvider = CommonFlightsManager.getFlightsProvider()
            return if (flightsProvider.isFlightEnabled(CommonFlight.IPC_RETRY_ENABLED)) {
                val maxRetries = maxOf(0, flightsProvider.getIntValue(CommonFlight.IPC_RETRY_MAX_ATTEMPTS))
                val baseDelayMs = maxOf(0L, flightsProvider.getIntValue(CommonFlight.IPC_RETRY_BASE_DELAY_MS).toLong())
                Logger.info(methodTag, "IPC retry enabled, wrapping strategies")
                IpcStrategyWithRetry(strategy, maxRetries, baseDelayMs)
            } else {
                strategy
            }
        }

    }
}
