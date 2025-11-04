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
package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext

/**
 * Interface for span factories that can be used by OTelUtility.
 */
interface IOTelSpanFactory {

    /**
     * Creates a span (with shared basic attributes).
     */
    fun createSpan(name: String): Span

    /**
     * Creates a span with caller package name (with shared basic attributes).
     */
    fun createSpan(name: String, callingPackageName: String): Span

    /**
     * Creates a span from a parent Span Context (with shared basic attributes).
     */
    fun createSpanFromParent(name: String, parentSpanContext: SpanContext?): Span

    /**
     * Creates a span from a parent Span Context with caller package name.
     */
    fun createSpanFromParent(
        name: String,
        parentSpanContext: SpanContext?,
        callingPackageName: String
    ): Span
}
