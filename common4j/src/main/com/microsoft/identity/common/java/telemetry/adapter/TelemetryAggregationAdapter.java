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
package com.microsoft.identity.common.java.telemetry.adapter;

import lombok.NonNull;

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.java.telemetry.rules.TelemetryAggregationRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;

@Deprecated
public class TelemetryAggregationAdapter implements ITelemetryAdapter<List<Map<String, String>>> {
    private ITelemetryAggregatedObserver mObserver;
    private static final String START = "start";
    private static final String END = "end";

    public TelemetryAggregationAdapter(@NonNull final ITelemetryAggregatedObserver observer) {
        mObserver = observer;
    }

    public ITelemetryAggregatedObserver getObserver() {
        return mObserver;
    }

    public void process(@NonNull final List<Map<String, String>> rawData) {
        mObserver.onReceived(aggregateEvent(rawData));
    }

    protected Map<String, String> aggregateEvent(@NonNull final List<Map<String, String>> rawData) {
        final Map<String, String> aggregatedData = new HashMap<>();
        final Map<String, String> responseTimeMap = new HashMap<>();

        for (Map<String, String> event : rawData) {
            final String eventName = event.get(Key.EVENT_NAME);
            final String eventType = event.get(Key.EVENT_TYPE);

            if (StringUtil.isNullOrEmpty(eventName)) {
                aggregatedData.putAll(applyAggregationRule(event));
                continue;
            }

            //Count the events. Only check the "*_start_event" when counting.
            if (eventName.contains(START)) {
                final String eventTypeCountKey = eventType + "_count";
                final String currentEventTypeCount = aggregatedData.get(eventTypeCountKey);
                final int currentEventTypeCountValue = Integer.parseInt(currentEventTypeCount == null ? "0" : currentEventTypeCount);
                final int newEventTypeCountValue = currentEventTypeCountValue + 1;
                aggregatedData.put(
                        eventTypeCountKey,
                        String.valueOf(newEventTypeCountValue)
                );
            }

            final String isSuccessful = event.containsKey(Key.IS_SUCCESSFUL) ? event.get(Key.IS_SUCCESSFUL) : TelemetryEventStrings.Value.FALSE;
            aggregatedData.put(
                    eventType + Key.IS_SUCCESSFUL,
                    StringUtil.isNullOrEmpty(isSuccessful) ? TelemetryEventStrings.Value.FALSE : isSuccessful
            );

            trackEventResponseTime(responseTimeMap, event);

            aggregatedData.putAll(applyAggregationRule(event));
        }

        calculateEventResponseTime(responseTimeMap, aggregatedData);

        return aggregatedData;
    }

    protected Map<String, String> applyAggregationRule(@NonNull final Map<String, String> properties) {
        final Map<String, String> nonPiiProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!StringUtil.isNullOrEmpty(entry.getValue())
                    && !TelemetryAggregationRules.getInstance().isRedundant(entry.getKey())) {
                nonPiiProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return nonPiiProperties;
    }

    //The response time is the duration of the last occurrence.
    private void trackEventResponseTime(@NonNull final Map<String, String> responseTimeMap,
                                        @NonNull final Map<String, String> event) {
        final String eventName = event.get(Key.EVENT_NAME);
        final String eventType = event.get(Key.EVENT_TYPE);

        if (eventName != null && eventName.contains(START)) {
            final String eventStartTime = eventType + "_start_time";
            responseTimeMap.put(
                    eventStartTime,
                    event.get(Key.OCCUR_TIME)
            );
        }

        if (eventName != null && eventName.contains(END)) {
            final String eventEndTime = eventType + "_end_time";
            responseTimeMap.put(
                    eventEndTime,
                    event.get(Key.OCCUR_TIME)
            );
        }
    }

    /**
     * The responseTimeMap has the start time and end time of all event types.
     * This function is used to calculated the response time of each event type.
     * Where event_type_response_time = event_type_end_time - event_type_start_time.
     * The response time of each event type is added into the result aggregated data map.
     *
     * @param responseTimeMap has the start time and end time of each event type.
     * @param aggregatedData  the result map of the aggregation adapter.
     */
    private void calculateEventResponseTime(@NonNull final Map<String, String> responseTimeMap,
                                            @NonNull final Map<String, String> aggregatedData) {
        for (Map.Entry<String, String> entry : responseTimeMap.entrySet()) {
            final String entryKey = entry.getKey();
            if (entryKey.contains(START)) {
                final String eventEndTimeKey = entryKey.replace(START, END);
                final String eventEndTimeValue = responseTimeMap.get(eventEndTimeKey);
                if (eventEndTimeValue != null) {
                    final String eventResponseTimeKey = entryKey.replace(START, "response");
                    final long startTime = Long.parseLong(entry.getValue());
                    final long endTime = Long.parseLong(eventEndTimeValue);
                    aggregatedData.put(eventResponseTimeKey, String.valueOf(endTime - startTime));
                }
            }
        }
    }
}
