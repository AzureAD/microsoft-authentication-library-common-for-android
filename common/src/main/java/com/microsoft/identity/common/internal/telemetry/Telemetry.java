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

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.events.BaseEvent;
import com.microsoft.identity.common.internal.telemetry.observers.AggregatedTelemetryObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;
import com.microsoft.identity.common.internal.telemetry.observers.TestTelemetryObserver;
import com.microsoft.identity.common.internal.telemetry.rules.TelemetryPiiOiiRules;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_CORRELATION_ID;

public class Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();
    static volatile Telemetry singleton = null;
    //private TelemetryDispatcher mTelemetryDispatcher;
    private List<ITelemetryObserver> mObservers;
    //private Map<String, Properties> mAggregatedTelemetryMap;
    private List<Map<String, String>> mTelemetryRawDataMap;
    private TelemetryConfiguration mDefaultConfiguration;
    private final TelemetryContext mTelemetryContext;
    private final boolean mIsDebugging;
    private final static ExecutorService sTelemetryExecutor = Executors.newCachedThreadPool();

    private Telemetry(final TelemetryConfiguration configuration,
                      final TelemetryContext telemetryContext,
                      final boolean isDebugging) {
        mDefaultConfiguration = configuration;
        mTelemetryContext = telemetryContext;
        mIsDebugging = isDebugging;
        mTelemetryRawDataMap = new LinkedList<>();

        //TODO For test purpose Remove later
        addObserver(new TestTelemetryObserver());
        addObserver(new AggregatedTelemetryObserver());
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

    public TelemetryContext getTelemetryContext() {
        return mTelemetryContext;
    }

    public List<Map<String, String>> getRequestMap() {
        return mTelemetryRawDataMap;
    }

    /**
     * Register the observer to upload the telemetry data.
     * @param observer ITelemetryReceiver
     */
    public synchronized void addObserver(final ITelemetryObserver observer) {
        if (null == observer) {
            throw new IllegalArgumentException("Receiver instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null == mObservers) {
            mObservers = new ArrayList<>();
        }

        mObservers.add(observer);
    }

    public synchronized void removeObserver(final ITelemetryObserver observer) {
        if (null == observer || null == mObservers) {
            return;
        }

        mObservers.remove(observer);
    }

    public List<ITelemetryObserver> getObservers() {
        return mObservers;
    }

    /**
     * Emit the event into the telemetry raw data map.
     *
     * @param event BaseEvent object
     * @return the event reference for future properties modification.
     */
    public BaseEvent emit(final BaseEvent event) {
        mTelemetryRawDataMap.add(event.getProperties());
        return event;
    }


//    public void track(final @NonNull String requestId, final BaseEvent event) {
//        track(requestId, event.getClass().getSimpleName(), event, null);
//    }
//
//    public void track(final @NonNull String requestId,
//                      final @NonNull String eventName,
//                      final @NonNull String propertyKey,
//                      final @Nullable String propertyValue) {
//        if (StringUtil.isEmpty(requestId)) {
//            throw new IllegalArgumentException("request id must not be null or empty.");
//        }
//
//        if (StringUtil.isEmpty(eventName)) {
//            throw new IllegalArgumentException("event name must not be null or empty.");
//        }
//
//        if (StringUtil.isEmpty(propertyKey)) {
//            throw new IllegalArgumentException("eventName must not be null or empty.");
//        }
//
//        sTelemetryExecutor.submit(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        //enqueue the event
//                        final String key = getPropertyKey(requestId, eventName);
//                        if (mAggregatedTelemetryMap.get(key) == null)
//                        {
//                            mAggregatedTelemetryMap.put(key, new Properties().put(propertyKey, propertyValue));
//                        } else {
//                            mAggregatedTelemetryMap.get(key).put(propertyKey, propertyValue);
//                        }
//                    }
//                });
//    }
//
//    public void track(final String requestId,
//                      final String eventName,
//                      final BaseEvent event,
//                      final TelemetryConfiguration options) {
//        if (StringUtil.isEmpty(requestId)) {
//            throw new IllegalArgumentException("requestId must not be null or empty.");
//        }
//
//        sTelemetryExecutor.submit(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        //Overwrite the telemetry configuration if the passed-in options is not null.
//                        if (options != null) {
//                            mDefaultConfiguration = options;
//                        }
//
//                        if (null != event) {
//                            //enqueue the event
//                            final String key = getPropertyKey(requestId, eventName);
//                            if (mAggregatedTelemetryMap.get(key) == null)
//                            {
//                                mAggregatedTelemetryMap.put(key, event);
//                            } else {
//                                mAggregatedTelemetryMap.get(key).put(event);
//                            }
//                        }
//                    }
//                });
//    }

    /**
     * Flush the telemetry data based on the requestId to the observer.
     *
     * @param correlationId The request id should either passed by the client app through the API call
     *                  or generated by the API dispatcher.
     */
    public void flush(@NonNull final String correlationId) {
        if (null == mObservers) {
            Logger.warn(TAG, "No telemetry observer set.");
            return;
        }

        if (StringUtil.isEmpty(correlationId)) {
            Logger.warn(TAG, "No correlation id set.");
            return;
        }

        synchronized (this) {
            //check the configuration
            if (!mDefaultConfiguration.isDebugEnabled() && mIsDebugging) {
                return;
            }

            List<Map<String, String>> finalRawMap = new ArrayList<>();

            for(Map<String, String> event : mTelemetryRawDataMap) {
                if (correlationId.equalsIgnoreCase(event.get(TELEMETRY_KEY_CORRELATION_ID))) {
                    finalRawMap.add(applyPiiOiiRule(event));
                }
            }

            //Add the telemetry context to the telemetry data
            finalRawMap.add(applyPiiOiiRule(mTelemetryContext.getProperties()));

            for (ITelemetryObserver observer : mObservers) {
                observer.send(finalRawMap);
            }
        }
    }

    private Map<String, String> applyPiiOiiRule(final Map<String, String> properties) {
        if (mDefaultConfiguration.isPiiEnabled()) {
            Logger.warn(TAG, "Telemetry PII/OII is enabled by the developer.");
            return properties;
        }

        final Map<String, String> nonPiiProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!TelemetryPiiOiiRules.getInstance().isPiiOrOii(entry.getKey())) {
                nonPiiProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return nonPiiProperties;
    }

    /**
     * API for creating {@link Telemetry} instances.
     */
    public static class Builder {
        private Context mContext;
        private TelemetryConfiguration mDefaultConfiguration;
        private TelemetryContext mTelemetryContext;
        private Boolean mIsDebugging;

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

            mTelemetryContext = TelemetryContext.create(mContext);

            try {
                String packageName = context.getPackageName();
                int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
                mIsDebugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            } catch (final PackageManager.NameNotFoundException exception) {
                Logger.warn(TAG, "The application is not found from PackageManager.");
                mIsDebugging = true;
            }

            mDefaultConfiguration = new TelemetryConfiguration();
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
            return new Telemetry(
                    mDefaultConfiguration,
                    mTelemetryContext,
                    mIsDebugging
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
