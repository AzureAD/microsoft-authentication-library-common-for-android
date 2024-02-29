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
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryObserver;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * Deprecated.
 *
 * This is now acting as an adapter for {@link com.microsoft.identity.common.java.telemetry.Telemetry}.
 **/
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
@Deprecated
public class Telemetry extends com.microsoft.identity.common.java.telemetry.Telemetry {
    private final static String TAG = Telemetry.class.getSimpleName();

    private static final Telemetry instance = new Telemetry();
    private static final com.microsoft.identity.common.java.telemetry.Telemetry actualInstance =
            com.microsoft.identity.common.java.telemetry.Telemetry.getInstance();

    public synchronized static Telemetry getInstance() {
        return instance;
    }

    @Override
    public void addObserver(@SuppressWarnings(WarningType.rawtype_warning) final ITelemetryObserver observer) {
        actualInstance.addObserver(observer);
    }

    @Override
    public void removeAllObservers() {
        actualInstance.removeAllObservers();
    }

    @Override
    public void removeObserver(Class<?> cls) {
        actualInstance.removeObserver(cls);
    }

    @Override
    public void removeObserver(@SuppressWarnings(WarningType.rawtype_warning) final ITelemetryObserver observer)  {
        actualInstance.removeObserver(observer);
    }

    @Override
    @SuppressWarnings({WarningType.rawtype_warning, WarningType.unchecked_warning})
    public List<ITelemetryObserver> getObservers() {
        return actualInstance.getObservers();
    }

    @Override
    public void flush() {
        actualInstance.flush();
    }

    @Override
    public void flush(@NonNull String correlationId) {
        actualInstance.flush(correlationId);
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
            final String methodTag = TAG + ":withContext";
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
                Logger.warn(methodTag, "The application is not found from PackageManager.");
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
            new com.microsoft.identity.common.java.telemetry.Telemetry.Builder()
                    .defaultConfiguration(mDefaultConfiguration)
                    .isDebugging(mIsDebugging)
                    .withTelemetryContext(mTelemetryContext)
                    .build();

            // Returns a shell object.
            return instance;
        }
    }
}
