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
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class ApiEndEvent extends BaseEvent {
    private static final String TAG = ApiStartEvent.class.getSimpleName();

    public ApiEndEvent() {
        super();
        names(TELEMETRY_EVENT_API_EVENT_END);
    }

    public ApiEndEvent putResult(@NonNull final AcquireTokenResult result) {
        put(TELEMETRY_KEY_IS_SUCCESSFUL, result.getSucceeded().toString());

        if (null != result.getLocalAuthenticationResult()) {
            put(TELEMETRY_KEY_USER_ID, result.getLocalAuthenticationResult().getUniqueId());
            put(TELEMETRY_KEY_TENANT_ID, result.getLocalAuthenticationResult().getTenantId());
            put(TELEMETRY_KEY_SPE_RING, result.getLocalAuthenticationResult().getSpeRing());
            put(TELEMETRY_KEY_RT_AGE, result.getLocalAuthenticationResult().getRefreshTokenAge());
        }

        return this;
    }

    public ApiEndEvent putException(@NonNull final BaseException exception) {
        if (exception  instanceof UserCancelException) {
            put(TELEMETRY_VALUE_USER_CANCELLED, TELEMETRY_VALUE_YES);
        }

        put(TELEMETRY_KEY_SERVER_ERROR_CODE, exception.getCliTelemErrorCode());
        put(TELEMETRY_KEY_SERVER_SUBERROR_CODE, exception.getCliTelemSubErrorCode());
        put(TELEMETRY_KEY_API_ERROR_CODE, exception.getErrorCode());
        put(TELEMETRY_KEY_SPE_RING, exception.getSpeRing());
        put(TELEMETRY_KEY_ERROR_DESCRIPTION, exception.getMessage()); //OII
        put(TELEMETRY_KEY_RT_AGE, exception.getRefreshTokenAge());
        put(TELEMETRY_KEY_IS_SUCCESSFUL, TELEMETRY_VALUE_NO);
        return this;
    }

    public ApiEndEvent putApiId(@NonNull final String apiId) {
        super.put(TELEMETRY_KEY_API_ID, apiId);
        return this;
    }
}
