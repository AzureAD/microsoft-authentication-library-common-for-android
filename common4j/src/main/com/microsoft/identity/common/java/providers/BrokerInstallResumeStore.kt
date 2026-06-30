// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.providers

/**
 * Store for [BrokerInstallResumeRequest]s persisted before a broker install and resumed afterwards.
 *
 * Contract: entries are **single-use** (consumed on read) and **TTL-bounded** (an expired entry is
 * never returned and is purged). Implementations must be safe for concurrent access and must back
 * persistence with an encrypted store on Android. Carries no secrets (see [BrokerInstallResumeRequest]).
 */
interface BrokerInstallResumeStore {
    /** Saves [request], overwriting any existing entry with the same correlation id. */
    fun save(request: BrokerInstallResumeRequest)

    /**
     * Consumes the request for [correlationId]: returns it then removes it (single-use). Returns
     * null when absent or expired (expired entries are purged on access).
     */
    fun consume(correlationId: String, nowMs: Long): BrokerInstallResumeRequest?
}

/**
 * In-memory, thread-safe [BrokerInstallResumeStore] used for the POC and unit tests. Production
 * Android implementations should persist to an encrypted store scoped to the broker signature.
 */
class InMemoryBrokerInstallResumeStore : BrokerInstallResumeStore {

    private val lock = Any()
    private val entries = HashMap<String, BrokerInstallResumeRequest>()

    override fun save(request: BrokerInstallResumeRequest) {
        synchronized(lock) { entries[request.correlationId] = request }
    }

    override fun consume(correlationId: String, nowMs: Long): BrokerInstallResumeRequest? {
        synchronized(lock) {
            val request = entries.remove(correlationId) ?: return null
            return if (request.isExpired(nowMs)) null else request
        }
    }

    companion object {
        /**
         * Process-wide store shared between the persist (pre-install) and resume (post-install)
         * paths within a single app process. POC convenience only — production should inject an
         * encrypted, persistent implementation.
         */
        @JvmField
        val INSTANCE: BrokerInstallResumeStore = InMemoryBrokerInstallResumeStore()
    }
}
