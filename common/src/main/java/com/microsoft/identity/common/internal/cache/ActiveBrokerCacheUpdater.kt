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
package com.microsoft.identity.common.internal.cache

import android.content.Context
import android.os.Bundle
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * With the new broker selection mechanism, it is possible that the active broker that actually
 * executes the request is NOT the one MSAL/ADAL forwards the request to.
 *
 * This could happen when the cache on the SDK side is outdated.
 *
 * When this happens, the broker will make sure that the request is properly handled by the right broker app,
 * and then it would return the response bundle back with the information of the right active broker app.
 *
 * On the SDK side, we'll need to update the cache.
 *
 * This class exists to facilitate the "packing" and "unpacking" active broker app on the response bundle.
 * */
class ActiveBrokerCacheUpdater(
    private val isValidBroker: (BrokerData) -> Boolean,
    private val cache: IClientActiveBrokerCache) {

    constructor(context: Context, cache: IClientActiveBrokerCache): this(
        isValidBroker = { brokerData: BrokerData ->
            BrokerValidator(context).isSignedByKnownKeys(brokerData)
        },
        cache = cache
    )

    companion object {
        private val TAG = ActiveBrokerCacheUpdater::class.simpleName

        /**
         * ACTIVE BROKER KEYS (Use these keys to retrieve the active broker that executed the request)
         * Do NOT change the value of these parameters (it's a breaking change!)
         */
        const val ACTIVE_BROKER_PACKAGE_NAME_KEY = "active.broker.package.name"
        const val ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY = "active.broker.signing.certificate.thumbprint"

        /**
         * Keys indicating that the "broker discovery" feature is turned off on the Broker side.
         * (and therefore the SDK should fall back to using Account Manager to figure out which app is the active broker.)
         *
         * Do NOT change the value of these parameters (it's a breaking change!)
         */
        const val BROKER_DISCOVERY_DISABLED_KEY = "broker.discovery.disabled"

        /**
         * If this key is present in the request bundle,
         * The broker will return the active broker information in the result bundle.
         * Note: This is only valid for requests that are processed by the BrokerOperationRequestDispatcher.
         */
        const val REQUEST_ACTIVE_BROKER_DATA_KEY = "com.microsoft.identity.request.broker.data"

        /**
         * Adds the active broker information to the result bundle.
         *
         * @param bundle       The result bundle.
         * @param activeBroker The active broker that will execute the request.
         */
        @JvmStatic
        fun appendActiveBrokerToResultBundle(bundle: Bundle, activeBroker: BrokerData) {
            bundle.putString(ACTIVE_BROKER_PACKAGE_NAME_KEY,
                activeBroker.packageName)
            bundle.putString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY,
                activeBroker.signingCertificateThumbprint)
        }

        /**
         * Adds the "broker discovery is now disabled" to the result bundle.
         *
         * @param bundle       The result bundle.
         */
        @JvmStatic
        fun appendBrokerDiscoveryDisabledToResultBundle(bundle: Bundle) {
            bundle.putBoolean(BROKER_DISCOVERY_DISABLED_KEY, true)
        }
    }

    /**
     * If the active broker is returned with the result bundle, update [IActiveBrokerCache] with it.
     * If the active broker indicates that the broker discovery is disabled, wipe [IActiveBrokerCache].
     *
     * @param bundle    The result bundle. could be null (for backward compatibility).
     */
    fun updateCachedActiveBrokerFromResultBundle(bundle: Bundle?) {
        val methodTag = "$TAG:updateCachedActiveBrokerFromResultBundle"
        if (bundle == null) {
            return
        }

        val shouldWipeCachedActiveBroker = bundle.getBoolean(BROKER_DISCOVERY_DISABLED_KEY, false)
        if (shouldWipeCachedActiveBroker) {
            Logger.info(methodTag, "Got a response indicating that the broker discovery is disabled." +
                    "Will also wipe the local active broker cache," +
                    "and skip broker discovery via IPC (only fall back to AccountManager) for the next 60 minutes.")
            cache.clearCachedActiveBroker()
            cache.setShouldUseAccountManagerForTheNextMilliseconds(
                TimeUnit.MINUTES.toMillis(60)
            )
            return
        }

        val packageName = bundle.getString(ACTIVE_BROKER_PACKAGE_NAME_KEY)
        val signingCertThumbprint = bundle.getString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_KEY)

        if (packageName.isNullOrEmpty() || signingCertThumbprint.isNullOrEmpty()) {
            Logger.info(methodTag, "A response was received without active broker information.")
            return
        }

        val brokerData = BrokerData(packageName, signingCertThumbprint)
        if (!isValidBroker(brokerData)) {
            Logger.warn(methodTag, "Cannot find an installed $packageName with a matching " +
                    "signing certificate thumbprint.")
            return
        }

        cache.setCachedActiveBroker(brokerData)
    }
}
