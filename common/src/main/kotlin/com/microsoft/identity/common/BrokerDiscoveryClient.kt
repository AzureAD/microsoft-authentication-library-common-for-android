package com.microsoft.identity.common

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class BrokerDiscoveryClient(private val brokerCandidates: Set<BrokerData>,
                            private val getActiveBrokerFromAccountManager: () -> BrokerData?,
                            private val ipcStrategy: IIpcStrategy,
                            private val cache: BrokerMetadataCache,
                            private val isPackageInstalled: (BrokerData) -> Boolean): IBrokerDiscoveryClient {

    companion object{
        val TAG = BrokerDiscoveryClient::class.simpleName



        private val BROKER_CANDIDATE_SET = setOf(
            BrokerData.MOCK_LTW,
            BrokerData.MOCK_AUTHAPP,
            BrokerData.MOCK_CP,
        )

        const val ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY = "ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY"
        const val ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY = "ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY"
        const val BENCHMARK_STRING_BUNDLE_KEY = "BENCHMARK_STRING_BUNDLE_KEY"
        const val ERROR_BUNDLE_KEY = "ERROR_BUNDLE_KEY"
        const val FORCE_DISCOVERY_BUNDLE_KEY = "FORCE_DISCOVERY_BUNDLE_KEY"

        // TODO: add tests on this.
        fun getActiveBrokerFromAccountManagerFunc(context: Context) : () -> BrokerData? {
            return {
                val activeBroker = BrokerValidator(context).currentActiveBrokerPackageName
                activeBroker?.let {
                    BrokerData(activeBroker,
                        PackageHelper(context.packageManager).getCurrentSignatureForPackage(activeBroker))
                }
            }
        }

        @SuppressLint("SimpleDateFormat")
        fun getCurrentTime(): String {
            val date = Date(System.currentTimeMillis())
            val formatter = SimpleDateFormat("hh:mm:ss.SSS");
            return formatter.format(date)
        }

        private fun queryFromBroker(brokerCandidates: Set<BrokerData>,
                                    ipcStrategy: IIpcStrategy,
                                    isPackageInstalled: (BrokerData) -> Boolean,
                                    forceTrigger: Boolean): BrokerData? {
            val methodTag = "$TAG:queryFromBroker"

            val bundle = Bundle()
            bundle.putBoolean(FORCE_DISCOVERY_BUNDLE_KEY, forceTrigger)

            // ping all candidates to frontload bootstrapping!
            val installedCandidates = brokerCandidates.filter(isPackageInstalled)

            val list = mutableListOf<Deferred<Unit>>()
            runBlocking {
                for (candidate in installedCandidates) {
                    val operationBundle = BrokerOperationBundle(
                        BrokerOperationBundle.Operation.BROKER_EMPTY_REQUEST_TEST,
                        candidate.packageName,
                        bundle
                    )
                    list.add(async(Dispatchers.IO) {
                        try {
                            ipcStrategy.communicateToBroker(operationBundle)
                        } catch (e: Exception) {
                            // do nothing.
                        }

                        return@async;
                    })
                }
                list.awaitAll()
            }

            try {
                Logger.warn(methodTag, "[${getCurrentTime()}] Starting IPC process")
                val operationBundle = BrokerOperationBundle(
                    BrokerOperationBundle.Operation.MSAL_BROKER_DISCOVERY,
                    installedCandidates.first().packageName,
                    bundle
                )
                val result = ipcStrategy.communicateToBroker(operationBundle)
                if (isErrorBundle(result)) {
                    throw result.getSerializable(ERROR_BUNDLE_KEY) as Throwable
                }

                val packageName = result.getString(ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY)!!
                val signatureHash =
                    result.getString(ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY)!!
                Logger.warn(methodTag, result.getString(BENCHMARK_STRING_BUNDLE_KEY, ""))
                Logger.info(methodTag, "Acquired Active broker from ${installedCandidates.first().packageName}. The active broker is $packageName")
                return BrokerData(packageName, signatureHash)
            } catch (e: BrokerCommunicationException) {
                // handle the scenario where the broker is too old.
                if (BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE == e.category) {
                    // log!
                }
            }

            return null
        }

        fun isErrorBundle(result: Bundle): Boolean {
            return result.containsKey(ERROR_BUNDLE_KEY)
        }
    }

    constructor(context: Context) : this(
        brokerCandidates = BROKER_CANDIDATE_SET,
        getActiveBrokerFromAccountManager = getActiveBrokerFromAccountManagerFunc(context),
        ipcStrategy = ContentProviderStrategy(context),
        cache = BrokerMetadataCache(context),
        isPackageInstalled = { brokerData ->
            PackageHelper.isPackageInstalledAndEnabled(context, brokerData.packageName)
        })

    override fun getActiveBroker(forceTrigger: Boolean): BrokerData? {
        val methodTag = "$TAG:getActiveBroker"

        if (!forceTrigger){
            val cachedData = cache.getCachedActiveBroker()
            if (cachedData != null){
                Logger.info(methodTag, "Returning cached broker: ${cachedData.packageName}")
                return cachedData
            }
        }

        var brokerData = queryFromBroker(brokerCandidates = brokerCandidates,
            ipcStrategy = ipcStrategy,
            forceTrigger = forceTrigger,
            isPackageInstalled = isPackageInstalled)

        if(brokerData == null) {
            brokerData = getActiveBrokerFromAccountManager()
            Logger.info(methodTag, "Tried getting active broker from account manager, get ${brokerData?.packageName}.")
        }

        if (brokerData == null){
            Logger.info(methodTag, "Broker not found.")
            cache.clearCachedActiveBroker()
            return null
        }

        cache.setCachedActiveBroker(brokerData)
        return brokerData
    }
}