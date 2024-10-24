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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.internal.broker.BrokerData
import javax.annotation.concurrent.ThreadSafe


/**
 * An interface for accessing cached [BrokerData].
 * This interface must be thread-safe.
 */
@ThreadSafe
interface IActiveBrokerCache {

    /**
     * Gets the active broker from the cache.
     */
    fun getCachedActiveBroker(): BrokerData?

    /**
     * Persists the active broker to the cache.
     *
     * @param brokerData the active [BrokerData] to persist.
     */
    fun setCachedActiveBroker(brokerData: BrokerData)

    /**
     * Clears the active broker from the cache.
     */
    fun clearCachedActiveBroker()

    /**
     * Returns true if AccountManager should still be used.
     **/
    fun shouldUseAccountManager(): Boolean

    /**
     * Set the time span when AccountManager should still be used.
     *
     * @param timeInMillis Time in milliseconds (from now)
     **/
    fun setShouldUseAccountManagerForTheNextMilliseconds(timeInMillis: Long)
}