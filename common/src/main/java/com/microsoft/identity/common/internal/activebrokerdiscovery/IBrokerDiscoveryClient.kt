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

import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.exception.ClientException

interface IBrokerDiscoveryClient {

    /**
     * Performs a discovery to figure out which broker app the SDK (MSAL/OneAuth)
     * has to send its request to.
     *
     * @param shouldSkipCache If true, this will skip cached value (if any)
     *                        and always query the broker for the result.
     * @return BrokerData package name and signature hash of the targeted app.
     * **/
    fun getActiveBroker(shouldSkipCache: Boolean = false) : BrokerData?

    /**
     * Force a broker app to figure out which broker app the SDK (MSAL/OneAuth)
     * has to send its request to.
     *
     * This method will ignore the cache values on both SDK and broker side.
     *
     * Due to the nature of the method, the Android broker team will limit which app can invoke this method,
     * and what circumstance it's allowed to do so (e.g. flight enabled for a certain tenant only).
     * Please consult the Android broker team before use.
     *
     * @return BrokerData package name and signature hash of the targeted app.
     * **/
    @kotlin.jvm.Throws(ClientException::class)
    fun forceBrokerRediscovery(brokerCandidate: BrokerData): BrokerData
}