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
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import com.microsoft.identity.common.logging.Logger;

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
            final String methodTag = TAG + ":onCustomTabsServiceConnection";
            Logger.info(methodTag,"CustomTabsService is connected");

            client.warmup(0L);
            mCustomTabsClient.set(client);

            mCustomTabsServiceIsBound = true;

            mClientLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            final String methodTag = TAG + ":onServiceDisconnected";
            Logger.info(methodTag,"CustomTabsService is disconnected");
            mCustomTabsServiceIsBound = false;
            mCustomTabsClient.set(null);
            mClientLatch.countDown();
        }

        @Override
        public void onBindingDied(final ComponentName name) {
            final String methodTag = TAG + ":onBindingDied";
            Logger.warn(methodTag,"Binding died callback on custom tabs service, there will likely be failures. " +
                    " Component class that failed: " + ((name == null) ? "null" : name.getClassName()));
            super.onBindingDied(name);
        }

        @Override
        public void onNullBinding(final ComponentName name) {
            final String methodTag = TAG + ":onNullBinding";
            Logger.warn(methodTag,"Null binding callback on custom tabs service, there will likely be failures."
                    + " Component class that failed: " + ((name == null) ? "null" : name.getClassName()));
            super.onNullBinding(name);
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
    public synchronized boolean bind(final @Nullable Context context, @NonNull String browserPackage) {
        final String methodTag = TAG + ":bind";
        // Initiate the service-bind action
        if (context == null
                || !CustomTabsClient.bindCustomTabsService(context, browserPackage, mCustomTabsServiceConnection)) {
            Logger.info(methodTag, "Unable to bind custom tabs service " +
                    ((context == null)
                            ? "because the context was null"
                            : "because the bind call failed") );
            mClientLatch.countDown();
            return false;
        }

        // Create the Intent used to launch the Url
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(createSession(null));
        mCustomTabsIntent = builder.setShowTitle(true).build();
        mCustomTabsIntent.intent.setPackage(browserPackage);
        return true;
    }

    /**
     * Creates a {@link androidx.browser.customtabs.CustomTabsSession custom tab session} for
     * use with a custom tab intent with optional callback. If no custom tab supporting browser
     * is available, this will return {@code null}.
     * @param callback
     * @return CustomTabsSession custom tab session
     */
    private CustomTabsSession createSession(@Nullable final CustomTabsCallback callback) {
        final String methodTag = TAG + ":createSession";
        final CustomTabsClient client = getClient();
        if (client == null) {
            Logger.warn(methodTag, "Failed to create custom tabs session with null CustomTabClient.");
            return null;
        }

        final CustomTabsSession session = client.newSession(callback);
        if (session == null) {
            Logger.warn(methodTag,"Failed to create custom tabs session through custom tabs client.");
        }

        return session;
    }

    /**
     * Retrieve the custom tab client used to communicate with the custom tab supporting browser,
     * if available when the {@link CustomTabsServiceConnection} is connected or the
     * {@link CustomTabsManager#CUSTOM_TABS_MAX_CONNECTION_TIMEOUT} is timed out.
     */
    public CustomTabsClient getClient() {
        final String methodTag = TAG + ":getClient";
        try {
            mClientLatch.await(CUSTOM_TABS_MAX_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.info(methodTag,"Interrupted while waiting for browser connection");
            mClientLatch.countDown();
        }

        return mCustomTabsClient.get();
    }

    /**
     * Method to unbind custom tabs service {@link androidx.browser.customtabs.CustomTabsService}.
     */
    public synchronized void unbind() {
        final String methodTag = TAG + ":unbind";
        final Context context = mContextRef.get();
        if (context != null && mCustomTabsServiceIsBound) {
            try {
                context.unbindService(mCustomTabsServiceConnection);
            } catch(final Exception e) {
                Logger.warn(methodTag,"Error unbinding custom tabs service, likely failed to bind or previously died: " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        mCustomTabsServiceIsBound = false;
        mCustomTabsClient.set(null);

        Logger.info(methodTag,"CustomTabsService is unbound.");
    }
}