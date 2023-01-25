package com.microsoft.identity.common

import android.content.Context
import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.logging.Logger

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

        fun getActiveBrokerFromAccountManagerFunc(context: Context) : () -> BrokerData? {
            return {
                val activeBroker = BrokerValidator(context).currentActiveBrokerPackageName
                if (activeBroker != null){
                    BrokerData(activeBroker,
                        PackageHelper(context.packageManager).getCurrentSignatureForPackage(activeBroker))
                }
                null
            }
        }

        private fun queryFromBroker(brokerCandidates: Set<BrokerData>,
                                    ipcStrategy: IIpcStrategy,
                                    isPackageInstalled: (BrokerData) -> Boolean): BrokerData? {
            val methodTag = "$TAG:queryFromBroker"
            for (candidate in brokerCandidates) {
                if (!isPackageInstalled(candidate)) {
                    continue
                }

                try {
                    val operationBundle = BrokerOperationBundle(
                        BrokerOperationBundle.Operation.MSAL_BROKER_DISCOVERY,
                        candidate.packageName,
                        Bundle()
                    )
                    val result = ipcStrategy.communicateToBroker(operationBundle)
                    val packageName = result.getString(ACTIVE_BROKER_PACKAGE_NAME_BUNDLE_KEY)!!
                    val signatureHash = result.getString(ACTIVE_BROKER_SIGNATURE_HASH_BUNDLE_KEY)!!
                    Logger.info(methodTag, "Acquired Active broker from ${candidate.packageName}. The active broker is $packageName")
                    return BrokerData(packageName, signatureHash)
                } catch (e: BrokerCommunicationException) {
                    // handle the scenario where the broker is too old.
                    if (BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE == e.category) {
                        continue
                    }
                }
            }

            return null
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

    override fun getActiveBroker(): BrokerData? {
        val methodTag = "$TAG:getActiveBroker"

        val cachedData = cache.getCachedActiveBroker()
        if (cachedData != null){
            Logger.info(methodTag, "Returning cached broker: ${cachedData.packageName}")
            return cachedData
        }

        var brokerData = queryFromBroker(brokerCandidates = brokerCandidates,
            ipcStrategy = ipcStrategy,
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