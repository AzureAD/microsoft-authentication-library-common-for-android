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
package com.microsoft.identity.common.internal.providers

import android.content.Context
import com.google.gson.Gson
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.BrokerInstallResumeRequest
import com.microsoft.identity.common.java.providers.BrokerInstallResumeStore

/**
 * Encrypted, persistent [BrokerInstallResumeStore] for Android.
 *
 * Unlike the in-memory store, this implementation survives process death within the TTL window:
 * the persist (pre-install) and resume (post-install) paths run in the **same** app but may be in
 * different process lifetimes, so the snapshot must outlive the originating activity/process.
 *
 * Backed by the platform's encrypted name-value storage (the same primitive used by the token
 * cache and active-broker cache), so the snapshot is encrypted-at-rest. The snapshot itself carries
 * no secrets (see [BrokerInstallResumeRequest]); encryption is defense-in-depth for the loginHint
 * and request parameters.
 *
 * Contract is identical to the interface: entries are **single-use** (removed on read) and
 * **TTL-bounded** (expired entries are purged and never returned).
 */
class EncryptedBrokerInstallResumeStore
internal constructor(
    private val storage: INameValueStorage<String>,
    private val gson: Gson = Gson()
) : BrokerInstallResumeStore {

    private val lock = Any()

    override fun save(request: BrokerInstallResumeRequest) {
        synchronized(lock) {
            storage.put(request.correlationId, gson.toJson(request))
        }
    }

    override fun consume(correlationId: String, nowMs: Long): BrokerInstallResumeRequest? {
        synchronized(lock) {
            val json = storage.get(correlationId) ?: return null
            // Single-use: remove regardless of expiry outcome.
            storage.remove(correlationId)
            return try {
                val request = gson.fromJson(json, BrokerInstallResumeRequest::class.java)
                if (request == null || request.isExpired(nowMs)) null else request
            } catch (e: Exception) {
                Logger.warn(TAG, "Failed to deserialize persisted resume request; dropping.")
                null
            }
        }
    }

    companion object {
        private val TAG = EncryptedBrokerInstallResumeStore::class.java.simpleName

        /** Encrypted store file name for broker-install resume snapshots. */
        private const val STORE_NAME = "com.microsoft.identity.broker.install.resume.store"

        /**
         * Creates an encrypted, persistent store rooted at [context]'s application context. Both the
         * persist and resume paths must call this with the **same** app context so they share the
         * underlying encrypted file.
         */
        @JvmStatic
        fun create(context: Context): EncryptedBrokerInstallResumeStore {
            val components = AndroidPlatformComponentsFactory.createFromContext(
                context.applicationContext
            )
            val storage = components.storageSupplier
                .getEncryptedNameValueStore(STORE_NAME, String::class.java)
            return EncryptedBrokerInstallResumeStore(storage)
        }
    }
}
