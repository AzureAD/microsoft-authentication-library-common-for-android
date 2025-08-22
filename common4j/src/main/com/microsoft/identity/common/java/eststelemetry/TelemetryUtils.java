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

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.util.StringUtil;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class TelemetryUtils {

    private static final String TAG = TelemetryUtils.class.getSimpleName();

    static boolean getBooleanFromString(@Nullable final String val) {
        return val != null && val.equals(SchemaConstants.Value.TRUE);
    }

    static String getSchemaCompliantStringFromBoolean(final boolean val) {
        return val ? SchemaConstants.Value.TRUE : SchemaConstants.Value.FALSE;
    }

    @NonNull
    static String getSchemaCompliantString(final String s) {
        if (StringUtil.isNullOrEmpty(s)) {
            return SchemaConstants.Value.EMPTY;
        } else if (s.equals(TelemetryEventStrings.Value.TRUE)) {
            return SchemaConstants.Value.TRUE;
        } else if (s.equals(TelemetryEventStrings.Value.FALSE)) {
            return SchemaConstants.Value.FALSE;
        } else {
            return s;
        }
    }

    /**
     * Functional interface for executing ests telemetry-related code.
     */
    public interface EstsAction {
        void execute();
    }

    /**
     * Checks if ESTS telemetry related code should be executed based on feature flag, and executes
     * the provided action.
     *
     * @param action The action to execute if ESTS telemetry should be emitted
     */
    public static void executeIfEstsTelemetryEnabled(@NonNull final EstsAction action) {
        final String methodTag = TAG + ":executeIfEstsTelemetryEnabled";
        final boolean shouldSkipEstsTelemetry = CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.SKIP_ESTS_TELEMETRY);

        if (!shouldSkipEstsTelemetry) {
            Logger.info(methodTag, "Executing ESTS telemetry action");
            action.execute();
            SpanExtension.current().setAttribute(AttributeName.skipped_ests_telemetry.name(), false);
        } else {
            Logger.info(methodTag, "Skipping ESTS telemetry action due to feature flag");
            SpanExtension.current().setAttribute(AttributeName.skipped_ests_telemetry.name(), true);
        }
    }
}
