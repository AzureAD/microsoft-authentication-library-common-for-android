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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * Persisted cache for the IPC hello() protocol.
 * Use client's protocol version and the targeted app's package name and app version as a key
 * to cache the negotiated protocol version.
 *
 *
 * This means a new hello() call will ONLY be triggered only when.
 * 1. IPC operation is invoked for the very first time.
 * 2. Client bumps up protocol version.
 * 3. The targeted app is updated, uninstalled, reinstalled.
 * 4. Cache entry is expired.
 */
/**
 * @param context              application context.
 * @param protocolName         name of the protocol that invokes hello().
 * @param targetAppPackageName package name of the app that this client will hello() with.
 * @param components           Platform components.
 * @param cacheExpiryTimeInMs  Cache entry expiry time.
 */
open class HelloCache (
    private val context: Context,
    private val protocolName: String,
    private val targetAppPackageName: String,
    components: IPlatformComponents,
    private val cacheExpiryTimeInMs: Long = DEFAULT_CACHE_EXPIRY_MILLIS
) {
    companion object {
        private val TAG = HelloCache::class.java.simpleName
        private const val SHARED_PREFERENCE_NAME = "com.microsoft.common.ipc.hello.cache"
        private var sIsEnabled = true

        /**
         * Default life time of cache entry.
         */
        private val DEFAULT_CACHE_EXPIRY_MILLIS = TimeUnit.HOURS.toMillis(4)

        /**
         * If set to false, Hello cache will be disabled.
         * When you're developing protocol change, you might not want the cache to be enabled.
         *
         * For debugging only.
         */
        fun setIsEnabled(value: Boolean) {
            synchronized(HelloCache::class.java) {
                sIsEnabled = value
            }
        }
    }

    private val fileManager: INameValueStorage<String>

    init {
        fileManager = components.storageSupplier.getUnencryptedNameValueStore(
            SHARED_PREFERENCE_NAME, String::class.java
        )
    }

    /**
     * Gets the cached negotiated protocol version.
     * Returns null
     * - if there is none.
     * - if there's error retrieving the value.
     * - if entry cache entry is expired (entry itself is cleared).
     * else return [HelloCacheResult] with either successful negotiated protocol version or error.
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     */
    fun getHelloCacheResult(
        clientMinimumProtocolVersion: String?,
        clientMaximumProtocolVersion: String
    ): HelloCacheResult? {
        val methodTag = "$TAG:tryGetNegotiatedProtocolVersion"
        if (!sIsEnabled) {
            Logger.infoPII(methodTag, "hello cache is not enabled.")
            return null
        }
        val key: String = try {
            getNegotiatedProtocolVersionCacheKey(
                clientMinimumProtocolVersion,
                clientMaximumProtocolVersion
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.error(methodTag, "Failed to retrieve key", e)
            return null
        }
        val cachedRawValue = fileManager[key]
        if (cachedRawValue.isNullOrEmpty()) {
            return null
        }
        val cacheResult = HelloCacheResult.deserialize(cachedRawValue)
        if (cacheResult == null) {
            Logger.info(methodTag, "Legacy or invalid cache value.")
            fileManager.remove(key)
            return null
        }

        // check if expired. Delete entry and return null.
        if ((System.currentTimeMillis() - cacheResult.timeStamp) > cacheExpiryTimeInMs) {
            Logger.info(methodTag, "Cache entry is expired.")
            fileManager.remove(key)
            return null
        }
        return cacheResult
    }

    /**
     * Store the given negotiated protocol version into the cache.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     * @param negotiatedProtocolVersion    the negotiated protocol version as returned from hello().
     */
    fun saveNegotiatedProtocolVersion(
        clientMinimumProtocolVersion: String?,
        clientMaximumProtocolVersion: String,
        negotiatedProtocolVersion: String
    ) {
        val methodTag = "$TAG:saveNegotiatedProtocolVersion"
        saveNegotiatedValue(
            clientMinimumProtocolVersion,
            clientMaximumProtocolVersion,
            HelloCacheResult.createFromNegotiatedProtocolVersion(negotiatedProtocolVersion),
            methodTag
        )
    }

    /**
     * Store the given negotiated protocol version into the cache.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     */
    fun saveHandShakeError(
        clientMinimumProtocolVersion: String?,
        clientMaximumProtocolVersion: String
    ) {
        val methodTag = "$TAG:saveHandShakeError"
        saveNegotiatedValue(
            clientMinimumProtocolVersion,
            clientMaximumProtocolVersion,
            HelloCacheResult.createHandshakeError(),
            methodTag
        )
    }

    /**
     * Store the given negotiated protocol version into the cache.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     * @param negotiationValue    the negotiated protocol version as returned from hello().
     */
    private fun saveNegotiatedValue(
        clientMinimumProtocolVersion: String?,
        clientMaximumProtocolVersion: String,
        result: HelloCacheResult,
        callerMethodTag: String
    ) {
        val methodTag = "$TAG$callerMethodTag:saveNegotiatedProtocolVersion"
        if (!sIsEnabled) {
            Logger.infoPII(methodTag, "hello cache is not enabled.")
            return
        }
        val key: String = try {
            getNegotiatedProtocolVersionCacheKey(
                clientMinimumProtocolVersion,
                clientMaximumProtocolVersion
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.error(methodTag, "Failed to retrieve key", e)
            return
        }
        fileManager.put(key, result.serialize())
    }

    /**
     * Generates [SharedPreferencesFileManager]'s s cache key for the negotiated protocol version.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     */
    @Throws(PackageManager.NameNotFoundException::class)
    private fun getNegotiatedProtocolVersionCacheKey(
        clientMinimumProtocolVersion: String?,
        clientMaximumProtocolVersion: String
    ): String {
        return (protocolName +
                "[" + clientMinimumProtocolVersion + "," + clientMaximumProtocolVersion + "]:"
                + targetAppPackageName + "[" + versionCode + "]")
    }

    @VisibleForTesting
    fun clearCache() {
        fileManager.clear()
    }

    @get:Throws(PackageManager.NameNotFoundException::class)
    @get:VisibleForTesting
    open val versionCode: String
        get() {
            val packageInfo = context.packageManager.getPackageInfo(targetAppPackageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                packageInfo.versionCode.toString()
            }
        }
}