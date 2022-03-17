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
package com.microsoft.identity.common.internal.telemetry.relay;

import android.content.Context;

import com.microsoft.applications.telemetry.EventProperties;
import com.microsoft.applications.telemetry.ILogger;
import com.microsoft.applications.telemetry.LogConfiguration;
import com.microsoft.applications.telemetry.LogManager;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.relay.filters.DefaultAriaTelemetryFilter;
import com.microsoft.identity.common.java.telemetry.relay.AbstractTelemetryRelayClient;
import com.microsoft.identity.common.java.telemetry.relay.TelemetryRelayClientException;

import java.util.Map;

/**
 * A relay client to send events to Aria
 */
public class AriaTelemetryRelayClient extends AbstractTelemetryRelayClient {
    private static final String ARIA_TABLE = "android_event";
    private static final String TAG = AriaTelemetryRelayClient.class.getSimpleName();

    private volatile static AriaTelemetryRelayClient mInstance = null;

    private ILogger logger;
    private final Context context;
    private final String ariaToken;
    private final LogConfiguration logConfiguration;

    private AriaTelemetryRelayClient(Context context, String ariaToken, LogConfiguration logConfiguration) {
        this.context = context.getApplicationContext();
        this.ariaToken = ariaToken;
        this.logConfiguration = logConfiguration;
    }

    public static AriaTelemetryRelayClient getInstance(Context context, String ariaToken, LogConfiguration logConfiguration) {
        if (mInstance == null) {
            mInstance = new AriaTelemetryRelayClient(context, ariaToken, logConfiguration);
        }
        return mInstance;
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }

    public ILogger getLogger() {
        return logger;
    }

    @Override
    public void initialize() throws TelemetryRelayClientException {
        Logger.info(TAG + ":initialize", "Initializing Aria relay client.");
        try {
            logger = LogManager.initialize(context, ariaToken, logConfiguration);
        } catch (Exception exception) {
            logger = LogManager.getLogger(ariaToken, "");

            if (logger == null) {
                throw new TelemetryRelayClientException("Aria failed to initialize LogManager",
                        exception,
                        TelemetryRelayClientException.INITIALIZATION_FAILED);
            }
        }

        this.addFilter(new DefaultAriaTelemetryFilter());
    }

    @Override
    public void relayTelemetryEvent(Map<String, String> event) {
        final EventProperties eventProperties = new EventProperties(ARIA_TABLE);
        for (final Map.Entry<String, String> entry : event.entrySet()) {
            eventProperties.setProperty(entry.getKey(), entry.getValue());
        }
        logger.logEvent(eventProperties);
        LogManager.flush();
    }


    @Override
    public void unInitialize() {
        Logger.info(TAG + ":unInitialize", "Tearing down Aria relay client.");
        LogManager.flushAndTeardown();
    }
}
