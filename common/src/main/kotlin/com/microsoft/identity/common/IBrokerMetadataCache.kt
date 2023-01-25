package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

interface IBrokerMetadataCache {

    fun getCachedActiveBroker(): BrokerData?

    fun setCachedActiveBroker(brokerData: BrokerData)

    fun clearCachedActiveBroker()
}