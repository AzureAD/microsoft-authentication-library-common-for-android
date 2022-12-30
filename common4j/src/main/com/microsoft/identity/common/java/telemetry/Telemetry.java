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
package com.microsoft.identity.common.java.telemetry;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.telemetry.adapter.BrokerTelemetryAdapter;
import com.microsoft.identity.common.java.telemetry.observers.IBrokerTelemetryObserver;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.telemetry.adapter.TelemetryAggregationAdapter;
import com.microsoft.identity.common.java.telemetry.adapter.TelemetryDefaultAdapter;
import com.microsoft.identity.common.java.telemetry.events.BaseEvent;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryObserver;
import com.microsoft.identity.common.java.telemetry.rules.TelemetryPiiOiiRules;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.logging.DiagnosticContext;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.microsoft.identity.common.java.logging.DiagnosticContext.CORRELATION_ID;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;

/**
 * A singleton class for logging Telemetry.
 * Must be instantiated via {@link Telemetry.Builder} before use.
 */
public class Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();
    private static volatile Telemetry sTelemetryInstance = null;

    // Suppressing rawtype warnings due to the generic type ITelemetryObserver
    @SuppressWarnings(WarningType.rawtype_warning)
    private static Queue<ITelemetryObserver> mObservers;

    private Queue<Map<String, String>> mTelemetryRawDataMap;
    private TelemetryConfiguration mDefaultConfiguration;
    private AbstractTelemetryContext mTelemetryContext;
    private boolean mIsDebugging;

    //if the telemetry configuration is not set
    private final boolean mIsTelemetryEnabled;

    protected Telemetry() {
        // Added for backcompat (with android-common code).
        // Once that Telemetry class is removed. Remove this too!
        mIsTelemetryEnabled = false;
    }

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
    private synchronized static Telemetry prepareInstance(final Builder builder) {
        if (sTelemetryInstance == null || !sTelemetryInstance.mIsTelemetryEnabled) {
            sTelemetryInstance = new Telemetry(builder);
        }

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

    private synchronized Queue<Map<String, String>> getRequestMap() {
        return mTelemetryRawDataMap;
    }

    /**
     * Register the observer to upload the telemetry data.
     *
     * @param observer ITelemetryObserver
     */
    public synchronized void addObserver(@SuppressWarnings(WarningType.rawtype_warning) final ITelemetryObserver observer) {
        if (null == observer) {
            throw new IllegalArgumentException("Telemetry Observer instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null == mObservers) {
            mObservers = new ConcurrentLinkedQueue<>();
        }

        if (!mObservers.contains(observer))
            mObservers.add(observer);
    }

    /**
     * Remove the observer in a given type.
     *
     * @param cls type of the observer.
     */
    public synchronized void removeObserver(final Class<?> cls) {
        final String methodName = ":removeObserver";

        if (null == cls || null == mObservers || mObservers.isEmpty()) {
            Logger.warn(
                    TAG + methodName,
                    "Unable to remove the observe. Either the observer is null or the observer list is empty."
            );
            return;
        }

        // Suppressing rawtype warnings due to the generic type ITelemetryObserver
        @SuppressWarnings(WarningType.rawtype_warning) final Iterator<ITelemetryObserver> observerIterator = mObservers.iterator();

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
    public synchronized void removeObserver(@SuppressWarnings(WarningType.rawtype_warning) final ITelemetryObserver observer) {
        final String methodName = ":removeObserver";

        if (null == observer || null == mObservers || mObservers.isEmpty()) {
            Logger.warn(
                    TAG + methodName,
                    "Unable to remove the observer. Either the observer is null or the observer list is empty."
            );
            return;
        }

        mObservers.remove(observer);
    }

    // Visible for testing.
    public synchronized void removeAllObservers() {
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
    // Suppressing rawtype warnings due to the generic type ITelemetryObserver
    // Suppressing unchecked warnings as generic type not provided for CopyOnWriteArrayList and Collections
    @SuppressWarnings({WarningType.rawtype_warning, WarningType.unchecked_warning})
    public synchronized List<ITelemetryObserver> getObservers() {
        List<ITelemetryObserver> observersList;
        if (mObservers != null) {
            observersList = new CopyOnWriteArrayList<ITelemetryObserver>(mObservers);
        } else {
            observersList = new CopyOnWriteArrayList<ITelemetryObserver>();
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
            flush(DiagnosticContext.INSTANCE.getRequestContext().get(CORRELATION_ID));
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

        if (StringUtil.isNullOrEmpty(correlationId)) {
            Logger.warn(TAG, "No correlation id set.");
            return;
        }

        //check the configuration
        if (!mDefaultConfiguration.isDebugEnabled() && mIsDebugging) {
            return;
        }

        final List<Map<String, String>> finalRawMap = new CopyOnWriteArrayList<>();

        for (Iterator<Map<String, String>> iterator = mTelemetryRawDataMap.iterator(); iterator.hasNext(); ) {
            Map<String, String> event = iterator.next();
            if (correlationId.equalsIgnoreCase(event.get(Key.CORRELATION_ID))) {
                finalRawMap.add(applyPiiOiiRule(event));
                iterator.remove();
            }
        }

        processRawMap(finalRawMap);
    }

    /**
     * Pass the final raw map to the observers.
     */
    private void processRawMap(final List<Map<String, String>> finalRawMap) {
        //Add the telemetry context to the telemetry data
        finalRawMap.add(applyPiiOiiRule(mTelemetryContext.getProperties()));

        if (null == mObservers) {
            Logger.warn(TAG, "No telemetry observer set.");
            return;
        }

        for (@SuppressWarnings(WarningType.rawtype_warning) ITelemetryObserver observer : mObservers) {
            if (observer instanceof IBrokerTelemetryObserver) {
                new BrokerTelemetryAdapter((IBrokerTelemetryObserver) observer).process(finalRawMap);
            } else if (observer instanceof ITelemetryAggregatedObserver) {
                new TelemetryAggregationAdapter((ITelemetryAggregatedObserver) observer).process(finalRawMap);
            } else if (observer instanceof ITelemetryDefaultObserver) {
                new TelemetryDefaultAdapter((ITelemetryDefaultObserver) observer).process(finalRawMap);
            } else {
                Logger.warn(TAG, "Unknown observer type: " + observer.getClass());
            }
        }
    }

    /**
     * Get telemetry data of current correlation id.
     */
    public List<Map<String, String>> getMap() {
        if (getInstance().mIsTelemetryEnabled) {
            return getMap(DiagnosticContext.INSTANCE.getRequestContext().get(CORRELATION_ID));
        }
        return Collections.emptyList();
    }


    /**
     * Get the Telemetry Map for a correlationId.
     *
     * @param correlationId The correlation id should either passed by the client app through the API call
     *                      or generated by the API dispatcher.
     */
    public List<Map<String, String>> getMap(@NonNull final String correlationId) {
        if (!mIsTelemetryEnabled) {
            return Collections.emptyList();
        }

        if (StringUtil.isNullOrEmpty(correlationId)) {
            Logger.warn(TAG, "No correlation id set.");
            return Collections.emptyList();
        }

        //check the configuration
        if (!mDefaultConfiguration.isDebugEnabled() && mIsDebugging) {
            return Collections.emptyList();
        }

        List<Map<String, String>> finalRawMap = new ArrayList<>();

        for (final Iterator<Map<String, String>> iterator = mTelemetryRawDataMap.iterator(); iterator.hasNext(); ) {
            final Map<String, String> event = iterator.next();
            if (correlationId.equalsIgnoreCase(event.get(Key.CORRELATION_ID))) {
                finalRawMap.add(applyPiiOiiRule(event));
            }
        }
        return finalRawMap;
    }

    private Map<String, String> applyPiiOiiRule(final Map<String, String> properties) {
        if (mDefaultConfiguration.isPiiEnabled()) {
            Logger.warn(TAG, "Telemetry PII/OII is enabled by the developer.");
            return properties;
        }

        final Map<String, String> nonPiiProperties = new HashMap<>();
        for (final Map.Entry<String, String> entry : properties.entrySet()) {
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
        private TelemetryConfiguration mDefaultConfiguration;
        private AbstractTelemetryContext mTelemetryContext;
        private Boolean mIsDebugging;

        public Builder() {
        }

        public Builder withTelemetryContext(final AbstractTelemetryContext context) {
            mTelemetryContext = context;
            return this;
        }

        /**
         * Specify the default options for all calls.
         */
        public Builder isDebugging(final boolean isDebugging) {
            mIsDebugging = isDebugging;
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
