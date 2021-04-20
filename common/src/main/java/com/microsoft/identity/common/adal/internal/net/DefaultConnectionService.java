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
package com.microsoft.identity.common.adal.internal.net;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.adal.internal.PowerManagerWrapper;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.java.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.internal.telemetry.events.BaseEvent;

/**
 * Default connection service check network connectivity.
 * TODO: No need for {@link IConnectionService}. Interface was created for testing purpose.
 * Same purpose could be achieved via mocking the context. Since it's a public interface, should
 * be removed in the next major version update.
 * https://github.com/AzureAD/azure-activedirectory-library-for-android/issues/626
 */
public class DefaultConnectionService implements IConnectionService {

    private final Context mConnectionContext;
    private static boolean connectionAvailable = false;
    // We need a single instance of the NetworkCallback to prevent multiple callbacks from being
    // registered for each instance of DefaultConnectionService.
    private static ConnectivityManager.NetworkCallback networkCallback;

    private static final Object lock = new Object();

    /**
     * Constructor of DefaultConnectionService.
     *
     * @param ctx Context
     */
    public DefaultConnectionService(Context ctx) {
        mConnectionContext = ctx;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        }
    }

    /**
     * Registers a new network callback
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void registerNetworkCallback() {
        synchronized (lock) {
            if (null == networkCallback) {
                try {
                    final ConnectivityManager connectivityManager = (ConnectivityManager) mConnectionContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    final NetworkRequest.Builder builder = new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                    // Initialize the connectionAvailable to the active network info, before the callback is registered.
                    DefaultConnectionService.connectionAvailable =
                            null != activeNetwork &&
                                    activeNetwork.isConnectedOrConnecting();

                    connectivityManager.registerNetworkCallback(
                            builder.build(),
                            networkCallback = new ConnectivityManager.NetworkCallback() {
                                @Override
                                public void onAvailable(@NonNull Network network) {
                                    DefaultConnectionService.connectionAvailable = true;
                                }

                                @Override
                                public void onLost(@NonNull Network network) {
                                    DefaultConnectionService.connectionAvailable = false;
                                }
                            });
                } catch (Exception e) {
                    // The connection callback registration failed
                    DefaultConnectionService.connectionAvailable = false;
                }
            }
        }
    }

    /**
     * De-registers the existing network callback.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void unregisterNetworkCallback() {
        synchronized (lock) {
            if (networkCallback != null) {
                final ConnectivityManager connectivityManager = (ConnectivityManager) mConnectionContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
        }
    }

    /**
     * Check if the network connection is available.
     *
     * @return True if network connection available, false otherwise.
     */
    public boolean isConnectionAvailable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) mConnectionContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            DefaultConnectionService.connectionAvailable =
                    activeNetwork != null &&
                            activeNetwork.isConnectedOrConnecting();
        }
        Telemetry.emit((BaseEvent) new BaseEvent().put(
                TelemetryEventStrings.Key.NETWORK_CONNECTION,
                String.valueOf(DefaultConnectionService.connectionAvailable)
        ));

        return DefaultConnectionService.connectionAvailable;
    }

    /**
     * Determines if the client app cannot access the network due to power saving optimizations introduced in API 23.
     *
     * @return true if the device is API23 and one or both of the following is true: the device is in doze or the company
     * portal is in standby, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean isNetworkDisabledFromOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PowerManagerWrapper powerManagerWrapper = PowerManagerWrapper.getInstance();
            if (powerManagerWrapper.isDeviceIdleMode(mConnectionContext) &&
                    !powerManagerWrapper.isIgnoringBatteryOptimizations(mConnectionContext)) {
                Telemetry.emit((BaseEvent) new BaseEvent().put(
                        TelemetryEventStrings.Key.POWER_OPTIMIZATION,
                        String.valueOf(true)));
                return true;
            }
        }
        Telemetry.emit((BaseEvent) new BaseEvent().put(
                TelemetryEventStrings.Key.POWER_OPTIMIZATION,
                String.valueOf(false)));
        return false;
    }
}
