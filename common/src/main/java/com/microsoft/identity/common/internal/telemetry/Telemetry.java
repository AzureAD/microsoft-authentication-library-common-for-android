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
package com.microsoft.identity.common.internal.telemetry;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Telemetry {
    static volatile Telemetry singleton = null;
    private TelemetryDispatcher mTelemetryDispatcher;
    private RequestValueMap mRequestMap;
    private TelemetryConfiguration mDefaultConfiguration;
    private final TelemetryContext mTelemetryContext;
    private final static ExecutorService sTelemetryExecutor = Executors.newCachedThreadPool();

    private Telemetry(final TelemetryConfiguration configuration,
                      final TelemetryContext telemetryContext) {
        mDefaultConfiguration = configuration;
        mTelemetryContext = telemetryContext;
    }

    public static Telemetry with(Context context) {
        if (singleton == null) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            synchronized (Telemetry.class) {
                if (singleton == null) {
                    Builder builder = new Builder(context);
                    singleton = builder.build();
                }
            }
        }
        return singleton;
    }

    /**
     * Register the receiver to flush the telemetry data.
     * @param receiver ITelemetryReceiver
     */
    public synchronized void registerReceiver(final ITelemetryReceiver receiver) {
        if (null == receiver) {
            throw new IllegalArgumentException("Receiver instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null != mTelemetryDispatcher) {
            throw new IllegalStateException(
                    ITelemetryReceiver.class.getSimpleName()
                            + " instances are not swappable at this time."
            );
        }

        // set this dispatcher
        mTelemetryDispatcher = new TelemetryDispatcher(receiver);
    }

    public void track(final @NonNull String event,
                      final @Nullable Properties properties,
                      final @NonNull String requestId,
                      final @Nullable TelemetryConfiguration options) {

        if (StringUtil.isEmpty(event)) {
            throw new IllegalArgumentException("event must not be null or empty.");
        }

        sTelemetryExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        final TelemetryConfiguration finalOptions;
                        if (options == null) {
                            finalOptions = mDefaultConfiguration;
                        } else {
                            finalOptions = options;
                        }

                        final Properties finalProperties;
                        if (properties == null) {
                            finalProperties = new Properties(new HashMap<String, String>());
                        } else {
                            finalProperties = properties;
                        }

                        //enqueue the properties to the event of the requestId
//                        TrackPayload.Builder builder =
//                                new TrackPayload.Builder().event(event).properties(finalProperties);
//                        fillAndEnqueue(builder, finalOptions);
                    }
                });
    }

    public void track(String eventName, Map<String, String> properties) {

    }

    public void flush(final String requestId) {
        if (null == mTelemetryDispatcher) {
            return;
        }

        synchronized (this) {

            // check for orphaned events...
            final List<Event> orphanedEvents = collateOrphanedEvents(requestId);
            // Add the OrphanedEvents to the existing IEventList
            if (null == mCompletedEvents.get(requestId)) {
                Logger.warning(TAG, null, "No completed Events returned for RequestId.");
                return;
            }

            mCompletedEvents.get(requestId).addAll(orphanedEvents);

            final List<Event> eventsToFlush = mCompletedEvents.remove(requestId);

            if (mTelemetryOnFailureOnly) {
                // iterate over Events, if the ApiEvent was successful, don't dispatch
                boolean shouldRemoveEvents = false;

                for (Event event : eventsToFlush) {
                    if (event instanceof ApiEvent) {
                        ApiEvent apiEvent = (ApiEvent) event;
                        shouldRemoveEvents = apiEvent.wasSuccessful();
                        break;
                    }
                }

                if (shouldRemoveEvents) {
                    eventsToFlush.clear();
                }
            }

            if (!eventsToFlush.isEmpty()) {
                //append the telemetry context.
                mTelemetryDispatcher.dispatch(eventsToFlush);
            }
        }
    }

    /**
     * API for creating {@link Telemetry} instances.
     */
    public static class Builder {
        private Context mContext;
        private TelemetryConfiguration mDefaultConfiguration;
        private TelemetryContext mTelemetryContext;

        public Builder(final Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            if (!hasPermission(context, Manifest.permission.INTERNET)) {
                throw new IllegalArgumentException("INTERNET permission is required.");
            }
            mContext = context.getApplicationContext();
            if (mContext == null) {
                throw new IllegalArgumentException("Application context must not be null.");
            }
        }

        /**
         * Specify the default options for all calls.
         */
        public Builder defaultConfiguration(final TelemetryConfiguration configuration) {
            if (configuration == null) {
                throw new IllegalArgumentException("defaultOptions must not be null.");
            }

            mDefaultConfiguration = configuration;
            return this;
        }
        /**
         * Create a {@link Telemetry} client.
         */
        public Telemetry build() {
            mTelemetryContext = TelemetryContext.create(mContext);

            return new Telemetry(
                    mDefaultConfiguration,
                    mTelemetryContext
            );
        }
    }

    /**
     * Returns true if the application has the given permission.
     */
    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
    }
}
