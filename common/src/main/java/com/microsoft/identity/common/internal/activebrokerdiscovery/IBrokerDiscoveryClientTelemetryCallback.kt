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

interface IBrokerDiscoveryClientTelemetryCallback {
    /**
     * Time spent acquiring the lock/mutex for broker discovery.
     *
     * @param timeSpentInNanoSeconds Time spent acquiring the lock/mutex (in nanoseconds).
     **/
    fun onLockAcquired(timeSpentInNanoSeconds: Long)

    /**
     * If triggered, the broker discovery client is using Account Manager
     * (e.g. one of the installed broker is too old and doesn't support this new mechanism).
     **/
    fun onUseAccountManager()

    /**
     * Triggered when BrokerDiscoveryClient finishes reading from its cache.
     *
     * @param timeSpentInNanoSeconds Time spent reading the cache (in nanoseconds).
     **/
    fun onReadFromCache(timeSpentInNanoSeconds: Long)

    /**
     * Triggered when BrokerDiscoveryClient finishes validating if the cached broker package is installed.
     *
     * @param timeSpentInNanoSeconds Time spent to validate if the package is installed (in nanoseconds).
     **/
    fun onFinishCheckingIfPackageIsInstalled(timeSpentInNanoSeconds: Long)

    /**
     * Triggered when BrokerDiscoveryClient finishes validating if the cached broker package supports the given IPC strategy.
     *
     * @param timeSpentInNanoSeconds Time spent to validate if the package supports the IPC strategy (in nanoseconds).
     **/
    fun onFinishCheckingIfSupportedByTargetedBroker(timeSpentInNanoSeconds: Long)

    /**
     * Triggered when BrokerDiscoveryClient finishes validating if the cached broker package is a valid broker.
     *
     * @param timeSpentInNanoSeconds Time spent to validate if the package is a valid broker (in nanoseconds).
     **/
    fun onFinishCheckingIfValidBroker(timeSpentInNanoSeconds: Long)

    /**
     * Triggered when BrokerDiscoveryClient finishes querying active broker from one of the broker apps.
     *
     * There are 2 possible cases when this is triggered.
     * 1. Both Broker and this MSAL/OneAuth app are freshly installed.
     * This will trigger the broker discovery in broker, and will take longer time.
     *
     * 2. Broker has already done broker discovery before (e.g. by other MSAL/OneAuth app). Only this MSAL/OneAuth app is freshly installed.
     * When the request reaches the broker, the broker would just return the cached value, which will be faster.
     *
     * @param timeSpentInNanoSeconds Time spent to query the active broker (in nanoseconds).
     **/
    fun onFinishQueryingResultFromBroker(timeSpentInNanoSeconds: Long)
}
