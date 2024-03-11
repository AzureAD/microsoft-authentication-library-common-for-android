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

import android.content.pm.PackageManager
import com.microsoft.identity.common.java.util.ported.InMemoryStorage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentProviderStatusLoaderTest {

    @Test
    fun testAppNotInstalled(){
        val loader = ContentProviderStatusLoader(
            getVersionCode = { throw PackageManager.NameNotFoundException() },
            supportedByContentProvider = { throw IllegalStateException() },
            fileManager = InMemoryStorage()
        )

        Assert.assertFalse(loader.getStatus("com.microsoft.test"))
    }

    @Test
    fun testGetValidResultAndPersistInCache(){
        val cache = InMemoryStorage<String>()
        val versionKey = "1.2.3456"

        val loader = ContentProviderStatusLoader(
            getVersionCode = { pkgName ->
                if (pkgName == "com.microsoft.test")
                    return@ContentProviderStatusLoader versionKey
                throw PackageManager.NameNotFoundException()
            },
            supportedByContentProvider = { true },
            fileManager = cache
        )

        Assert.assertTrue(loader.getStatus("com.microsoft.test"))
        Assert.assertTrue(cache.get("com.microsoft.test:$versionKey").toBoolean())
        Assert.assertEquals(1, cache.size())
    }

    @Test
    fun testGetInalidResultAndPersistInCache(){
        val cache = InMemoryStorage<String>()
        val versionKey = "123.456.789"

        val loader = ContentProviderStatusLoader(
            getVersionCode = { pkgName ->
                if (pkgName == "com.microsoft.test")
                    return@ContentProviderStatusLoader versionKey
                throw PackageManager.NameNotFoundException()
            },
            supportedByContentProvider = { false },
            fileManager = cache
        )

        Assert.assertFalse(loader.getStatus("com.microsoft.test"))
        Assert.assertFalse(cache.get("com.microsoft.test:$versionKey").toBoolean())
        Assert.assertEquals(1, cache.size())
    }

    @Test
    fun testGetValueFromCache(){
        val cache = InMemoryStorage<String>()
        val versionKey = "123.456.789"
        cache.put("com.microsoft.test:$versionKey", true.toString())

        val loader = ContentProviderStatusLoader(
            getVersionCode = { pkgName ->
                if (pkgName == "com.microsoft.test")
                    return@ContentProviderStatusLoader versionKey
                throw PackageManager.NameNotFoundException()
            },
            supportedByContentProvider = {
                throw IllegalStateException("This line should not be reached") },
            fileManager = cache
        )

        Assert.assertTrue(loader.getStatus("com.microsoft.test"))
        Assert.assertEquals(1, cache.size())
    }

    @Test
    fun testTryGetValueFromCache_VersionMismatch(){
        val cache = InMemoryStorage<String>()
        val versionKey = "123.456.789"
        cache.put("com.microsoft.test:$versionKey", true.toString())
        val newVersionKey = "1.2.234"

        val loader = ContentProviderStatusLoader(
            getVersionCode = { pkgName ->
                if (pkgName == "com.microsoft.test")
                    // Returns a different version
                    return@ContentProviderStatusLoader newVersionKey
                throw PackageManager.NameNotFoundException()
            },
            supportedByContentProvider = { false },
            fileManager = cache
        )

        Assert.assertFalse(loader.getStatus("com.microsoft.test"))
        Assert.assertFalse(cache.get("com.microsoft.test:$newVersionKey").toBoolean())
        Assert.assertEquals(2, cache.size())
    }
}
