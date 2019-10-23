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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.BuildConfig;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.adapter.TelemetryAggregationAdapter;
import com.microsoft.identity.common.internal.telemetry.adapter.TelemetryDefaultAdapter;
import com.microsoft.identity.common.internal.telemetry.events.BaseEvent;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;
import com.microsoft.identity.common.internal.telemetry.rules.TelemetryPiiOiiRules;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.microsoft.identity.common.internal.logging.DiagnosticContext.CORRELATION_ID;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Key;

public class Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();
    private static volatile Telemetry sTelemetryInstance = null;
    private static Queue<ITelemetryObserver> mObservers;
    private Queue<Map<String, String>> mTelemetryRawDataMap;
    private TelemetryConfiguration mDefaultConfiguration;
    private TelemetryContext mTelemetryContext;
    private boolean mIsDebugging;

    //if the telemetry configuration is not set
    private final boolean mIsTelemetryEnabled;

    private Telemetry(final Builder builder) {
        if (builder == null
                || builder.mTelemetryContext == null
                || builder.mDefaultConfiguration == null) {
            //we do not want to throw exception for telemetry.
            Logger.warn(
                    TAG,
                    "Telemetry is disabled because the Telemetry context or configuration is null"
            );
            mIsTelemetryEnabled = false;
        } else {
            mIsTelemetryEnabled = true;
            mDefaultConfiguration = builder.mDefaultConfiguration;
            mTelemetryContext = builder.mTelemetryContext;
            mIsDebugging = builder.mIsDebugging;
            mTelemetryRawDataMap = new ConcurrentLinkedQueue<>();
        }
    }

    /**
     * Prepares instance using builder.
     **/
    private synchronized static Telemetry prepareInstance(Builder builder) {
        sTelemetryInstance = new Telemetry(builder);

        return sTelemetryInstance;
    }

    /**
     * This is for getting instance of Telemetry
     **/
    public synchronized static Telemetry getInstance() {
        // If sTelemetryInstance is not initialized, telemetry will be disabled.
        if (sTelemetryInstance == null) {
            new Builder().build();
        }

        return sTelemetryInstance;
    }

    private Queue<Map<String, String>> getRequestMap() {
        return mTelemetryRawDataMap;
    }

    /**
     * Register the observer to upload the telemetry data.
     *
     * @param observer ITelemetryObserver
     */
    public void addObserver(final ITelemetryObserver observer) {
        if (null == observer) {
            throw new IllegalArgumentException("Telemetry Observer instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null == mObservers) {
            mObservers = new ConcurrentLinkedQueue<>();
        }

        mObservers.add(observer);
    }

    /**
     * Remove the observer in a given type.
     *
     * @param cls type of the observer.
     */
    public void removeObserver(final Class<?> cls) {
        if (null == cls || null == mObservers) {
            Logger.warn(
                    TAG,
                    "Unable to remove the observe. Either the observer is null or the observer list is empty."
            );
            return;
        }

        final Iterator<ITelemetryObserver> observerIterator = mObservers.iterator();

        while (observerIterator.hasNext()) {
            if (observerIterator.next().getClass() == cls) {
                Logger.verbose(TAG, "The [" + cls.getSimpleName() + "] observer is removed.");
                observerIterator.remove();
            }
        }
    }

    /**
     * Remove the passed in observer from the list.
     *
     * @param observer ITelemetryObserver object.
     */
    public void removeObserver(final ITelemetryObserver observer) {
        if (null == observer || null == mObservers) {
            Logger.warn(
                    TAG,
                    "Unable to remove the observer. Either the observer is null or the observer list is empty."
            );
            return;
        }

        mObservers.remove(observer);
    }

    @VisibleForTesting
    void removeAllObservers() {
        if (mObservers == null) {
            return;
        }

        mObservers.clear();
    }

    /**
     * Return the list of observers registered.
     *
     * @return List of ITelemetryObserver object.
     */
    public List<ITelemetryObserver> getObservers() {
        List observersList;
        if (mObservers != null) {
            observersList = new CopyOnWriteArrayList(mObservers);
        } else {
            observersList = new CopyOnWriteArrayList();
        }
        return Collections.unmodifiableList(observersList);
    }

    /**
     * Emit the event into the telemetry raw data map.
     *
     * @param event BaseEvent object
     * @return the event reference for future properties modification.
     */
    public static void emit(final BaseEvent event) {
        if (getInstance().mIsTelemetryEnabled) {
            //only enqueue the telemetry properties when the telemetry is enabled.
            getInstance().getRequestMap().add(event.getProperties());
        }
    }

    /**
     * Flush the telemetry data of current correlation id to the observers.
     */
    public void flush() {
        if (getInstance().mIsTelemetryEnabled) {
            flush(DiagnosticContext.getRequestContext().get(CORRELATION_ID));
        }
    }

    /**
     * Flush the telemetry data based on the correlation id to the observers.
     *
     * @param correlationId The correlation id should either passed by the client app through the API call
     *                      or generated by the API dispatcher.
     */
    public void flush(@NonNull final String correlationId) {
        if (!mIsTelemetryEnabled) {
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

        //check the configuration
        if (!mDefaultConfiguration.isDebugEnabled() && mIsDebugging) {
            return;
        }

        List<Map<String, String>> finalRawMap = new CopyOnWriteArrayList<>();

        for (Iterator<Map<String, String>> iterator = mTelemetryRawDataMap.iterator(); iterator.hasNext(); ) {
            Map<String, String> event = iterator.next();
            if (correlationId.equalsIgnoreCase(event.get(Key.CORRELATION_ID))) {
                finalRawMap.add(applyPiiOiiRule(event));
                iterator.remove();
            }
        }

        //Add the telemetry context to the telemetry data
        finalRawMap.add(applyPiiOiiRule(mTelemetryContext.getProperties()));

        for (ITelemetryObserver observer : mObservers) {
            if (observer instanceof ITelemetryAggregatedObserver) {
                new TelemetryAggregationAdapter((ITelemetryAggregatedObserver) observer).process(finalRawMap);
            } else if (observer instanceof ITelemetryDefaultObserver) {
                new TelemetryDefaultAdapter((ITelemetryDefaultObserver) observer).process(finalRawMap);
            } else {
                Logger.warn(TAG, "Unknown observer type: " + observer.getClass());
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
                mIsDebugging = BuildConfig.DEBUG;
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
}