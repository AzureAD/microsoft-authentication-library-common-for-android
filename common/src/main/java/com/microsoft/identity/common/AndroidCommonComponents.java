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
package com.microsoft.identity.common;

import android.content.Context;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.crypto.AndroidBrokerStorageEncryptionManager;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.internal.util.SharedPrefLongNameValueStorage;
import com.microsoft.identity.common.internal.util.SharedPrefStringNameValueStorage;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.interfaces.ICommonComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * Android implementations of platform-dependent components in Common.
 */
public class AndroidCommonComponents implements ICommonComponents<Context> {
    private static String TAG = AndroidCommonComponents.class.getSimpleName();

    protected final Context mContext;

    public AndroidCommonComponents(@NonNull final Context context){
        mContext = context;
    }

    @Override
    public void flushHttpCache() {
        HttpCache.flush();
    }

    // TODO: The caller of this base 'common' class is unclear whether it's in Broker or ADAL/MSAL.
    //       Once we wired this e2e, we should be able to supply the right object,
    //       and shouldn't need process to decide which one to return.
    @Override
    public IKeyAccessor getStorageEncryptionManager(@Nullable final ITelemetryCallback telemetryCallback) {
        final String methodName = ":getStorageEncryptionManager";

        if (ProcessUtil.isBrokerProcess(mContext)) {
            Logger.info(TAG + methodName, "Returning AndroidBrokerStorageEncryptionManager");
            return new AndroidBrokerStorageEncryptionManager(mContext, telemetryCallback);
        }

        Logger.info(TAG + methodName, "Returning AndroidAuthSdkStorageEncryptionManager");
        return new AndroidAuthSdkStorageEncryptionManager(mContext, telemetryCallback);
    }

    @Override
    public <T> INameValueStorage<T> getNameValueStore(String storeName, Class<T> clazz) {
        return getEncryptedNameValueStore(storeName, null, clazz);
    }

    @Override
    public <T> INameValueStorage<T> getEncryptedNameValueStore(String storeName, IKeyAccessor helper, Class<T> clazz) {
        final ISharedPreferencesFileManager mgr = SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, helper);
        if (Long.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            final INameValueStorage<T> store = (INameValueStorage<T>) new SharedPrefLongNameValueStorage(mgr);
            return store;
        } else if (String.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            final INameValueStorage<T> store = (INameValueStorage<T>) new SharedPrefStringNameValueStorage(mgr);
            return store;
        }
        throw new UnsupportedOperationException("Only Long and String are natively supported as types");
    }

    @Override
    public ISharedPreferencesFileManager getEncryptedFileStore(String storeName, IKeyAccessor helper) {
        return SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, helper);
    }

    @Override
    public ISharedPreferencesFileManager getFileStore(String storeName) {
        return SharedPreferencesFileManager.getSharedPreferences(mContext, storeName, null);
    }

    @Deprecated
    @Override
    public Context getPlatformContext() {
        return mContext;
    }
}
