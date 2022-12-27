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

package com.microsoft.identity.common.java.eststelemetry;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.java.eststelemetry.SchemaConstants.Key.API_ID;
import static com.microsoft.identity.common.java.eststelemetry.SchemaConstants.Key.FORCE_REFRESH;

class CurrentRequestTelemetry extends RequestTelemetry implements ICurrentTelemetry {

    @Getter
    @Accessors(prefix = "m")
    private String mApiId;

    @Getter
    @Accessors(prefix = "m")
    private boolean mForceRefresh;

    CurrentRequestTelemetry() {
        super(SchemaConstants.CURRENT_SCHEMA_VERSION);
    }

    @Override
    public String getHeaderStringForFields() {
        return TelemetryUtils.getSchemaCompliantString(mApiId) + "," +
                TelemetryUtils.getSchemaCompliantStringFromBoolean(mForceRefresh);

    }

    @Override
    public void put(@NonNull final String key, @NonNull final String value) {
        switch (key) {
            case API_ID:
                mApiId = value;
                break;
            case FORCE_REFRESH:
                mForceRefresh = TelemetryUtils.getBooleanFromString(value);
                break;
            default:
                putInPlatformTelemetry(key, value);
                break;
        }
    }
}
