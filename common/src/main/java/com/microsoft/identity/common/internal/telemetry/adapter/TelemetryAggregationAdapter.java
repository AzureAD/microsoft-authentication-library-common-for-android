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
package com.microsoft.identity.common.internal.telemetry.adapter;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.rules.TelemetryAggregationRules;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public final class TelemetryAggregationAdapter implements ITelemetryAdapter<List<Map<String, String>>> {
    private ITelemetryAggregatedObserver mObserver;
    private final String START = "start";
    private final String END = "end";

    public TelemetryAggregationAdapter(@NonNull final ITelemetryAggregatedObserver observer) {
        mObserver = observer;
    }

    public ITelemetryAggregatedObserver getObserver() {
        return mObserver;
    }

    public void process(@NonNull final List<Map<String, String>> rawData) {
        final Map<String, String> aggregatedData = new HashMap<>();
        final Map<String, String> responseTimeMap = new HashMap<>();

        final Iterator<Map<String, String>> iterator = rawData.iterator();
        while (iterator.hasNext()) {
            Map<String, String> event = iterator.next();
            final String eventName = event.get(Key.EVENT_NAME);
            final String eventType = event.get(Key.EVENT_TYPE);

            if (StringUtil.isEmpty(eventName)) {
                aggregatedData.putAll(applyAggregationRule(event));
                continue;
            }

            //Count the events. Only check the "*_start_event" when counting.
            if (eventName.contains(START)) {
                final String eventTypeCount = eventType + "_count";
                aggregatedData.put(
                        eventTypeCount,
                        null == aggregatedData.get(eventTypeCount) ?
                                "1"
                                : String.valueOf(Integer.parseInt(aggregatedData.get(eventTypeCount)) + 1)
                );
            }

            if(!StringUtil.isEmpty(event.get(Key.IS_SUCCESSFUL))) {
                aggregatedData.put(
                        eventType + Key.IS_SUCCESSFUL,
                        event.get(Key.IS_SUCCESSFUL)
                );
            }

            trackEventResponseTime(responseTimeMap, event);

            aggregatedData.putAll(applyAggregationRule(event));
        }

        calculateEventResponseTime(responseTimeMap, aggregatedData);

        mObserver.onReceived(aggregatedData);
    }

    private Map<String, String> applyAggregationRule(@NonNull final Map<String, String> properties) {
        final Map<String, String> nonPiiProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!StringUtil.isEmpty(entry.getValue())
                    && !TelemetryAggregationRules.getInstance().isRedundant(entry.getKey())) {
                nonPiiProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return nonPiiProperties;
    }

    private void trackEventResponseTime(@NonNull final Map<String, String> responseTimeMap,
                                        @NonNull final Map<String, String> event) {
        final String eventName = event.get(Key.EVENT_NAME);
        final String eventType = event.get(Key.EVENT_TYPE);

        if (eventName.contains(START)) {
            final String eventStartTime = eventType + "_start_time";
            responseTimeMap.put(
                    eventStartTime,
                    null == event.get(Key.OCCUR_TIME) ?
                            null
                            : event.get(Key.OCCUR_TIME)
            );
        }

        if (eventName.contains(END)) {
            final String eventEndTime = eventType + "_end_time";
            responseTimeMap.put(
                    eventEndTime,
                    null == event.get(Key.OCCUR_TIME) ?
                            null
                            : event.get(Key.OCCUR_TIME)
            );
        }
    }

    private void calculateEventResponseTime(@NonNull final Map<String, String> responseTimeMap,
                                            @NonNull final Map<String, String> aggregatedData) {
        for (Map.Entry<String,String> entry : responseTimeMap.entrySet()) {
            if (entry.getKey().contains(START)
                    && responseTimeMap.containsKey(entry.getKey().replace(START, END))
                    && responseTimeMap.get(entry.getKey().replace(START, END)) != null) {
                final String key = entry.getKey().replace(START, "response");
                final long startTime = Long.parseLong(entry.getValue());
                final long endTime = Long.parseLong(responseTimeMap.get(entry.getKey().replace(START, END)));
                aggregatedData.put(key, String.valueOf(endTime - startTime));
            }
        }
    }
}
