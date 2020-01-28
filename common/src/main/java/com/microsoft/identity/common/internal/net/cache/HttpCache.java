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
package com.microsoft.identity.common.internal.net.cache;

import android.net.http.HttpResponseCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for caching HTTP and HTTPS responses.
 *
 * @see HttpResponseCache
 */
public class HttpCache {

    private static final String TAG = HttpCache.class.getSimpleName();

    /**
     * The default name of the HTTP/S cache on disk.
     */
    public static final String DEFAULT_HTTP_CACHE_NAME = "com.microsoft.identity.http-cache";

    /**
     * The default size of the HTTP/S cache on disk.
     */
    public static final long DEFAULT_HTTP_CACHE_CAPACITY_BYTES = 10 * 1024 * 1024; // 10 MiB

    /**
     * Initializes a new {@link HttpResponseCache} based on the supplied parameters.
     *
     * @param cacheDirectory The parent-directory in which the cache should reside.
     * @param cacheFileName  The directory name for the HTTP cache.
     * @param maxSizeBytes   The maximum allowed size of the cache. Once capacity is hit,
     *                       entries are removed according to an LRU strategy.
     * @return True if the cache was successfully installed. False otherwise.
     */
    public static boolean initialize(@NonNull final File cacheDirectory,
                                     @NonNull final String cacheFileName,
                                     final long maxSizeBytes) {
        final String methodName = ":initialize (File, Filename, Capacity)";
        boolean success = false;

        try {
            final File httpCacheDir = new File(cacheDirectory, cacheFileName);
            HttpResponseCache.install(httpCacheDir, maxSizeBytes);
            success = true;
        } catch (final IOException e) {
            Logger.error(
                    TAG + methodName,
                    "HTTP Response cache installation failed.",
                    e
            );
        }

        return success;
    }

    /**
     * Initializes a new {@link HttpResponseCache} inside the provided directory.
     *
     * @param cacheDirectory The parent-directory in which the cache should reside.
     * @return True if the cache was successfully installed. False otherwise.
     */
    public static boolean initialize(@NonNull final File cacheDirectory) {
        return initialize(
                cacheDirectory,
                DEFAULT_HTTP_CACHE_NAME,
                DEFAULT_HTTP_CACHE_CAPACITY_BYTES
        );
    }

    /**
     * Returns the currently intalled {@link HttpResponseCache} or null, if none is installed.
     *
     * @return The HttpResponseCache.
     */
    @Nullable
    public static HttpResponseCache getInstalled() {
        return HttpResponseCache.getInstalled();
    }

    /**
     * Convenience function for {@link HttpResponseCache#flush()}.
     */
    public static void flush() {
        final String methodName = ":flush";

        final HttpResponseCache responseCache = getInstalled();

        if (null != responseCache) {
            responseCache.flush();
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Unable to flush cache because none is installed."
            );
        }
    }
}
