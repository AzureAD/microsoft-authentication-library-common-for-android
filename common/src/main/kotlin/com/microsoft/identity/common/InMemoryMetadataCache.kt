package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

// For testing only
class InMemoryMetadataCache: IBrokerMetadataCache {
    var cacheData : BrokerData? = null

    override fun getCachedActiveBroker(): BrokerData? {
        return cacheData
    }

    override fun setCachedActiveBroker(brokerData: BrokerData) {
        cacheData = brokerData
    }

    override fun clearCachedActiveBroker() {
        cacheData = null
    }
}