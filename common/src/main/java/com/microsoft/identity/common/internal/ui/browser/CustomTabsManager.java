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
package com.microsoft.identity.common.internal.ui.browser;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hides the details of establishing connections and sessions with custom tabs.
 */
public class CustomTabsManager {
    private static final String TAG = CustomTabsManager.class.getSimpleName();

    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private static final long CUSTOM_TABS_MAX_CONNECTION_TIMEOUT = 1L;

    private final CountDownLatch mClientLatch;

    private final WeakReference<Context> mContextRef;

    private final AtomicReference<CustomTabsClient> mCustomTabsClient;

    private boolean mCustomTabsServiceIsBound;

    private CustomTabsIntent mCustomTabsIntent;

    private CustomTabsServiceConnection mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(final ComponentName name, final CustomTabsClient client) {
            Logger.info(TAG, "CustomTabsService is connected");
            client.warmup(0L);
            mCustomTabsServiceIsBound = true;
            mCustomTabsClient.set(client);
            mClientLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Logger.info(TAG, "CustomTabsService is disconnected");
            mCustomTabsServiceIsBound = false;
            mCustomTabsClient.set(null);
            mClientLatch.countDown();
        }
    };

    public CustomTabsIntent getCustomTabsIntent() {
        return mCustomTabsIntent;
    }

    /**
     * Constructor of CustomTabManager.
     *
     * @param context Instance of calling activity.
     */
    public CustomTabsManager(@NonNull final Context context) {
        mContextRef = new WeakReference<>(context);
        mCustomTabsClient = new AtomicReference<>();
        mClientLatch = new CountDownLatch(1);
    }

    /**
     * Method to bind Browser {@link androidx.browser.customtabs.CustomTabsService}.
     * Waits until the {@link CustomTabsServiceConnection} is connected.
     */
    public synchronized void bind(@NonNull String browserPackage) {
        // Initiate the service-bind action
        if (mContextRef.get() == null
                || !CustomTabsClient.bindCustomTabsService(mContextRef.get(), browserPackage, mCustomTabsServiceConnection)) {
            Logger.info(TAG, "Unable to bind custom tabs service");
            mClientLatch.countDown();
        }

        // Create the Intent used to launch the Url
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(createSession(null));
        mCustomTabsIntent = builder.setShowTitle(true).build();
        mCustomTabsIntent.intent.setPackage(browserPackage);
    }

    /**
     * Creates a {@link androidx.browser.customtabs.CustomTabsSession custom tab session} for
     * use with a custom tab intent with optional callback. If no custom tab supporting browser
     * is available, this will return {@code null}.
     * @param callback
     * @return CustomTabsSession custom tab session
     */
    private CustomTabsSession createSession(@Nullable final CustomTabsCallback callback) {
        final CustomTabsClient client = getClient();
        if (client == null) {
            Logger.warn(TAG, "Failed to create custom tabs session with null CustomTabClient.");
            return null;
        }

        final CustomTabsSession session = client.newSession(callback);
        if (session == null) {
            Logger.warn(TAG, "Failed to create custom tabs session through custom tabs client.");
        }

        return session;
    }

    /**
     * Retrieve the custom tab client used to communicate with the custom tab supporting browser,
     * if available when the {@link CustomTabsServiceConnection} is connected or the
     * {@link CustomTabsManager#CUSTOM_TABS_MAX_CONNECTION_TIMEOUT} is timed out.
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
     * Method to unbind custom tabs service {@link androidx.browser.customtabs.CustomTabsService}.
     */
    public synchronized void unbind() {
        if (mContextRef.get() != null && mCustomTabsServiceIsBound) {
            mContextRef.get().unbindService(mCustomTabsServiceConnection);
        }

        mCustomTabsServiceIsBound = false;
        mCustomTabsClient.set(null);

        Logger.info(TAG, "CustomTabsService is unbound.");
    }
}