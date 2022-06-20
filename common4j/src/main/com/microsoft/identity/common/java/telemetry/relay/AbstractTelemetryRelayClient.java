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
package com.microsoft.identity.common.java.telemetry.relay;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * A relay client gives the flexibility to send telemetry events to a database.
 * It extends a {@link ITelemetryObserver} and applies filter on every event captured before relaying the event to the database.
 *
 * @param <T>
 */
public abstract class AbstractTelemetryRelayClient<T> implements ITelemetryObserver<T> {

    private static final String TAG = AbstractTelemetryRelayClient.class.getSimpleName();
    // Filters to apply on the events captured.
    // This basically performs an AND operation on the filters to determine whether the event will be relayed.
    private final List<ITelemetryEventFilter<T>> eventFilters = new ArrayList<>();

    @Override
    public void onReceived(T telemetryData) {
        final String methodTag = TAG + ":onReceived";

        for (final ITelemetryEventFilter<T> filter : eventFilters) {
            // Prevent event relay if any of the filters returns false.
            if (!filter.shouldRelay(telemetryData)) {
                return;
            }
        }

        try {
            relayEvent(telemetryData);
        } catch (TelemetryRelayException e) {
            Logger.error(methodTag, "Error relaying telemetry data", e);
        }
    }

    /**
     * Remove an event filter from the list of filters
     */
    public void removeFilter(ITelemetryEventFilter<T> filter) {
        this.eventFilters.remove(filter);
    }

    /**
     * Add an event filter to the list of filters
     */
    public void addFilter(ITelemetryEventFilter<T> filter) {
        this.eventFilters.add(filter);
    }

    /**
     * Clear all the filters in this relay client
     */
    public void clearFilters() {
        this.eventFilters.clear();
    }

    /**
     * Initialize the relay client. This should only happen once.
     */
    public abstract void initialize() throws TelemetryRelayException;

    /**
     * Returns true if the relay client has been initialized.
     */
    public abstract boolean isInitialized();

    /**
     * Invoked when an event is ready to be relayed
     */
    public abstract void relayEvent(final T eventData) throws TelemetryRelayException;

    /**
     * Flush any pending telemetry events in memory to disk and shutdown the telemetry system.
     * This method can be invoked when the application is being closed.
     */
    public abstract void flushAndTeardown();
}
