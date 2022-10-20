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

import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.NonNull;

/**
 * Describes an adapter for the {@link SpanData} produced by Open Telemetry. Implementers of this
 * interface will adapt the data in the format suitable for their telemetry storage service.
 * <p>
 * For instance, one may choose to implement separate adapters for services like Aria and
 * Application Insights because each of them will need a different format of data to be uploaded to
 * those services through their {@link io.opentelemetry.sdk.trace.export.SpanExporter}.
 *
 * @param <T> the type into which to adapt the span data
 */
public interface ISpanDataAdapter<T> {

    /**
     * Adapts the supplied {@link SpanData} into the specified type.
     *
     * @param spanData the {@link SpanData} to adapt
     * @return the adapted span data
     */
    T adapt(@NonNull final SpanData spanData);
}
