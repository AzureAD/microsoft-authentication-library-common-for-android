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
package com.microsoft.identity.common.internal.broker.ipc

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Build
import android.os.DeadObjectException
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger

/**
 * packageManager.queryContentProviders involves an ipc call (we've seen DeadObjectException from it before)
 *
 * We'll avoid making unnecessary ipc request by caching them.
 * The content provider support status should remain the same for a given broker version.
 **/
class ContentProviderStatusLoader(
    private val getVersionCode: (appPackageName: String) -> String,
    private val supportedByContentProvider: (appPackageName: String) -> Boolean,
    private val fileManager: INameValueStorage<String>,
) : IContentProviderStatusLoader {

    companion object {
        private val TAG = ContentProviderStatusLoader::class.java.simpleName
        private const val SHARED_PREFERENCE_NAME = "com.microsoft.common.ipc.content.provider.query.cache"

        @Throws(PackageManager.NameNotFoundException::class)
        private fun getVersionCode(context: Context, appPackageName: String) : String {
            val packageInfo = context.packageManager.getPackageInfo(appPackageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                packageInfo.versionCode.toString()
            }
        }

        @Throws(DeadObjectException::class)
        fun supportedByContentProvider(context: Context, appPackageName: String) : Boolean {
            val contentProviderAuthority: String =
                ContentProviderStrategy.getContentProviderAuthority(appPackageName)

            val providers: List<ProviderInfo> = context.packageManager
                .queryContentProviders(null, 0, 0)

            for (providerInfo in providers) {
                if (providerInfo.authority != null && providerInfo.authority == contentProviderAuthority) {
                    return true
                }
            }

            return false
        }
    }

    constructor(context: Context, components: IPlatformComponents): this(
        fileManager = components.storageSupplier.getUnencryptedNameValueStore(
            SHARED_PREFERENCE_NAME, String::class.java),
        getVersionCode = { pkgName -> getVersionCode(context, pkgName) },
        supportedByContentProvider = { pkgName -> supportedByContentProvider(context, pkgName) }
    )

    override fun getStatus(packageName: String) : Boolean {
        val methodTag = "$TAG:getResult"
        try {
            // Construct a key for the cache.
            val key = packageName + ":" + getVersionCode(packageName)

            // Try loading from cache, return if the value is found.
            fileManager.get(key)?.let {
                return it.toBoolean()
            }

            val queriedResult = supportedByContentProvider(packageName)
            fileManager.put(key, queriedResult.toString())
            return queriedResult
        } catch (t: Throwable) {
            Logger.error(methodTag, t.message, t)
            return false
        }
    }
}
