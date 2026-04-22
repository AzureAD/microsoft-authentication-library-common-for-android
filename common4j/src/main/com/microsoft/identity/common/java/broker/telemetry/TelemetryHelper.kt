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
package com.microsoft.identity.common.java.broker.telemetry

/**
 * Null-safe utility for recording events into an [EventCollector].
 * Callers may hold a nullable collector reference and use this helper to avoid null checks.
 */
object TelemetryHelper {
    /**
     * Adds an event to the given [collector] if it is non-null; otherwise does nothing.
     *
     * @param collector The [EventCollector] to record the event into, or null.
     * @param tag The [EventTag] identifying the phase.
     * @param diagnosticCode Optional diagnostic code for additional context.
     * @param errorCode Optional error code if this event represents a failure.
     */
    @JvmStatic
    fun addEventSafely(
        collector: EventCollector?,
        tag: EventTag,
        diagnosticCode: Int? = null,
        errorCode: Int? = null
    ) {
        collector?.addEvent(tag, diagnosticCode, errorCode)
    }
}
