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

import java.util.HashMap;
import java.util.Map;

public class EventValueMap {
    private Map<String, Properties> mEventValueMap;

    public EventValueMap() {
        mEventValueMap = new HashMap<>();
    }

    public EventValueMap(final String event, final Properties properties) {
        mEventValueMap = new HashMap<>();
        mEventValueMap.put(event, properties);
    }

    public void put(final String event, final Properties properties) {
        if (mEventValueMap != null) {
            //TODO initialize the event with start time
            mEventValueMap.put(
                    event,
                    mEventValueMap.get(event) == null ?
                            properties :
                            mEventValueMap.get(event).put(properties)
            );
        } else {
            mEventValueMap = new HashMap<>();
            mEventValueMap.put(event, properties);
        }
    }

    public Map<String, Properties> getEventValueMap() {
        return mEventValueMap;
    }

    public void put(final EventValueMap eventValueMap) {
        if (mEventValueMap == null) {
            mEventValueMap = new HashMap<>();
        }

        mEventValueMap.putAll(eventValueMap.getEventValueMap());
    }
}
