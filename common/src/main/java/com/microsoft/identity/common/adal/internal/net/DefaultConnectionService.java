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
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.microsoft.identity.common.adal.internal.PowerManagerWrapper;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.events.BaseEvent;

import io.opentelemetry.api.metrics.LongCounter;

/**
 * Default connection service check network connectivity.
 * TODO: No need for {@link IConnectionService}. Interface was created for testing purpose.
 * Same purpose could be achieved via mocking the context. Since it's a public interface, should
 * be removed in the next major version update.
 * https://github.com/AzureAD/azure-activedirectory-library-for-android/issues/626
 */
public class DefaultConnectionService implements IConnectionService {

    private final Context mConnectionContext;
    private static final LongCounter sNetworkCheckFailureCount = OTelUtility.createLongCounter(
            "network_check_failure_count",
            "Number of times network was not available"
    );
    private static final LongCounter sNetworkCheckSuccessCount = OTelUtility.createLongCounter(
            "network_check_success_count",
            "Number of times network was available"
    );
    /**
     * Constructor of DefaultConnectionService.
     *
     * @param ctx Context
     */
    public DefaultConnectionService(Context ctx) {
        mConnectionContext = ctx;
    }

    /**
     * Check if the network connection is available.
     *
     * @return True if network connection available, false otherwise.
     */
    public boolean isConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mConnectionContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.DISABLE_NETWORK_CONNECTIVITY_CHECK)){
            // Skip the check.
            return true;
        }

        final boolean isConnectionAvailable;
        final boolean useNetworkCapabilityForNetworkCheck
                = CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.USE_NETWORK_CAPABILITY_FOR_NETWORK_CHECK);
        if (useNetworkCapabilityForNetworkCheck && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            final NetworkCapabilities networkCapabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            isConnectionAvailable =
                    networkCapabilities != null
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            if (isConnectionAvailable) {
                sNetworkCheckSuccessCount.add(1);
            } else {
                sNetworkCheckFailureCount.add(1);
            }
        } else {
            @SuppressWarnings("deprecation")
            final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            isConnectionAvailable = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        Telemetry.emit((BaseEvent) new BaseEvent().put(TelemetryEventStrings.Key.NETWORK_CONNECTION, String.valueOf(isConnectionAvailable)));
        return isConnectionAvailable;
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
