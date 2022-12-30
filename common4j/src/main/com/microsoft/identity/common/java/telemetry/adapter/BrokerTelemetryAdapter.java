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
package com.microsoft.identity.common.java.telemetry.adapter;

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.observers.IBrokerTelemetryObserver;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

/**
 * Adapter to aggregate events for broker telemetry. We make use of the {@link TelemetryAggregationAdapter} which
 * aggregates the events into MATS format.
 * <p>
 * We then send out the error events as single events since the {@link TelemetryAggregationAdapter} will pick the last error event
 * and add combine it with the aggregated event. This means that the event sent by {@link TelemetryAggregationAdapter} only contains
 * the last error that was dispatched.
 */
public class BrokerTelemetryAdapter extends TelemetryAggregationAdapter {
    public BrokerTelemetryAdapter(@NonNull IBrokerTelemetryObserver observer) {
        super(observer);
    }

    @Override
    public void process(@NonNull List<Map<String, String>> rawData) {
        final Map<String, String> aggregatedMap = aggregateEvent(rawData);

        // Filter out the error events first
        final List<Map<String, String>> errorEvents = filterErrorEvents(rawData, aggregatedMap);

        // collect the number of hits of the errors
        final List<Map<String, String>> countedErrors = countErrors(errorEvents);
        // publish the event
        for (final Map<String, String> errorEvent : countedErrors) {
            getObserver().onReceived(errorEvent);
        }

        // send out the aggregated event separately. This contains the tag of the error event that was
        // fired last, hence in case of a failure, we know which error caused it.
        getObserver().onReceived(aggregatedMap);
    }

    /**
     * Filters out error events from the list of events.
     */
    private List<Map<String, String>> filterErrorEvents(@NonNull final List<Map<String, String>> rawData, @NonNull final Map<String, String> aggregatedMap) {
        final List<Map<String, String>> errorEvents = new ArrayList<>();

        for (final Map<String, String> event : rawData) {
            if (TelemetryEventStrings.EventType.ERROR_EVENT.equals(event.get(TelemetryEventStrings.Key.EVENT_TYPE))) {
                final Map<String, String> errorEvent = new HashMap<>(aggregatedMap);
                errorEvent.putAll(event);
                errorEvent.put(TelemetryEventStrings.Key.IS_ERROR_EVENT, TelemetryEventStrings.Value.TRUE);

                errorEvents.add(errorEvent);
            }
        }
        return errorEvents;
    }

    /**
     * Errors with the same tag are essentially the same error, we don't need to send them to telemetry as different errors. Therefore
     * we shall add an {@link TelemetryEventStrings.Key#ERROR_COUNT} key that denotes the number of hits of the error.
     *
     * @param errorEvents the list of error events
     * @return a list containing unique error events (by tag) with a field denoting the number of hits of the error.
     */
    private List<Map<String, String>> countErrors(@NonNull final List<Map<String, String>> errorEvents) {
        final Map<String, Map<String, String>> countedErrors = new HashMap<>();

        for (Map<String, String> errorEvent : errorEvents) {
            final String errorTag = errorEvent.get(TelemetryEventStrings.Key.ERROR_TAG);

            int errorCount = 1;
            final Map<String, String> errorProperties = countedErrors.containsKey(errorTag) ? countedErrors.get(errorTag) : applyAggregationRule(errorEvent);

            if (countedErrors.containsKey(errorTag)) {
                errorCount += Integer.parseInt(errorProperties.get(TelemetryEventStrings.Key.ERROR_COUNT));
            } else {
                countedErrors.put(errorTag, errorProperties);
            }
            errorProperties.put(TelemetryEventStrings.Key.ERROR_COUNT, String.valueOf(errorCount));
        }

        return new ArrayList<>(countedErrors.values());
    }
}
