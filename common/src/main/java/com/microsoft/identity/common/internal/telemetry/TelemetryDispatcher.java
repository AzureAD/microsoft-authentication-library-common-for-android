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

import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelemetryDispatcher {
//    private final List<ITelemetryObserver> mEventReceiver;
//
//    /**
//     * Constructs a new EventDispatcher.
//     *
//     * @param receiver the {@link ITelemetryObserver} to receive {@link Telemetry} data.
//     */
//    TelemetryDispatcher(final ITelemetryObserver receiver) {
//        mEventReceiver = receiver;
//    }
//
//    /**
//     * Returns the {@link ITelemetryObserver} to which telemetry data is dispatched.
//     *
//     * @return the event receiver.
//     */
//    ITelemetryObserver getReceiver() {
//        return mEventReceiver;
//    }
//
//    /**
//     * Dispatches the {@link Telemetry} instances associated to receiver.
//     *
//     * @param eventsToPublish the Events to publish.
//     */
//    void dispatch(final List<Properties> eventsToPublish) {
//        if (null == mEventReceiver) {
//            return;
//        }
//
//        final Map<String, String> eventsForPublication = new HashMap<>();
//
//        for (final Properties event : eventsToPublish) {
//            eventsForPublication.putAll(event.getProperties());
//        }
//
//        mEventReceiver.upload(eventsForPublication);
//    }
}
