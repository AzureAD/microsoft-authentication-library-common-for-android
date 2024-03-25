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
package com.microsoft.identity.common.internal.telemetry.events;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.UserCancelException;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Value;

@Deprecated
public class BrokerEndEvent extends com.microsoft.identity.common.java.telemetry.events.BaseEvent {
    public BrokerEndEvent() {
        super();
        names(Event.BROKER_END_EVENT);
        types(EventType.BROKER_EVENT);
    }

    public BrokerEndEvent putAction(final String actionName) {
        put(Key.BROKER_ACTION, actionName);
        return this;
    }


    public BrokerEndEvent isSuccessful(final boolean isSuccessful) {
        put(Key.IS_SUCCESSFUL, String.valueOf(isSuccessful));
        return this;
    }

    public BrokerEndEvent putException(@NonNull final BaseException exception) {
        if (exception == null) {
            return this;
        }

        if (exception instanceof UserCancelException) {
            put(Key.USER_CANCEL, Value.TRUE);
        }

        put(Key.SERVER_ERROR_CODE, exception.getCliTelemErrorCode());
        put(Key.SERVER_SUBERROR_CODE, exception.getCliTelemSubErrorCode());
        put(Key.ERROR_CODE, exception.getErrorCode());
        put(Key.SPE_RING, exception.getSpeRing());
        put(Key.ERROR_DESCRIPTION, exception.getMessage()); //OII
        put(Key.RT_AGE, exception.getRefreshTokenAge());
        put(Key.IS_SUCCESSFUL, Value.FALSE);
        return this;
    }

    public BrokerEndEvent putErrorCode(@NonNull final String errorCode) {
        put(Key.ERROR_CODE, errorCode);
        return this;
    }

    public BrokerEndEvent putErrorDescription(@NonNull final String errorDescription) {
        put(Key.ERROR_DESCRIPTION, errorDescription);
        return this;
    }
}
