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
package com.microsoft.identity.common.internal.telemetry.rules;

import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_EVENT_NAME;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.TELEMETRY_KEY_OCCUR_TIME;

public class TelemetryAggregationRules {
    private static TelemetryAggregationRules sInstance;
    private Set<String> aggregatedPropertiesSet;

    final private String aggregatedArray[] = {
            TELEMETRY_KEY_EVENT_NAME,
            TELEMETRY_KEY_OCCUR_TIME
    };

    private TelemetryAggregationRules() {
        aggregatedPropertiesSet = new HashSet<>(Arrays.asList(aggregatedArray));
    }

    public static TelemetryAggregationRules getInstance() {
        if (sInstance == null) {
            synchronized (TelemetryAggregationRules.class) {
                sInstance = new TelemetryAggregationRules();
            }
        }

        return sInstance;
    }

    public boolean isRedundant(final String propertyName) {
        if (StringUtil.isEmpty(propertyName)) {
            return false;
        }

        return aggregatedPropertiesSet.contains(propertyName);
    }
}