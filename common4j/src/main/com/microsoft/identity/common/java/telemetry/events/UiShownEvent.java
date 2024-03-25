package com.microsoft.identity.common.java.telemetry.events;
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

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;

/** This event is used in telemetry to check if UI was visible to the end user.
 *  The value is set in case UI is visible, even if it is for a brief second.
*/
@Deprecated
public class UiShownEvent extends com.microsoft.identity.common.java.telemetry.events.BaseEvent {
    public UiShownEvent() {
        super();
        names(Event.UI_SHOWN_EVENT);
        types(EventType.UI_EVENT);
    }

    public UiShownEvent putVisible(String value) {
        put(Key.UI_VISIBLE, value);
        return this;
    }
}
