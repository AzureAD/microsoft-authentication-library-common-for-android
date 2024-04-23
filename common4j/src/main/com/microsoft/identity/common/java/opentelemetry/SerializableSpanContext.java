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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A JSON Serializable implementation of {@link SpanContext}.
 */
@Builder
@Accessors(prefix = "m")
public class SerializableSpanContext implements SpanContext, Serializable {

    //Used to pass span context to another activity.
    public static final String SERIALIZABLE_SPAN_CONTEXT = "serializable_span_context";

    public static class SerializedNames {
        public static final String TRACE_ID = "trace_id";
        public static final String SPAN_ID = "span_id";
        public static final String TRACE_FLAGS = "trace_flags";
        public static final String PARENT_SPAN_NAME = "parent_span_name";
    }

    @SerializedName(SerializedNames.TRACE_ID)
    @NonNull
    private final String mTraceId;

    @SerializedName(SerializedNames.SPAN_ID)
    @NonNull
    private final String mSpanId;

    @SerializedName(SerializedNames.TRACE_FLAGS)
    private final byte mTraceFlags;

    @Override
    public String getTraceId() {
        return mTraceId;
    }

    @Override
    public String getSpanId() {
        return mSpanId;
    }

    @Override
    public TraceFlags getTraceFlags() {
        return TraceFlags.fromByte(mTraceFlags);
    }

    @Override
    public TraceState getTraceState() {
        return TraceState.getDefault();
    }

    @Override
    public boolean isRemote() {
        return false;
    }
}
