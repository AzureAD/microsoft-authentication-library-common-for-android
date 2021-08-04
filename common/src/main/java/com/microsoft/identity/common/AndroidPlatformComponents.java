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

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.crypto.AndroidBrokerStorageEncryptionManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.platform.AndroidDeviceMetadata;
import com.microsoft.identity.common.internal.platform.AndroidPlatformUtil;
import com.microsoft.identity.common.internal.platform.DevicePopManager;
import com.microsoft.identity.common.internal.providers.oauth2.AndroidTaskStateGenerator;
import com.microsoft.identity.common.internal.ui.AndroidAuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.internal.util.SharedPreferenceLongStorage;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.util.ClockSkewManager;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.strategies.IAuthorizationStrategyFactory;

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
public class AndroidPlatformComponents implements IPlatformComponents {
    private static final String TAG = AndroidPlatformComponents.class.getSimpleName();

    /**
     * SharedPref filename for Clock Skew storage.
     */
    private static final String SKEW_PREFERENCES_FILENAME =
            "com.microsoft.identity.client.clock_correction";

    @NonNull
    protected final Context mContext;

    @Nullable
    protected final Activity mActivity;

    @Nullable
    protected final Fragment mFragment;

    private IClockSkewManager mClockSkewManager;
    private IDevicePopManager mDefaultDevicePoPManager;

    /**
     * True if all of the platform-dependent static classes have been initialized.
     */
    private static boolean sInitialized = false;

    /**
     * Initializes platform-dependent static classes.
     * TODO: Once we finish the work, this should be extracted out.
     *       It should be init separately, not as part of this class' construction.
     */
    public static synchronized void initializeStaticClasses() {
        if (!sInitialized) {
            Device.setDeviceMetadata(new AndroidDeviceMetadata());
            Logger.setAndroidLogger();
            sInitialized = true;
        }
    }

    /**
     * Creates an {@link AndroidPlatformComponents} object from a {@link Context}.
     *
     * @param context an application context.
     **/
    public static AndroidPlatformComponents createFromContext(@NonNull final Context context) {
        return new AndroidPlatformComponents(context, null, null);
    }

    /**
     * Creates an {@link AndroidPlatformComponents} object from an {@link Activity} and, optionally, a {@link Fragment}.
     *
     * @param activity an activity where an interactive session will be attached to.
     * @param fragment a fragment where an interactive session will be attached to.
     **/
    public static AndroidPlatformComponents createFromActivity(@NonNull final Activity activity,
                                                               @Nullable final Fragment fragment) {
        return new AndroidPlatformComponents(activity.getApplicationContext(), activity, fragment);
    }

    protected AndroidPlatformComponents(@NonNull final Context applicationContext,
                                        @Nullable final Activity activity,
                                        @Nullable final Fragment fragment) {
        mContext = applicationContext;
        mActivity = activity;
        mFragment = fragment;
        initializeStaticClasses();
    }

    @Override
    public @NonNull INameValueStorage<String> getNameValueStorage(@NonNull String name) {
        return new SharedPreferenceStringStorage(mContext, name);
    }

    // TODO: The caller of this base 'common' class is unclear whether it's in Broker or ADAL/MSAL.
    //       Once we wired this e2e, we should be able to supply the right object,
    //       and shouldn't need process to decide which one to return.
    @Override
    public @NonNull IKeyAccessor getStorageEncryptionManager() {
        final String methodName = ":getStorageEncryptionManager";

        if (ProcessUtil.isBrokerProcess(mContext)) {
            Logger.info(TAG + methodName, "Returning AndroidBrokerStorageEncryptionManager");
            return new AndroidBrokerStorageEncryptionManager(mContext, null);
        }

        Logger.info(TAG + methodName, "Returning AndroidAuthSdkStorageEncryptionManager");
        return new AndroidAuthSdkStorageEncryptionManager(mContext, null);
    }

    @Override
    public synchronized @NonNull IClockSkewManager getClockSkewManager() {
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
    public synchronized @NonNull IDevicePopManager getDefaultDevicePopManager() throws ClientException {
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
            if (alias == null) {
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

    @SuppressWarnings(WarningType.rawtype_warning)
    @Override
    public @NonNull IAuthorizationStrategyFactory getAuthorizationStrategyFactory() {
        return AndroidAuthorizationStrategyFactory.builder()
                .context(mContext)
                .activity(mActivity)
                .fragment(mFragment)
                .build();
    }

    @Override
    public @NonNull IStateGenerator getStateGenerator() {
        if (mActivity == null) {
            throw new IllegalStateException("StateGenerator requires an activity");
        }

        return new AndroidTaskStateGenerator(mActivity.getTaskId());
    }

    @Override
    public @NonNull IPlatformUtil getPlatformUtil() {
        return new AndroidPlatformUtil(mContext, mActivity);
    }
}
