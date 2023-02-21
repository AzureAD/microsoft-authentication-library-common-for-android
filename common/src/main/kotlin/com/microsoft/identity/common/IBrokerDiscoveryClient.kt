package com.microsoft.identity.common

import com.microsoft.identity.common.internal.broker.BrokerData

interface IBrokerDiscoveryClient {

    /**
     * Performs a discovery to figure out which broker app the SDK (MSAL/OneAuth)
     * has to send its request to.
     *
     * @param forceTrigger If true, this will skip cached value (if any)
     *                     and force triggering the broker election process.
     * @return BrokerData package name and signature hash of the targeted app.
     * **/
    fun getActiveBroker(forceTrigger: Boolean) : BrokerData?
}