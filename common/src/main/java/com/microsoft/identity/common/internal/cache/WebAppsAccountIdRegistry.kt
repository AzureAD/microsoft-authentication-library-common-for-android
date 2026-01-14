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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A registry that maps home account IDs to sets of web app client IDs.
 *
 * This is used to track which web applications are associated with which accounts,
 * enabling coordinated sign-out and management of web app sessions.
 */
class WebAppsAccountIdRegistry private constructor(
    private val storage: IMultiTypeNameValueStorage
){
    companion object {
        private val TAG = WebAppsAccountIdRegistry::class.java.simpleName
        private const val WEBAPPS_ACCOUNT_ID_REGISTRY_STORAGE_KEY = "WebAppsAccountIdRegistryStorageKey"
        private val rwLock = ReentrantReadWriteLock()

        /**
         * Factory method to create an instance of [WebAppsAccountIdRegistry].
         *
         * @param supplier The storage supplier.
         * @return A new instance of [WebAppsAccountIdRegistry].
         */
        @JvmStatic
        fun create(supplier: IStorageSupplier): WebAppsAccountIdRegistry {
            val store = supplier.getEncryptedFileStore(WEBAPPS_ACCOUNT_ID_REGISTRY_STORAGE_KEY)
            return WebAppsAccountIdRegistry(store)
        }
    }

    /**
     * Deserialize a JSON string into a set of client IDs.
     *
     * @param raw The JSON string to deserialize.
     * @return A mutable set of client IDs.
     */
    private fun deserializeSet(raw: String?): MutableSet<String> {
        if (raw.isNullOrBlank()) return mutableSetOf()
        return try {
            ObjectMapper.deserializeJsonStringToObject(raw, Array<String>::class.java).toMutableSet()
        } catch (e: Exception) {
            Logger.warn(TAG, "Failed to deserialize set: ${e.message}")
            mutableSetOf()
        }
    }

    /**
     * Serialize the set of client IDs to a JSON string.
     *
     * @param set The set of client IDs to serialize.
     * @return The JSON string representation of the set.
     */
    private fun serializeSet(set: Set<String>): String {
        return ObjectMapper.serializeObjectToJsonString(set)
    }

    /**
     * Load the account entry from storage.
     *
     * @param homeAccountId The home account ID to load.
     * @return A set of client IDs associated with the given account ID. Or an empty set if none exist.
     */
    private fun loadClientIdsForAccount(homeAccountId: String): MutableSet<String>{
        return deserializeSet(storage.getString(homeAccountId))
    }

    /**
     * Save the account entry to storage.
     *
     * @param homeAccountId The home account ID.
     * @param set The set of client IDs to associate with the account ID.
     */
    private fun saveAccount(homeAccountId: String, set: Set<String>) {
        storage.putString(homeAccountId, serializeSet(set))
    }


    /**
     * Remove the account entry from storage.
     *
     * @param homeAccountId The account ID to remove.
     */
    private fun removeAccountStorage(homeAccountId: String) {
        try {
            storage.remove(homeAccountId)
        } catch (e: Exception) {
            storage.putString(homeAccountId, null)
        }
    }

    /**
     * Add a client ID to the set associated with the given account ID.
     *
     * @param homeAccountId The account ID.
     * @param clientId The client ID to add.
     */
    fun addClient(homeAccountId: String, clientId: String) {
        rwLock.write {
            val set = loadClientIdsForAccount(homeAccountId)
            if (set.add(clientId)) {
                saveAccount(homeAccountId, set)
            }
        }
    }
    /**
     * Remove a client ID from the set associated with the given account ID.
     * If the set becomes empty after removal, the account ID is also removed from the registry.
     *
     * @param homeAccountId The account ID.
     * @param clientId The client ID to remove.
     */
    fun removeClient(homeAccountId: String, clientId: String) {
        rwLock.write {
            val set = loadClientIdsForAccount(homeAccountId)
            if (!set.remove(clientId)) return
            if (set.isEmpty()) {
                removeAccountStorage(homeAccountId)
            } else {
                saveAccount(homeAccountId, set)
            }
        }
    }

    /** Get the set of client IDs associated with the given account ID.
     *
     * @param homeAccountId The account ID.
     * @return A set of client IDs associated with the account ID, or an empty set if none exist.
     */
    fun getClients(homeAccountId: String): Set<String> {
        return rwLock.read {
            loadClientIdsForAccount(homeAccountId).toSet()
        }
    }

    /** Check if the registry contains the given client ID for the specified account ID.
     *
     * @param homeAccountId The account ID.
     * @param clientId The client ID to check for.
     * @return True if the client ID is associated with the account ID, false otherwise.
     */
    fun contains(homeAccountId: String, clientId: String): Boolean {
        return rwLock.read {
            loadClientIdsForAccount(homeAccountId).contains(clientId)
        }
    }

    /**
     * Remove the given account ID and all its associated client IDs from the registry.
     *
     * @param homeAccountId The account ID to remove.
     */
    fun removeAccount(homeAccountId: String) {
        rwLock.write {
            val set = loadClientIdsForAccount(homeAccountId)
            if (set.isEmpty()) return
            removeAccountStorage(homeAccountId)
        }
    }
}
