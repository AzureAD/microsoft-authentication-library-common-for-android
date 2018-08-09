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
package com.microsoft.identity.common.internal.ui.systembrowser;

import android.app.Activity;
import android.content.ComponentName;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;

import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hides the details of establishing connections and sessions with custom tabs.
 */
public class CustomTabManager {
    private static final String TAG = CustomTabManager.class.getSimpleName();
    private CustomTabsIntent mCustomTabsIntent;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private final WeakReference<Activity> mActivityRef;
    private final AtomicReference<CustomTabsClient> mCustomTabsClient;
    private final CountDownLatch mClientLatch;

    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private static final long CUSTOM_TABS_MAX_CONNECTION_TIMEOUT = 1L;

    /**
     * Constructor of CustomTabManager.
     * @param activity Instance of calling activity.
     */
    public CustomTabManager(@NonNull final Activity activity) {
        mActivityRef = new WeakReference<>(activity);
        mCustomTabsClient = new AtomicReference<>();
        mClientLatch = new CountDownLatch(1);
    }

    /**
     * Method to bind Browser {@link android.support.customtabs.CustomTabsService}.
     * Waits until the {@link CustomTabsServiceConnection} is connected.
     */
    public synchronized void bind(@NonNull String browserPackage) {
        if (mCustomTabsServiceConnection != null) {
            return;
        }

        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                Logger.info(TAG, "CustomTabsService is connected");
                client.warmup(0L);
                mCustomTabsClient.set(client);
                mClientLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.info(TAG, "CustomTabsService is disconnected");
                mCustomTabsClient.set(null);
                //mClientLatch.countDown();
            }
        };

        // Initiate the service-bind action
        if (mActivityRef.get().getApplicationContext() == null
                || !CustomTabsClient.bindCustomTabsService(mActivityRef.get(), browserPackage, mCustomTabsServiceConnection)) {
            Logger.info(TAG, "Unable to bind custom tabs service");
            mClientLatch.countDown();
        }

        // Create the Intent used to launch the Url
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getClient().newSession(null));
        mCustomTabsIntent = builder.setShowTitle(true).build();
        mCustomTabsIntent.intent.setPackage(browserPackage);
    }

    /**
     * Retrieve the custom tab client used to communicate with the custom tab supporting browser,
     * if available when the {@link CustomTabsServiceConnection} is connected or the
     * {@link CustomTabManager#CUSTOM_TABS_MAX_CONNECTION_TIMEOUT} is timed out.
     */
    public CustomTabsClient getClient() {
        try {
            mClientLatch.await(CUSTOM_TABS_MAX_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.info(TAG, "Interrupted while waiting for browser connection");
            mClientLatch.countDown();
        }

        return mCustomTabsClient.get();
    }

    /**
     * Method to unbind custom tabs service {@link android.support.customtabs.CustomTabsService}.
     */
    public synchronized void unbind() {
        if(mCustomTabsServiceConnection == null) {
            return;
        }

        if (mActivityRef.get() != null) {
            mActivityRef.get().getApplicationContext().unbindService(mCustomTabsServiceConnection);
        }

        mCustomTabsClient.set(null);

        Logger.info(TAG, "CustomTabsService is unbound.");
    }
}
