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

import com.microsoft.identity.common.java.logging.Logger;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Utility class for working with OpenTelemetry Baggage objects.
 */
public class BaggageExtension {

    private static final String TAG = BaggageExtension.class.getSimpleName();

    /**
     * Default constructor.
     */
    private BaggageExtension() {
        // Utility class, private constructor to prevent instantiation
    }

    /**
     * Makes the provided Baggage current in the context, catching any exceptions silently.
     * This is useful in scenarios where Baggage propagation should not interrupt normal operation flow.
     *
     * @param baggage The Baggage to make current.
     * @return the resulting scope.
     */
    public static Scope makeBaggageCurrent(final Baggage baggage) {
        try {
            if (baggage == null) {
                return SpanExtension.NoopScope.INSTANCE;
            }
            return baggage.storeInContext(Context.current()).makeCurrent();
        } catch (final Throwable e) {
            Logger.error(TAG + ":makeBaggageCurrent", "Failed to make baggage current", e);
            return SpanExtension.NoopScope.INSTANCE;
        }
    }

    /**
     * Returns the current Baggage from the context, or a NoopBaggage if an error occurs.
     *
     * @return the current Baggage.
     */
    public static Baggage fromContext(final Context context) {
        try {
            return Baggage.fromContext(context);
        } catch (final Throwable e) {
            Logger.error(TAG + ":fromContext", "Failed to get baggage from context", e);
            return new NoopBaggage();
        }
    }
}
