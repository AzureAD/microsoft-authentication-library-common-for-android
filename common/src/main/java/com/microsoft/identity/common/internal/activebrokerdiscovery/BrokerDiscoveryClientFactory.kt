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

import android.content.Context
import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache
import com.microsoft.identity.common.internal.cache.IClientActiveBrokerCache
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A class for initializing a new [IBrokerDiscoveryClient] object.
 **/
class BrokerDiscoveryClientFactory {

    companion object {

        private val TAG = BrokerDiscoveryClientFactory::class.simpleName

        @Volatile
        private var IS_NEW_DISCOVERY_ENABLED = true

        @Volatile
        private var clientSdkInstance: IBrokerDiscoveryClient? = null

        @Volatile
        private var brokerSdkInstance: IBrokerDiscoveryClient? = null

        /**
         * Coroutine-level lock.
         **/
        private val lock = Mutex()

        /**
         * If set to true, the new Broker discovery mechanism will be enabled.
         * This is currently turned off by default - until we're ready to ship the feature.
         **/
        @JvmStatic
        fun setNewBrokerDiscoveryEnabled(isEnabled: Boolean){
            // If the flag changes, wipe the existing singleton.
            if (isEnabled != IS_NEW_DISCOVERY_ENABLED) {
                clientSdkInstance = null
                brokerSdkInstance = null
                IS_NEW_DISCOVERY_ENABLED = isEnabled
            }
        }

        /**
         * Returns true if the new Broker discovery mechanism is enabled.
         **/
        @JvmStatic
        fun isNewBrokerDiscoveryEnabled(): Boolean {
            return BuildConfig.newBrokerDiscoveryEnabledFlag || IS_NEW_DISCOVERY_ENABLED;
        }

        /**
         * Initializes a new [IBrokerDiscoveryClient] object.
         **/
        @JvmStatic
        fun getInstanceForClientSdk(context: Context,
                                    platformComponents: IPlatformComponents) : IBrokerDiscoveryClient{
            if (clientSdkInstance == null){
                runBlocking {
                    lock.withLock {
                        if (clientSdkInstance == null) {
                            clientSdkInstance = getInstance(context,
                                platformComponents,
                                ClientActiveBrokerCache.getClientSdkCache(platformComponents.storageSupplier))
                        }
                    }
                }
            }
            return clientSdkInstance!!
        }

        /**
         * Initializes a new [IBrokerDiscoveryClient] object
         * to be used by Broker API, WPJ API.
         **/
        @JvmStatic
        fun getInstanceForBrokerSdk(context: Context,
                                    platformComponents: IPlatformComponents) : IBrokerDiscoveryClient{
            if (brokerSdkInstance == null){
                runBlocking {
                    lock.withLock {
                        if (brokerSdkInstance == null) {
                            brokerSdkInstance = getInstance(context,
                                platformComponents,
                                ClientActiveBrokerCache.getBrokerSdkCache(platformComponents.storageSupplier))
                        }
                    }
                }
            }
            return brokerSdkInstance!!
        }

        /**
         * Initializes a new [IBrokerDiscoveryClient] object.
         * to be used by OneAuth/MSAL.
         **/
        @JvmStatic
        private fun getInstance(context: Context,
                                platformComponents: IPlatformComponents,
                                cache: IClientActiveBrokerCache) : IBrokerDiscoveryClient{
            val methodTag = "$TAG:getInstance"
            return if (isNewBrokerDiscoveryEnabled()) {
                Logger.info(methodTag, "Broker Discovery is enabled. Use the new logic on the SDK side")
                BrokerDiscoveryClient(context, platformComponents, cache)
            } else {
                Logger.info(methodTag, "Broker Discovery is disabled. Use AccountManager on the SDK side.")
                LegacyBrokerDiscoveryClient(context)
            }
        }
    }
}
