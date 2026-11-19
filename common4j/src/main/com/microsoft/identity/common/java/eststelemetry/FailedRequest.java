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
package com.microsoft.identity.common.java.eststelemetry;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

class FailedRequest {

    private static final char DELIMITER_REPLACEMENT = '_';

    @Nullable
    private final String mApiId;

    @Nullable
    private final String mCorrelationId;

    @Nullable
    private final String mError;

    public FailedRequest(@Nullable final String mApiId,
                         @Nullable final String mCorrelationId,
                         @Nullable final String error) {
        this.mApiId = mApiId;
        this.mCorrelationId = mCorrelationId;
        this.mError = error;
    }

    @NonNull
    public String toApiIdCorrelationString() {
        return sanitizeSegmentValue(mApiId)
                + SchemaConstants.SEPARATOR_COMMA
                + sanitizeSegmentValue(mCorrelationId);
    }

    @NonNull
    public String toErrorCodeString() {
        return sanitizeSegmentValue(mError);
    }

    @NonNull
    private static String sanitizeSegmentValue(@Nullable final String value) {
        if (value == null || value.isEmpty()) {
            return SchemaConstants.Value.EMPTY;
        }
        if (value.indexOf('|') < 0 && value.indexOf(',') < 0) {
            return value;
        }
        final char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '|' || chars[i] == ',') {
                chars[i] = DELIMITER_REPLACEMENT;
            }
        }
        return new String(chars);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FailedRequest that = (FailedRequest) o;
        return Objects.equals(mCorrelationId, that.mCorrelationId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mCorrelationId);
    }
}
