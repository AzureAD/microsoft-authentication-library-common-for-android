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

package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.logging.Logger;

/**
 * Persisted cache for the IPC hello() protocol.
 * Use client's protocol version and the targeted app's package name and app version as a key
 * to cache the negotiated protocol version.
 * <p>
 * This means a new hello() call will ONLY be triggered only when.
 * 1. IPC operation is invoked for the very first time.
 * 2. Client bumps up protocol version.
 * 3. The targeted app is updated, uninstalled, reinstalled.
 */
public class HelloCache {
    private static final String TAG = HelloCache.class.getSimpleName();

    private static final String SHARED_PREFERENCE_NAME = "com.microsoft.common.ipc.hello.cache";

    private final INameValueStorage<String> mFileManager;
    private final Context mContext;
    private final String mProtocolName;
    private final String mTargetAppPackageName;
    private static boolean sIsEnabled = true;

    /**
     * If set to false, Hello cache will be disabled.
     * When you're developing protocol change, you might not want the cache to be enabled.
     * <p>
     * For debugging only.
     */
    public static void setIsEnabled(final boolean value) {
        synchronized (HelloCache.class) {
            sIsEnabled = value;
        }
    }

    /**
     * Default constructor.
     *  @param context              application context.
     * @param protocolName         name of the protocol that invokes hello().
     * @param targetAppPackageName package name of the app that this client will hello() with.
     * @param components
     */
    public HelloCache(final @NonNull Context context,
                      final @NonNull String protocolName,
                      final @NonNull String targetAppPackageName,
                      final @NonNull IPlatformComponents components) {
        mFileManager = components.getStorageSupplier().getNameValueStore(SHARED_PREFERENCE_NAME, String.class);
        mContext = context;
        mProtocolName = protocolName;
        mTargetAppPackageName = targetAppPackageName;
    }

    /**
     * Gets the cached negotiated protocol version. Returns null if there is none.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     */
    public @Nullable String tryGetNegotiatedProtocolVersion(final @Nullable String clientMinimumProtocolVersion,
                                                            final @NonNull String clientMaximumProtocolVersion) {
        final String methodTag = TAG + ":tryGetNegotiatedProtocolVersion";

        if (!sIsEnabled) {
            Logger.infoPII(methodTag, "hello cache is not enabled.");
            return null;
        }

        final String key;
        try {
            key = getNegotiatedProtocolVersionCacheKey(clientMinimumProtocolVersion, clientMaximumProtocolVersion);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.error(methodTag, "Failed to retrieve key", e);
            return null;
        }

        return mFileManager.get(key);
    }

    /**
     * Store the given negotiated protocol version into the cache.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     * @param negotiatedProtocolVersion    the negotiated protocol version as returned from hello().
     */
    public void saveNegotiatedProtocolVersion(final @Nullable String clientMinimumProtocolVersion,
                                              final @NonNull String clientMaximumProtocolVersion,
                                              final @NonNull String negotiatedProtocolVersion) {
        final String methodTag = TAG + ":saveNegotiatedProtocolVersion";

        if (!sIsEnabled) {
            Logger.infoPII(methodTag, "hello cache is not enabled.");
            return;
        }

        final String key;
        try {
            key = getNegotiatedProtocolVersionCacheKey(clientMinimumProtocolVersion, clientMaximumProtocolVersion);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.error(methodTag, "Failed to retrieve key", e);
            return;
        }

        mFileManager.put(key, negotiatedProtocolVersion);
    }

    /**
     * Generates {@link SharedPreferencesFileManager}'s s cache key for the negotiated protocol version.
     *
     * @param clientMinimumProtocolVersion minimum version of the protocol that the client supports.
     * @param clientMaximumProtocolVersion maximum version of the protocol that to be advertised by the client.
     */
    private @NonNull String getNegotiatedProtocolVersionCacheKey(final @Nullable String clientMinimumProtocolVersion,
                                                                 final @NonNull String clientMaximumProtocolVersion)
            throws PackageManager.NameNotFoundException {
        return mProtocolName +
                "[" + clientMinimumProtocolVersion + "," + clientMaximumProtocolVersion + "]:"
                + mTargetAppPackageName + "[" + getVersionCode() + "]";
    }

    @VisibleForTesting
    public void clearCache() {
        mFileManager.clear();
    }

    @VisibleForTesting
    public @NonNull String getVersionCode() throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mTargetAppPackageName, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return String.valueOf(packageInfo.getLongVersionCode());
        } else {
            return String.valueOf(packageInfo.versionCode);
        }
    }
}
