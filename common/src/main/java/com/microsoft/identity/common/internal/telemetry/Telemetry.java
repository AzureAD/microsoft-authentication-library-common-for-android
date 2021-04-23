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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.microsoft.identity.common.BuildConfig;
import com.microsoft.identity.common.logging.Logger;

/**
 * Deprecated. Use {@link com.microsoft.identity.common.java.telemetry.Telemetry} instead.
 **/
@Deprecated
public class Telemetry extends com.microsoft.identity.common.java.telemetry.Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();

    /**
     * This is for getting instance of Telemetry
     * Deprecated. Use {@link com.microsoft.identity.common.java.telemetry.Telemetry} instead.
     **/
    @Deprecated
    public synchronized static Telemetry getInstance() {
        return (Telemetry) com.microsoft.identity.common.java.telemetry.Telemetry.getInstance();
    }

    /**
     * API for creating {@link Telemetry} instances.
     */
    public static class Builder {
        private com.microsoft.identity.common.java.telemetry.TelemetryConfiguration mDefaultConfiguration;
        private AndroidTelemetryContext mTelemetryContext;
        private Boolean mIsDebugging;

        public Builder() {
        }

        public Builder withContext(final Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }

            final Context mContext = context.getApplicationContext();
            if (mContext == null) {
                throw new IllegalArgumentException("Application context must not be null.");
            }

            mTelemetryContext = new AndroidTelemetryContext(mContext);

            try {
                final String packageName = context.getPackageName();
                int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
                mIsDebugging = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            } catch (final PackageManager.NameNotFoundException exception) {
                Logger.warn(TAG, "The application is not found from PackageManager.");
                mIsDebugging = BuildConfig.DEBUG;
            }

            return this;
        }

        /**
         * Specify the default options for all calls.
         */
        public Builder defaultConfiguration(final TelemetryConfiguration configuration) {
            mDefaultConfiguration = configuration;
            return this;
        }

        /**
         * Create a {@link Telemetry} client.
         */
        public Telemetry build() throws IllegalArgumentException {
            return (Telemetry) new com.microsoft.identity.common.java.telemetry.Telemetry.Builder()
                    .defaultConfiguration(mDefaultConfiguration)
                    .isDebugging(mIsDebugging)
                    .withTelemetryContext(mTelemetryContext)
                    .build();
        }
    }
}