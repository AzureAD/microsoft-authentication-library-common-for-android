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
import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.cache.ActiveBrokerCache
import com.microsoft.identity.common.internal.cache.IActiveBrokerCache
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.logging.Logger


/**
 * A class for figuring out which Broker app the caller should communicate with.
 *
 * This class will try pinging each installed apps provided in [brokerCandidates], which will
 * essentially trigger Broker Discovery on that side (and subsequently returned result).
 *
 * If none of the installed app supports the Broker Discovery protocol, this class will fall back
 * to the legacy AccountManager method.
 *
 * @param brokerCandidates                  list of (Broker hosting) candidate apps to perform discovery with.
 * @param getActiveBrokerFromAccountManager a function which returns a validated [BrokerData]
 *                                          based on Android's AccountManager API
 * @param ipcStrategy                       An [IIpcStrategy] to aggregate data with.
 * @param cache                             A local cache for storing active broker discovery results.
 * @param isPackageInstalled                a function to determine if any given broker app is installed.
 **/
class BrokerDiscoveryClient(private val brokerCandidates: Set<BrokerData>,
                            private val getActiveBrokerFromAccountManager: () -> BrokerData?,
                            private val ipcStrategy: IIpcStrategy,
                            private val cache: IActiveBrokerCache,
                            private val isPackageInstalled: (BrokerData) -> Boolean) : IBrokerDiscoveryClient {

    companion object {
        val TAG = BrokerDiscoveryClient::class.simpleName

        const val ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY = "ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY"
        const val ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY = "ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY"
        const val ERROR_BUNDLE_KEY = "ERROR_BUNDLE_KEY"

        /**
         * Performs an IPC operation to get a result from the provided [brokerCandidates].
         *
         * @param brokerCandidates      the candidate(s) to query from.
         * @param ipcStrategy           the ipc mechanism to query with.
         * @param isPackageInstalled    a method which returns true if the provided [BrokerData] is installed.
         **/
        internal fun queryFromBroker(brokerCandidates: Set<BrokerData>,
                                     ipcStrategy: IIpcStrategy,
                                     isPackageInstalled: (BrokerData) -> Boolean): BrokerData? {
            val methodTag = "$TAG:queryFromBroker"

            val installedCandidates = brokerCandidates.filter(isPackageInstalled)

            installedCandidates.forEach { candidate ->
                val operationBundle = BrokerOperationBundle(
                    BrokerOperationBundle.Operation.BROKER_DISCOVERY_FROM_SDK,
                    candidate.packageName,
                    Bundle()
                )

                try {
                    val result = ipcStrategy.communicateToBroker(operationBundle)
                    return extractResult(result)
                } catch (t: Throwable) {
                    if (t is BrokerCommunicationException &&
                        BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE == t.category) {
                        Logger.info(methodTag, "Tried broker discovery on ${candidate}. It doesn't support the operation")
                    } else {
                        Logger.error(methodTag, "Tried broker discovery on ${candidate}, get an error", t)
                    }
                }
            }

            return null
        }

        /**
         * Extract the result returned via the IPC operation
         **/
        @Throws(NoSuchElementException::class)
        private fun extractResult(bundle: Bundle?): BrokerData? {
            if (bundle == null) {
                return null
            }

            val errorData = bundle.getSerializable(ERROR_BUNDLE_KEY)
            if (errorData != null) {
                throw errorData as Throwable
            }

            val pkgName = bundle.getString(ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY)?:
                throw NoSuchElementException("ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY must not be null")

            val signatureHash = bundle.getString(ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY)?:
                throw NoSuchElementException("ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY must not be null")

            return BrokerData(pkgName, signatureHash)
        }
    }

    constructor(context: Context, components: IPlatformComponents): this(
        brokerCandidates = BrokerData.getKnownBrokerApps(),
        getActiveBrokerFromAccountManager = {
            AccountManagerBrokerDiscoveryUtil(context).getActiveBrokerFromAccountManager()
        },
        ipcStrategy = ContentProviderStrategy(context),
        cache = ActiveBrokerCache.getBrokerMetadataStoreOnSdkSide(components.storageSupplier),
        isPackageInstalled = { brokerData ->
            PackageHelper(context).
                isPackageInstalledAndEnabled(brokerData.packageName)
        })

    override fun getActiveBroker(shouldSkipCache: Boolean): BrokerData? {
        val methodTag = "$TAG:getActiveBroker"

        if (!shouldSkipCache){
            val cachedData = cache.getCachedActiveBroker()
            cachedData?.let {
                if (isPackageInstalled(cachedData)){
                    Logger.info(methodTag, "Returning cached broker: $cachedData")
                    return cachedData
                } else {
                    Logger.info(methodTag, "There is a cached broker: $cachedData, but the app is no longer installed.")
                    cache.clearCachedActiveBroker()
                }
            }
        }

        var brokerData = queryFromBroker(
                brokerCandidates = brokerCandidates,
                ipcStrategy = ipcStrategy,
                isPackageInstalled = isPackageInstalled)

        if(brokerData == null) {
            brokerData = getActiveBrokerFromAccountManager()
            Logger.info(methodTag, "Tried getting active broker from account manager, " +
                    "get ${brokerData?.packageName}.")
        }

        if (brokerData == null){
            Logger.info(methodTag, "Broker not found.")
            return null
        }

        cache.setCachedActiveBroker(brokerData)
        return brokerData
    }

}