package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

// For testing only
class NoOpMetadataCache: IBrokerMetadataCache {

    override fun getCachedActiveBroker(): BrokerData? {
        return null
    }

    override fun setCachedActiveBroker(brokerData: BrokerData) {
        // Do nothing
    }

    override fun clearCachedActiveBroker() {
        // Do nothing
    }
}