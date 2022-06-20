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
 * <p>
 * On the server side, we will perform an INNER JOIN based on the correlation id in order to identify all the events that were
 * captured in the flow.
 */
public class BrokerTelemetryAdapter extends TelemetryAggregationAdapter {
    public BrokerTelemetryAdapter(@NonNull IBrokerTelemetryObserver observer) {
        super(observer);
    }

    @Override
    public void process(@NonNull List<Map<String, String>> rawData) {
        super.process(rawData);

        Map<String, Map<String, String>> errorEvents = new HashMap<>();

        // aggregate error events by counting the number of occurrences of the error.
        for (Map<String, String> event : rawData) {
            final String eventType = event.get(TelemetryEventStrings.Key.EVENT_TYPE);

            if (TelemetryEventStrings.EventType.ERROR_EVENT.equals(eventType)) {
                final String errorTag = event.get(TelemetryEventStrings.Key.ERROR_TAG);

                if (errorEvents.containsKey(errorTag)) {
                    final Map<String, String> errorEventMap = errorEvents.get(errorTag);
                    errorEventMap.put(
                            TelemetryEventStrings.Key.ERROR_COUNT,
                            String.valueOf(
                                    Integer.parseInt(errorEventMap.get(TelemetryEventStrings.Key.ERROR_COUNT)) + 1
                            )
                    );
                } else {
                    final Map<String, String> errorEventMap = applyAggregationRule(event);
                    errorEventMap.put(TelemetryEventStrings.Key.ERROR_COUNT, String.valueOf(1));

                    errorEvents.put(errorTag, errorEventMap);
                }
            }
        }

        // send out the error events
        for (Map<String, String> errorEvent : errorEvents.values()) {
            getObserver().onReceived(errorEvent);
        }
    }
}
