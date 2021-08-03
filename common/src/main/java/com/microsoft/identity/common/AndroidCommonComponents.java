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

import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.crypto.AndroidBrokerStorageEncryptionManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.platform.AndroidDeviceMetadata;
import com.microsoft.identity.common.internal.platform.DevicePopManager;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.internal.util.SharedPreferenceLongStorage;
import com.microsoft.identity.common.internal.util.SharedPrefStringNameValueStorage;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.ICommonComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.java.util.ClockSkewManager;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import lombok.NonNull;

import static com.microsoft.identity.common.java.exception.ClientException.CERTIFICATE_LOAD_FAILURE;
import static com.microsoft.identity.common.java.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;

/**
 * Android implementations of platform-dependent components in Common.
 */
public class AndroidCommonComponents implements ICommonComponents<Context> {
    private static String TAG = AndroidCommonComponents.class.getSimpleName();

    /**
     * SharedPref filename for Clock Skew storage.
     */
    private static final String SKEW_PREFERENCES_FILENAME =
            "com.microsoft.identity.client.clock_correction";

    protected final Context mContext;
    private IClockSkewManager mClockSkewManager;
    private IDevicePopManager mDefaultDevicePoPManager;

    public AndroidCommonComponents(@NonNull final Context context) {
        mContext = context;
        Device.setDeviceMetadata(new AndroidDeviceMetadata());
    }

    // TODO: The caller of this base 'common' class is unclear whether it's in Broker or ADAL/MSAL.
    //       Once we wired this e2e, we should be able to supply the right object,
    //       and shouldn't need process to decide which one to return.
    @Override
    public IKeyAccessor getStorageEncryptionManager() {
        final String methodName = ":getStorageEncryptionManager";

        if (ProcessUtil.isBrokerProcess(mContext)) {
            Logger.info(TAG + methodName, "Returning AndroidBrokerStorageEncryptionManager");
            return new AndroidBrokerStorageEncryptionManager(mContext, null);
        }

        Logger.info(TAG + methodName, "Returning AndroidAuthSdkStorageEncryptionManager");
        return new AndroidAuthSdkStorageEncryptionManager(mContext, null);
    }

    @Override
    public synchronized IClockSkewManager getClockSkewManager() {
        if (null == mClockSkewManager) {
            mClockSkewManager = new ClockSkewManager(new SharedPreferenceLongStorage(
                    SharedPreferencesFileManager.getSharedPreferences(
                            mContext,
                            SKEW_PREFERENCES_FILENAME,
                            null
                    )
            ));
        }

        return mClockSkewManager;
    }

    @Override
    public synchronized IDevicePopManager getDefaultDevicePopManager() throws ClientException {
        if (mDefaultDevicePoPManager == null) {
            mDefaultDevicePoPManager = getDevicePopManager(null);
        }
        return mDefaultDevicePoPManager;
    }

    @Override
    public @NonNull IDevicePopManager getDevicePopManager(@Nullable String alias) throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            if (alias == null){
                return new DevicePopManager(mContext);
            } else {
                return new DevicePopManager(mContext, alias);
            }
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        } catch (final CertificateException e) {
            exception = e;
            errCode = CERTIFICATE_LOAD_FAILURE;
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final IOException e) {
            exception = e;
            errCode = IO_ERROR;
        }

        throw new ClientException(
                errCode,
                "Failed to initialize DevicePoPManager = " + exception.getMessage(),
                exception
        );
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
            final INameValueStorage<T> store = (INameValueStorage<T>) new SharedPreferenceLongStorage(mgr);
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
