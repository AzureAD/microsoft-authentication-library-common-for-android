package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

interface IBrokerDiscoveryClient {
    fun getActiveBroker(forceTrigger: Boolean) : BrokerData?
}