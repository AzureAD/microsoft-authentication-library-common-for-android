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
package com.microsoft.identity.deviceregistration

import android.content.Context
import com.microsoft.identity.client.IDeviceRegistrationService
import com.microsoft.identity.common.internal.broker.ipc.BoundServiceStrategy
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.DeviceRegistrationServiceClient
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger

/**
 * Provides IPC strategies for device registration communication.
 *
 * Default implementation supplies ContentProvider + BoundService strategies.
 * Broker subclass overrides [getStrategies] to add WpjLegacyAccountAuthenticatorStrategy.
 *
 * @param supportsBoundService whether to include the BoundService IPC strategy.
 */
open class DeviceRegistrationIpcStrategiesProvider @JvmOverloads constructor(
    val supportsBoundService: Boolean = true
) {
    companion object {
        private val TAG = DeviceRegistrationIpcStrategiesProvider::class.java.simpleName
    }

    /**
     * Returns the list of IPC strategies to use for device registration.
     * Default: ContentProvider + BoundService (if supported).
     *
     * Broker overrides this to append legacy strategy after calling super.
     *
     * @param context                  application context.
     * @param components               platform components for the active broker.
     * @param activeBrokerPackageName  package name of the active broker.
     * @return ordered list of IPC strategies.
     */
    open fun getStrategies(
        context: Context,
        components: IPlatformComponents,
        activeBrokerPackageName: String
    ): MutableList<IIpcStrategy> {
        val methodTag = "$TAG:getStrategies"
        val strategies = mutableListOf<IIpcStrategy>()

        val contentProviderStrategy = ContentProviderStrategy(context, components)
        if (contentProviderStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
            Logger.info(methodTag, "Adding primary strategy: ${ContentProviderStrategy::class.java.simpleName}")
            //strategies.add(contentProviderStrategy)
        }

        val boundServiceStrategy = BoundServiceStrategy<IDeviceRegistrationService>(
            DeviceRegistrationServiceClient(context)
        )
        if (boundServiceStrategy.isSupportedByTargetedBroker(activeBrokerPackageName)) {
            Logger.info(methodTag, "Adding fallback strategy: ${BoundServiceStrategy::class.java.simpleName}")
            strategies.add(boundServiceStrategy)
        }

        return strategies
    }
}
