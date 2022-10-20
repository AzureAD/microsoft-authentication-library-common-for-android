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
package com.microsoft.identity.common.opentelemetry;

import android.content.Context;

import com.microsoft.applications.telemetry.EventProperties;
import com.microsoft.applications.telemetry.ILogger;
import com.microsoft.applications.telemetry.LogConfiguration;
import com.microsoft.applications.telemetry.LogManager;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.Collection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.NonNull;

/**
 * A {@link SpanExporter} that exports Spans to Aria using {@link ILogger}.
 */
public class AriaSpanExporter implements SpanExporter {

    private static final String TAG = AriaSpanExporter.class.getSimpleName();

    private final ILogger mLogger;
    private final ISpanDataAdapter<EventProperties> mSpanDataAdapter = new AriaSpanDataAdapter();

    public AriaSpanExporter(@NonNull final Context context,
                            @NonNull final String ariaToken) {
        this(context, ariaToken, new LogConfiguration());
    }

    public AriaSpanExporter(@NonNull final Context context,
                            @NonNull final String ariaToken,
                            @NonNull final LogConfiguration logConfiguration) {
        this.mLogger = LogManager.initialize(context, ariaToken, logConfiguration);
    }


    @Override
    public CompletableResultCode export(@NonNull final Collection<SpanData> spans) {
        final String methodTag = TAG + ":export";
        try {
            for (final SpanData spanData : spans) {
                final EventProperties eventProperties = mSpanDataAdapter.adapt(spanData);
                mLogger.logEvent(eventProperties);
            }
            return flush();
        } catch (final Throwable throwable) {
            Logger.error(methodTag, throwable.getMessage(), throwable);
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode flush() {
        final String methodTag = TAG + ":flush";
        try {
            LogManager.flush();
            return CompletableResultCode.ofSuccess();
        } catch (final Throwable throwable) {
            Logger.error(methodTag, throwable.getMessage(), throwable);
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        final String methodTag = TAG + ":shutdown";
        try {
            LogManager.flushAndTeardown();
            return CompletableResultCode.ofSuccess();
        } catch (final Throwable throwable) {
            Logger.error(methodTag, throwable.getMessage(), throwable);
            return CompletableResultCode.ofFailure();
        }
    }
}
