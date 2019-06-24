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

import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.events.BaseEvent;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;
import com.microsoft.identity.common.internal.telemetry.rules.TelemetryPiiOiiRules;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.microsoft.identity.common.internal.logging.DiagnosticContext.CORRELATION_ID;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_CORRELATION_ID;

public class Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();
    private static volatile Telemetry singleton = null;
    private List<ITelemetryObserver> mObservers;
    private List<Map<String, String>> mTelemetryRawDataMap;
    private TelemetryConfiguration mDefaultConfiguration;
    private TelemetryContext mTelemetryContext;
    private boolean mIsDebugging;

    //if the telemetry configuration is not set
    private final boolean mIsTelemetryEnabled;

    private Telemetry(final Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        if (builder.mTelemetryContext == null) {
            throw new IllegalArgumentException("Telemetry context cannot be null");
        }

        if (builder.mDefaultConfiguration == null) {
            //if the telemetry configuration is not set in the json, disable the telemetry.
            mIsTelemetryEnabled = false;
        } else {
            mIsTelemetryEnabled = true;
            mDefaultConfiguration = builder.mDefaultConfiguration;
            mTelemetryContext = builder.mTelemetryContext;
            mIsDebugging = builder.mIsDebugging;
            mTelemetryRawDataMap = new LinkedList<>();
            mObservers = new LinkedList<>();
        }
    }

    /**
     * Prepares instance using builder.
     **/
    private static Telemetry prepareInstance(Builder builder) {
        if (singleton == null) {
            synchronized (Telemetry.class) {
                if (singleton == null) {
                    singleton = new Telemetry(builder);
                }
            }
        }
        return singleton;
    }

    /**
     * This is for getting instance of Telemetry
     **/
    public static Telemetry getInstance() {
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
     *
     * @param observer ITelemetryReceiver
     */
    public synchronized void addObserver(final ITelemetryObserver observer) {
        if (null == observer) {
            throw new IllegalArgumentException("Receiver instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null == mObservers) {
            mObservers = new LinkedList<>();
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
    public static BaseEvent emit(final BaseEvent event) {
        if (getInstance().mIsTelemetryEnabled) {
            //only enqueue the telemetry properties when the telemetry is enabled.
            getInstance().getRequestMap().add(event.getProperties());
        }

        return event;
    }

    public void flush() {
        if (getInstance().mIsTelemetryEnabled) {
            flush(DiagnosticContext.getRequestContext().get(CORRELATION_ID));
        }
    }

    /**
     * Flush the telemetry data based on the requestId to the observer.
     *
     * @param correlationId The request id should either passed by the client app through the API call
     *                      or generated by the API dispatcher.
     */
    public void flush(@NonNull final String correlationId) {
        if (!mIsTelemetryEnabled) {
            Logger.warn(TAG, "Telemetry is disabled.");
            return;
        }
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

            for (Map<String, String> event : mTelemetryRawDataMap) {
                if (correlationId.equalsIgnoreCase(event.get(TELEMETRY_KEY_CORRELATION_ID))) {
                    finalRawMap.add(applyPiiOiiRule(event));
                    finalRawMap.remove(event);
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

        public Builder() {
        }

        public Builder withContext(final Context context) {
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

            return this;
        }

        /**
         * Specify the default options for all calls.
         */
        public Builder defaultConfiguration(final TelemetryConfiguration configuration) {
            mDefaultConfiguration = configuration;
            return this;
        }

        /**
         * Create a {@link Telemetry} client.
         */
        public Telemetry build() throws IllegalArgumentException {
            return prepareInstance(this);
        }
    }

    /**
     * Returns true if the application has the given permission.
     */
    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED;
    }
}
