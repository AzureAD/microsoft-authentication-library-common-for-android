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
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.cache.IClientActiveBrokerCache
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ClientException.ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE
import com.microsoft.identity.common.java.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit


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
 * @param isValidBroker                     a function to determine if the installed broker app contains a matching signature hash.
 **/
class BrokerDiscoveryClient(private val brokerCandidates: Set<BrokerData>,
                            private val getActiveBrokerFromAccountManager: () -> BrokerData?,
                            private val ipcStrategy: IIpcStrategy,
                            private val cache: IClientActiveBrokerCache,
                            private val isPackageInstalled: (BrokerData) -> Boolean,
                            private val isValidBroker: (BrokerData) -> Boolean) : IBrokerDiscoveryClient {

    companion object {
        val TAG = BrokerDiscoveryClient::class.simpleName

        @OptIn(ExperimentalCoroutinesApi::class)
        val dispatcher = Dispatchers.IO.limitedParallelism(10)

        const val ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY = "ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY"
        const val ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY = "ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY"
        const val ERROR_BUNDLE_KEY = "ERROR_BUNDLE_KEY"

        /**
         * Per-process Thread-safe, coroutine-safe Mutex of this class.
         * This is to prevent the IPC mechanism from being unnecessarily triggered due to race condition.
         *
         * The object here must be both coroutine-safe and thread-safe.
         **/
        private val classLevelLock = Mutex()

        /**
         * Performs an IPC operation to get a result from the provided [brokerCandidates].
         *
         * @param brokerCandidates          the candidate(s) to query from.
         * @param ipcStrategy               the ipc mechanism to query with.
         * @param isPackageInstalled        a method which returns true if the provided [BrokerData] is installed.
         * @param shouldStopQueryForAWhile  a method which, if invoked, will force [BrokerDiscoveryClient]
         *                                  to skip the IPC discovery process for a while.
         **/
        internal suspend fun queryFromBroker(brokerCandidates: Set<BrokerData>,
                                             ipcStrategy: IIpcStrategy,
                                             isPackageInstalled: (BrokerData) -> Boolean,
                                             isValidBroker: (BrokerData) -> Boolean
        ): BrokerData? {
            return coroutineScope {
                val installedCandidates = brokerCandidates.filter(isPackageInstalled).filter(isValidBroker)
                val deferredResults = installedCandidates.map { candidate ->
                    async(dispatcher) {
                        return@async makeRequest(candidate, ipcStrategy)
                    }
                }
                return@coroutineScope deferredResults.awaitAll().filterNotNull().firstOrNull()
            }
        }

        private fun makeRequest(candidate: BrokerData,
                                ipcStrategy: IIpcStrategy): BrokerData? {
            val methodTag = "$TAG:makeRequest"
            val operationBundle = BrokerOperationBundle(
                BrokerOperationBundle.Operation.BROKER_DISCOVERY_FROM_SDK,
                candidate.packageName,
                Bundle()
            )

            return try {
                val result = ipcStrategy.communicateToBroker(operationBundle)
                extractResult(result)
            } catch (t: Throwable) {
                if (t is BrokerCommunicationException &&
                    BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE == t.category) {
                    Logger.info(methodTag,
                        "Tried broker discovery on ${candidate}. It doesn't support the IPC mechanism.")
                } else if (t is ClientException && ONLY_SUPPORTS_ACCOUNT_MANAGER_ERROR_CODE == t.errorCode){
                    Logger.info(methodTag,
                        "Tried broker discovery on ${candidate}. " +
                                "The Broker side indicates that only AccountManager is supported.")
                } else {
                    Logger.error(methodTag,
                        "Tried broker discovery on ${candidate}, get an error", t)
                }
                null
            }
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

            val signatureHash = bundle.getString(ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY)?:
                throw NoSuchElementException("ACTIVE_BROKER_SIGNING_CERTIFICATE_THUMBPRINT_BUNDLE_KEY must not be null")

            return BrokerData(pkgName, signatureHash)
        }
    }

    constructor(context: Context,
                cache: IClientActiveBrokerCache): this(
        brokerCandidates = BrokerData.getKnownBrokerApps(),
        getActiveBrokerFromAccountManager = {
            AccountManagerBrokerDiscoveryUtil(context).getActiveBrokerFromAccountManager()
        },
        ipcStrategy = ContentProviderStrategy(context),
        cache = cache,
        isPackageInstalled = { brokerData ->
            PackageHelper(context).
            isPackageInstalledAndEnabled(brokerData.packageName)
        },
        isValidBroker = { brokerData ->
            BrokerValidator(context).isSignedByKnownKeys(brokerData)
        })

    override fun getActiveBroker(shouldSkipCache: Boolean): BrokerData? {
        return runBlocking {
            return@runBlocking getActiveBrokerAsync(shouldSkipCache)
        }
    }

    private suspend fun getActiveBrokerAsync(shouldSkipCache:Boolean): BrokerData?{
        val methodTag = "$TAG:getActiveBrokerAsync"
        classLevelLock.withLock {
            if (!shouldSkipCache) {
                if (cache.shouldUseAccountManager()) {
                    return getActiveBrokerFromAccountManager()
                }
                cache.getCachedActiveBroker()?.let {
                    if (!isPackageInstalled(it)) {
                        Logger.info(
                            methodTag,
                            "There is a cached broker: $it, but the app is no longer installed."
                        )
                        cache.clearCachedActiveBroker()
                        return@let
                    }

                    if (!isValidBroker(it)) {
                        Logger.info(
                            methodTag,
                            "Clearing cache as the installed app does not have a matching signature hash."
                        )
                        cache.clearCachedActiveBroker()
                        return@let
                    }

                    if(!ipcStrategy.isSupportedByTargetedBroker(it.packageName)){
                        Logger.info(
                            methodTag,
                            "Clearing cache as the installed app does not provide any IPC mechanism to communicate to. (e.g. the broker code isn't shipped)"
                        )
                        cache.clearCachedActiveBroker()
                        return@let
                    }

                    Logger.info(methodTag, "Returning cached broker: $it")
                    return it
                }
            }

            val brokerData = queryFromBroker(
                brokerCandidates = brokerCandidates,
                ipcStrategy = ipcStrategy,
                isPackageInstalled = isPackageInstalled,
                isValidBroker = isValidBroker
            )

            if (brokerData != null) {
                cache.setCachedActiveBroker(brokerData)
                return brokerData
            }

            Logger.info(
                methodTag,
                "Will skip broker discovery via IPC and fall back to AccountManager " +
                        "for the next 60 minutes."
            )
            cache.clearCachedActiveBroker()
            cache.setShouldUseAccountManagerForTheNextMilliseconds(
                TimeUnit.MINUTES.toMillis(
                    60
                )
            )

            val accountManagerResult = getActiveBrokerFromAccountManager()
            Logger.info(
                methodTag, "Tried getting active broker from account manager, " +
                        "get ${accountManagerResult?.packageName}."
            )

            return accountManagerResult
        }
    }
}