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
package com.microsoft.identity.common.java.opentelemetry;

import io.opentelemetry.api.NoopOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.NoopMeterProvider;
import io.opentelemetry.api.trace.Tracer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A custom safe Open Telemetry Instance holder that doesn't crash on the call to "get" Open
 * Telemetry. The motivation behind this is that all the Broker code has state that is per-process,
 * whereas other libraries such as Open Telemetry can carry state across processes. The default
 * Global Open Telemetry only allows initializing Open Telemetry once and subsequent calls to
 * initialize it will throw an error. This is problematic for code such as ours that runs in
 * multiple processes because we end up initializing OTel twice (once in app process and once in
 * auth process).
 * <p>
 * Open Telemetry itself recommends not using {@link io.opentelemetry.api.GlobalOpenTelemetry} and
 * with this change we are now moving away from that model.
 * TODO: We should make this even better by putting an instance of {@link OpenTelemetry} on the
 * {@link com.microsoft.identity.common.java.interfaces.IPlatformComponents} and completely move
 * away from static state, however, that requires us to then pass around either the components or
 * the Open Telemetry instances in all places where we want to create Spans and that change is
 * trivial in some areas of the code but more complex in other areas where we don't currently
 * pass platform components and therefore this will be handled in a separate PR.
 */
public class OpenTelemetryHolder {

    private static final OpenTelemetry NOOP = new NoopOpenTelemetry();

    @Accessors(prefix = "s")
    @Setter
    @Getter
    @NonNull
    private static OpenTelemetry sOpenTelemetry = NOOP;

    private static final MeterProvider NOOP_METER_PROVIDER = NoopMeterProvider.getInstance();

    /**
     * See {@link io.opentelemetry.api.GlobalOpenTelemetry#getTracer(String)}.
     */
    public static Tracer getTracer(final String instrumentationScopeName) {
        return sOpenTelemetry.getTracerProvider().get(instrumentationScopeName);
    }

    /**
     * See {@link io.opentelemetry.api.GlobalOpenTelemetry#getMeter(String)}.
     */
    public static Meter getMeter(String instrumentationScopeName) {
        try {
            return sOpenTelemetry.getMeterProvider().get(instrumentationScopeName);
        } catch (final AbstractMethodError error) {
            return NOOP_METER_PROVIDER.get(instrumentationScopeName);
        }
    }

    public static Logger getLogger(final String instrumentationScopeName) {
        return sOpenTelemetry.getLogsBridge().get(instrumentationScopeName);
    }

}
