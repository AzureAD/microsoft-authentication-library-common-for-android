package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

interface IBrokerMetadataCache {

    fun getCachedActiveBroker(): BrokerData?

    /**
     * Persists the active broker and protocol version to the cache.
     * Do not invoke this if the result is obtained via AccountManager.
     */
    fun setCachedActiveBroker(brokerData: BrokerData)
//    fun setCachedActiveBroker(brokerData: BrokerData, protocolVersion: String)

    fun clearCachedActiveBroker()

//    fun getKnownProtocolVersion(): String?
}