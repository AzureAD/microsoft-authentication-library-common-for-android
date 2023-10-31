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
package com.microsoft.identity.common.internal.providers.oauth2;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import lombok.NonNull;

/**
 * Converts SerializableSpanContext to and from JSON.
 */
public class SerializableSpanContextJsonAdapter {
    private static final JsonAdapter<SerializableSpanContext> sAdapterInstance = new Moshi.Builder()
            .build()
            .adapter(SerializableSpanContext.class);

    /**
     * Converts SerializableSpanContext instance to JSON string.
     * @param spanContext SerializableSpanContext instance.
     * @return JSON string.
     */
    public static String toJson(@NonNull final SerializableSpanContext spanContext) {
        return sAdapterInstance.toJson(spanContext);
    }

    /**
     * Converts JSON representation to SerializableSpanContext.
     * In the case of an exception thrown, this method will return null.
     * @param spanContextString JSON representation of SerializableSpanContext.
     * @return SerialzableSpanContext.
     */
    @Nullable
    public static SerializableSpanContext fromJson(@NonNull final String spanContextString) {
        try {
            return sAdapterInstance.fromJson(spanContextString);
        } catch (IOException e) {
            return null;
        }
    }
}
