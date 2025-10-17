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

import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.logging.Logger

/**
 * A registry that maps account IDs to sets of web app client IDs.
 *
 * This is used to track which web applications are associated with which accounts,
 * enabling coordinated sign-out and management of web app sessions.
 */
class WebAppsAccountIdRegistry private constructor(
    private val storage: IMultiTypeNameValueStorage
){
    private val lock = Any()

    companion object {
        private val TAG = WebAppsAccountIdRegistry::class.java.simpleName
        private const val WEBAPPS_ACCOUNT_ID_REGISTRY_STORAGE_KEY = "WebAppsAccountIdRegistryStorageKey"
        private const val MAP_JSON_KEY = "MapJsonKey"

        /**
         * Factory method to create an instance of [WebAppsAccountIdRegistry].
         *
         * @param supplier The storage supplier.
         * @return A new instance of [WebAppsAccountIdRegistry].
         */
        fun create(supplier: IStorageSupplier): WebAppsAccountIdRegistry {
            val store = supplier.getEncryptedFileStore(WEBAPPS_ACCOUNT_ID_REGISTRY_STORAGE_KEY)
            return WebAppsAccountIdRegistry(store)
        }
    }

    @Volatile
    private var cache: MutableMap<String, MutableSet<String>>? = null

    // So we can more easily serialize/deserialize the entire map.
    private data class Container(
        val accounts: MutableMap<String, MutableSet<String>> = mutableMapOf()
    )

    /**
     * Load the registry from storage, or return the cached version if already loaded.
     *
     * @return The current mapping of account IDs to sets of client IDs.
     */
    private fun load(): MutableMap<String, MutableSet<String>> {
        val methodTag = "$TAG:load"
        cache?.let { return it }
        val raw = storage.getString(MAP_JSON_KEY)
        val map = if (!raw.isNullOrBlank()) {
            try {
                ObjectMapper.deserializeJsonStringToObject(raw, Container::class.java).accounts
            } catch (e: Exception) {
                Logger.warn(methodTag, "Failed to deserialize existing registry: ${e.message}.")
                mutableMapOf()
            }
        } else {
            Logger.info(methodTag, "No existing registry, creating a new one.")
            mutableMapOf()
        }
        cache = map
        return map
    }

    /**
     * Save the current registry state to storage.
     *
     * @param current The mapping of account IDs to sets of client IDs to save.
     */
    private fun save(current: Map<String, MutableSet<String>>) {
        val json = ObjectMapper.serializeObjectToJsonString(Container(current.toMutableMap()))
        storage.putString(MAP_JSON_KEY, json)
    }

    /**
     * Add a client ID to the set associated with the given account ID.
     *
     * @param accountId The account ID.
     * @param clientId The client ID to add.
     */
    fun addClient(accountId: String, clientId: String) {
        synchronized(lock) {
            val map = load()
            val set = map.getOrPut(accountId) { mutableSetOf() }
            if (set.add(clientId)) save(map)
        }
    }

    /**
     * Remove a client ID from the set associated with the given account ID.
     * If the set becomes empty after removal, the account ID is also removed from the registry.
     *
     * @param accountId The account ID.
     * @param clientId The client ID to remove.
     */
    fun removeClient(accountId: String, clientId: String) {
        synchronized(lock) {
            val map = load()
            val set = map[accountId] ?: return
            if (set.remove(clientId)) {
                if (set.isEmpty()) map.remove(accountId)
                save(map)
            }
        }
    }

    /** Get the set of client IDs associated with the given account ID.
     *
     * @param accountId The account ID.
     * @return A set of client IDs associated with the account ID, or an empty set if none exist.
     */
    fun getClients(accountId: String): Set<String> {
        synchronized(lock) {
            val map = load()
            return map[accountId]?.toSet() ?: emptySet()
        }
    }

    /** Check if the registry contains the given client ID for the specified account ID.
     *
     * @param accountId The account ID.
     * @param clientId The client ID to check for.
     * @return True if the client ID is associated with the account ID, false otherwise.
     */
    fun contains(accountId: String, clientId: String): Boolean {
        synchronized(lock) {
            val map = load()
            return map[accountId]?.contains(clientId) == true
        }
    }

    /**
     * Remove the given account ID and all its associated client IDs from the registry.
     */
    fun removeAccount(accountId: String) {
        synchronized(lock) {
            val map = load()
            if (map.remove(accountId) != null) save(map)
        }
    }

    /**
     * Clear the in-memory cache and reload the registry from storage.
     * This is useful for testing or if the underlying storage may have changed externally.
     */
    fun refresh() {
        synchronized(lock) {
            cache = null
            load()
        }
    }

    @VisibleForTesting
    fun getAll(): Map<String, Set<String>> {
        synchronized(lock) {
            return load().mapValues { it.value.toSet() }
        }
    }
}
