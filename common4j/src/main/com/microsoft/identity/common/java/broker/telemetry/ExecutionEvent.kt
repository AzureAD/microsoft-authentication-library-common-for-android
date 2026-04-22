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

import com.google.gson.annotations.SerializedName

/**
 * Represents a single timed event within the broker authentication flow.
 *
 * @param tag The [EventTag] identifying which phase this event captures.
 * @param timestampMs Elapsed time in milliseconds from the start of the flow.
 * @param threadId The ID of the thread that recorded this event.
 * @param diagnosticCode Optional diagnostic code for additional context.
 * @param errorCode Optional error code if the event represents a failure.
 */
data class ExecutionEvent(
    @SerializedName("t") val tag: EventTag,
    @SerializedName("ts") val timestampMs: Long,
    @SerializedName("tid") val threadId: Long = Thread.currentThread().id,
    @SerializedName("d") val diagnosticCode: Int? = null,
    @SerializedName("e") val errorCode: Int? = null
)
