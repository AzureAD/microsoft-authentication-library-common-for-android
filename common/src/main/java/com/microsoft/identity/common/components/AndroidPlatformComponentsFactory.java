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
package com.microsoft.identity.common.components;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.platform.AndroidBroadcaster;
import com.microsoft.identity.common.internal.platform.AndroidDeviceMetadata;
import com.microsoft.identity.common.internal.platform.AndroidPlatformUtil;
import com.microsoft.identity.common.internal.providers.oauth2.AndroidTaskStateGenerator;
import com.microsoft.identity.common.internal.ui.AndroidAuthorizationStrategyFactory;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.PlatformComponents;
import com.microsoft.identity.common.java.net.DefaultHttpClientWrapper;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;

import lombok.NonNull;

/**
 * A factory class for building Android implementations of platform-dependent components in Common.
 */
public class AndroidPlatformComponentsFactory {

    private static final String TAG = AndroidPlatformComponentsFactory.class.getSimpleName();

    /**
     * True if all of the platform-dependent static classes have been initialized.
     */
    private static boolean sGlobalStateInitalized = false;

    /**
     * Initializes platform-dependent static classes.
     */
    public static synchronized void initializeGlobalStates(@NonNull final Context context){
        final String methodTag = TAG + ":initializeGlobalStates";
        if (!sGlobalStateInitalized) {
            HttpCache.initialize(context);
            Device.setDeviceMetadata(new AndroidDeviceMetadata());
            Logger.setAndroidLogger();

            final File cacheDir = context.getCacheDir();
            if (cacheDir != null) {
                HttpCache.initialize(cacheDir);
            } else {
                Logger.warn(methodTag, "Http caching is not enabled because the cache dir is null");
            }

            sGlobalStateInitalized = true;
        }
    }

    /**
     * Creates an {@link IPlatformComponents} object from a {@link Context}.
     *
     * @param context an application context.
     **/
    public static IPlatformComponents createFromContext(@NonNull final Context context) {
        return create(context, null, null);
    }

    /**
     * Creates an {@link IPlatformComponents} object from an {@link Activity} and, optionally, a {@link Fragment}.
     *
     * @param activity an activity where an interactive session will be attached to.
     * @param fragment a fragment where an interactive session will be attached to.
     **/
    public static IPlatformComponents createFromActivity(@NonNull final Activity activity,
                                                         @Nullable final Fragment fragment) {

        return create(activity.getApplicationContext(), activity, fragment);
    }

    @SuppressWarnings(WarningType.rawtype_warning)
    private static IPlatformComponents create(@NonNull final Context context,
                                              @Nullable final Activity activity,
                                              @Nullable final Fragment fragment) {
        initializeGlobalStates(context);

        final PlatformComponents.PlatformComponentsBuilder builder = PlatformComponents.builder();
        fillBuilder(builder, context, activity, fragment);
        return builder.build();
    }

    /**
     * Fill {@link PlatformComponents.PlatformComponentsBuilder} with Android implementations.
     */
    @SuppressWarnings(WarningType.rawtype_warning)
    private static void fillBuilder(@NonNull final PlatformComponents.PlatformComponentsBuilder builder,
                                    @NonNull final Context context,
                                    @Nullable final Activity activity,
                                    @Nullable final Fragment fragment) {
        builder.storageEncryptionManager(new AndroidAuthSdkStorageEncryptionManager(context));
        fillBuilderWithBasicImplementations(builder, context, activity, fragment);
    }

    /**
     * Fill {@link PlatformComponents.PlatformComponentsBuilder}
     * with Android implementations that could be shared with other Factories, i.e. Broker.
     */
    @SuppressWarnings(WarningType.rawtype_warning)
    public static void fillBuilderWithBasicImplementations(
            @NonNull final PlatformComponents.PlatformComponentsBuilder builder,
            @NonNull final Context context,
            @Nullable final Activity activity,
            @Nullable final Fragment fragment) {
        builder.clockSkewManager(new AndroidClockSkewManager(context))
                .broadcaster(new AndroidBroadcaster(context))
                .popManagerLoader(new AndroidPopManagerSupplier(context))
                .storageLoader(new AndroidStorageSupplier(context))
                .platformUtil(new AndroidPlatformUtil(context, activity))
                .httpClientWrapper(new DefaultHttpClientWrapper());

        if (activity != null){
            builder.authorizationStrategyFactory(
                            AndroidAuthorizationStrategyFactory.builder()
                                    .context(activity.getApplicationContext())
                                    .activity(activity)
                                    .fragment(fragment)
                                    .build())
                    .stateGenerator(new AndroidTaskStateGenerator(activity.getTaskId()));
        }
    }
}
