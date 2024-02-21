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
package com.microsoft.identity.common.java.telemetry;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Deprecated
public class TelemetryConfiguration implements Serializable {

    private static final long serialVersionUID = 4048693049821792485L;

    /**
     * Field names used for serialization by Gson.
     */
    public static final class SerializedNames {
        public static final String PII_ENABLED = "pii_enabled";
        public static final String NOTIFY_ON_FAILTURE_ONLY = "notify_on_failure_only";
        public static final String DEBUG_ENABLED = "debug_enabled";
    }

    @SerializedName(SerializedNames.PII_ENABLED)
    private boolean mPiiEnabled = false;

    @SerializedName(SerializedNames.NOTIFY_ON_FAILTURE_ONLY)
    private boolean mNotifyOnFailureOnly = true;

    @SerializedName(SerializedNames.DEBUG_ENABLED)
    private boolean mDebugEnabled = false;

    public TelemetryConfiguration() {
    }

    /**
     * @return true to if the pii telemetry is enabled; false otherwise.
     */
    public boolean isPiiEnabled() {
        return mPiiEnabled;
    }

    /**
     * Setting piiEnabled to true, will allow sdk to return fields with user information in the telemetry events.
     * SDK does not send telemetry data by itself to any server.
     * If apps want to collect telemetry with user information
     * they must setup the telemetry callback and set this flag on.
     * <p>
     * By default sdk will not return any user information in telemetry.
     *
     * @param piiEnabled true to enable the pii telemetry; false otherwise.
     */
    public void setPiiEnabled(final boolean piiEnabled) {
        mPiiEnabled = piiEnabled;
    }

    /**
     * @return true if telemetry only dispatched when error occurred, false otherwise.
     */
    public boolean shouldNotifyOnFailureOnly() {
        return mNotifyOnFailureOnly;
    }

    /**
     * If set true, telemetry events are only dispatched when errors occurred;
     * If set false, sdk will dispatch all events.
     * <p>
     * By default sdk enables the notify on failure only
     *
     * @param notifyOnFailureOnly true to enable telemetry when error occurred, false otherwise.
     */
    public void setNotifyOnFailureOnly(final boolean notifyOnFailureOnly) {
        mNotifyOnFailureOnly = notifyOnFailureOnly;
    }

    /**
     * @return true if debugging for telemetry is enabled, false otherwise.
     */
    public boolean isDebugEnabled() {
        return mDebugEnabled;
    }

    /**
     * If set false, telemetry events are not dispatched when the app is in debugging.
     * If set true, sdk will dispatch all events.
     * <p>
     * By default telemetry is disabled in debugging mode.
     *
     * @param debugEnabled true to enable debugging for telemetry, false otherwise.
     */
    public void setDebugEnabled(final boolean debugEnabled) {
        mDebugEnabled = debugEnabled;
    }
}
