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

import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

/**
 * Base class for relaying telemetry events.
 */
public abstract class AbstractTelemetryRelayClient implements ITelemetryAggregatedObserver {

    private final List<ITelemetryEventFilter> predicates = new ArrayList<>();

    /**
     * Handle initialization of the relay client before being registered as an observer.
     *
     * @throws TelemetryRelayClientException when initialization failed, with the appropriate
     *                                       error code.
     */
    public abstract void initialize() throws TelemetryRelayClientException;

    /**
     * Handle detach from the telemetry system.
     * This would be where we de-register the relay system from sending any more events.
     */
    public abstract void unInitialize();


    /**
     * Handle the relay of telemetry events.
     *
     * @param event the map containing the event key value pairs.
     */
    public abstract void relayTelemetryEvent(Map<String, String> event);


    public void addFilter(@NonNull ITelemetryEventFilter predicate) {
        predicates.add(predicate);
    }

    public void removeFilter(@NonNull ITelemetryEventFilter predicate) {
        predicates.remove(predicate);
    }

    public boolean shouldRelayEvent(final Map<String, String> event) {
        for (final ITelemetryEventFilter predicate : predicates) {
            if (!predicate.shouldRelayEvent(event)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onReceived(final Map<String, String> telemetryData) {
        if (shouldRelayEvent(telemetryData)) {
            relayTelemetryEvent(telemetryData);
        }
    }
}
