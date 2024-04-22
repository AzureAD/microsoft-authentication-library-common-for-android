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
package com.microsoft.identity.common.java.telemetry.events;

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Value;

@Deprecated
public class CacheEndEvent extends com.microsoft.identity.common.java.telemetry.events.BaseEvent {
    public CacheEndEvent() {
        super();
        names(Event.CACHE_END_EVENT);
        types(EventType.CACHE_EVENT);
    }

    public CacheEndEvent putRtStatus(final String rtStatus) {
        put(Key.RT_STATUS, rtStatus);
        return this;
    }

    public CacheEndEvent putAtStatus(final String rtStatus) {
        put(Key.AT_STATUS, rtStatus);
        return this;
    }

    public CacheEndEvent putFrtStatus(final String frtStatus) {
        put(Key.FRT_STATUS, frtStatus);
        return this;
    }

    public CacheEndEvent putCacheRecordStatus(final CacheRecord cacheRecord) {
        if (cacheRecord == null) {
            return this;
        }

        put(Key.AT_STATUS, cacheRecord.getAccessToken() == null ? Value.FALSE : Value.TRUE);
        if (null != cacheRecord.getRefreshToken()) {
            put(Key.MRRT_STATUS, Value.TRUE); //MSAL RT is MRRT and ADFS is not supported by now.
            put(Key.RT_STATUS, Value.TRUE);
            put(Key.FRT_STATUS, StringUtil.isNullOrEmpty(cacheRecord.getRefreshToken().getFamilyId()) ? TelemetryEventStrings.Value.FALSE : TelemetryEventStrings.Value.TRUE);
        } else {
            put(Key.RT_STATUS, Value.FALSE);
        }
        put(Key.ID_TOKEN_STATUS, cacheRecord.getIdToken() == null ? TelemetryEventStrings.Value.FALSE : TelemetryEventStrings.Value.TRUE);
        put(Key.V1_ID_TOKEN_STATUS, cacheRecord.getV1IdToken() == null ? TelemetryEventStrings.Value.FALSE : TelemetryEventStrings.Value.TRUE);
        put(Key.ACCOUNT_STATUS, cacheRecord.getAccount() == null ? TelemetryEventStrings.Value.FALSE : TelemetryEventStrings.Value.TRUE);
        EstsTelemetry.getInstance().emit(this.getProperties());
        return this;
    }

    public CacheEndEvent putSpeInfo(final String speInfo) {
        put(Key.SPE_INFO, speInfo);
        return this;
    }
}
