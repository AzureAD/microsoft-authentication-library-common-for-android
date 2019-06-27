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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.UserCancelException;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class BrokerEndEvent extends BaseEvent {
    public BrokerEndEvent() {
        super();
        names(TELEMETRY_EVENT_BROKER_END_EVENT);
        types(TELEMETRY_EVENT_BROKER_EVENT);
    }

    public BrokerEndEvent putAction(final String actionName) {
        put(TELEMETRY_KEY_BROKER_ACTION, actionName);
        return this;
    }

    public BrokerEndEvent isSuccessful(final boolean isSuccessful) {
        put(TELEMETRY_KEY_IS_SUCCESSFUL, String.valueOf(isSuccessful));
        return this;
    }

    public BrokerEndEvent putException(@NonNull final BaseException exception) {
        if (exception  instanceof UserCancelException) {
            put(TELEMETRY_VALUE_USER_CANCELLED, TELEMETRY_VALUE_TRUE);
        }

        put(TELEMETRY_KEY_SERVER_ERROR_CODE, exception.getCliTelemErrorCode());
        put(TELEMETRY_KEY_SERVER_SUBERROR_CODE, exception.getCliTelemSubErrorCode());
        put(TELEMETRY_KEY_API_ERROR_CODE, exception.getErrorCode());
        put(TELEMETRY_KEY_SPE_RING, exception.getSpeRing());
        put(TELEMETRY_KEY_ERROR_DESCRIPTION, exception.getMessage()); //OII
        put(TELEMETRY_KEY_RT_AGE, exception.getRefreshTokenAge());
        put(TELEMETRY_KEY_IS_SUCCESSFUL, TELEMETRY_VALUE_FALSE);
        return this;
    }

    public BrokerEndEvent putErrorCode(@NonNull final String errorCode) {
        put(TELEMETRY_KEY_API_ERROR_CODE, errorCode);
        return this;
    }

    public BrokerEndEvent putErrorDescription(@NonNull final String errorDescription) {
        put(TELEMETRY_KEY_ERROR_DESCRIPTION, errorDescription);
        return this;
    }
}
