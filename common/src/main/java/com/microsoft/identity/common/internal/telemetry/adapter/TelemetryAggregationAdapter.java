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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.rules.TelemetryAggregationRules;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_EVENT_API_END_EVENT;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_EVENT_API_START_EVENT;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_END_TIME;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_EVENT_NAME;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_EVENT_TYPE;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_OCCUR_TIME;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_START_TIME;

public final class TelemetryAggregationAdapter implements ITelemetryAdapter<List<Map<String, String>>> {
    private ITelemetryAggregatedObserver mObserver;

    public TelemetryAggregationAdapter(@NonNull final ITelemetryAggregatedObserver observer) {
        mObserver = observer;
    }

    public ITelemetryAggregatedObserver getObserver() {
        return mObserver;
    }

    public void process(List<Map<String, String>> rawData) {
        final Map<String, String> aggregatedData = new HashMap<>();
        long apiStartTime = -1;
        long apiEndTime = -1;

        final Iterator<Map<String, String>> iterator = rawData.iterator();
        while (iterator.hasNext()) {
            Map<String, String> event = iterator.next();
            final String eventName = event.get(TELEMETRY_KEY_EVENT_NAME);
            final String eventType = event.get(TELEMETRY_KEY_EVENT_TYPE);

            if (StringUtil.isEmpty(eventName)) {
                aggregatedData.putAll(applyAggregationRule(event));
                continue;
            }

            //only count the starting of event.
            if (eventName.contains("start")) {
                final String eventTypeCount = eventType + "_count";
                aggregatedData.put(
                        eventTypeCount,
                        null == aggregatedData.get(eventTypeCount) ?
                                "1"
                                : String.valueOf(Integer.parseInt(aggregatedData.get(eventTypeCount)) + 1)
                );
            }

            final long eventOccurTime = Long.parseLong(event.get(TELEMETRY_KEY_OCCUR_TIME));
            if (eventName.equalsIgnoreCase(TELEMETRY_EVENT_API_START_EVENT)
                    && (apiStartTime == -1
                    || eventOccurTime < apiStartTime)) {
                apiStartTime = eventOccurTime;
            }

            if (eventName.equalsIgnoreCase(TELEMETRY_EVENT_API_END_EVENT)
                    && (apiEndTime == -1
                    || eventOccurTime > apiEndTime)) {
                apiEndTime = eventOccurTime;
            }

            aggregatedData.putAll(applyAggregationRule(event));
        }

        aggregatedData.put(TELEMETRY_KEY_START_TIME, String.valueOf(apiStartTime));
        aggregatedData.put(TELEMETRY_KEY_END_TIME, String.valueOf(apiEndTime));

        mObserver.onReceived(aggregatedData);
    }

    private Map<String, String> applyAggregationRule(final Map<String, String> properties) {
        final Map<String, String> nonPiiProperties = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!StringUtil.isEmpty(entry.getValue())
                    && !TelemetryAggregationRules.getInstance().isRedundant(entry.getKey())) {
                nonPiiProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return nonPiiProperties;
    }
}
