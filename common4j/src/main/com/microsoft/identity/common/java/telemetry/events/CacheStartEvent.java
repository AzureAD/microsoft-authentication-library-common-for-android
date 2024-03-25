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

@Deprecated
public class CacheStartEvent extends com.microsoft.identity.common.java.telemetry.events.BaseEvent {
    public CacheStartEvent() {
        super();
        names(Event.CACHE_START_EVENT);
        types(EventType.CACHE_EVENT);
    }

    public CacheStartEvent putTokenType(final String tokenType) {
        put(Key.TOKEN_TYPE, tokenType);
        return this;
    }

    public CacheStartEvent isFrt(final boolean isFrt) {
        put(Key.IS_FRT, String.valueOf(isFrt));
        return this;
    }

    public CacheStartEvent isMrrt(final boolean isMrrt) {
        put(Key.IS_MRRT, String.valueOf(isMrrt));
        return this;
    }

    public CacheStartEvent isRt(final boolean isRt) {
        put(Key.IS_RT, String.valueOf(isRt));
        return this;
    }

    public CacheStartEvent isAt(final boolean isAt) {
        put(Key.IS_AT, String.valueOf(isAt));
        return this;
    }

    public CacheStartEvent putWipeApp(final boolean appWiped) {
        put(Key.WIPE_APP, String.valueOf(appWiped));
        return this;
    }
}
