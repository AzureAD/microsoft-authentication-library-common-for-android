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
package com.microsoft.identity.common.java.telemetry.relay;

import com.microsoft.identity.common.java.exception.BaseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * An exception class for the telemetry pipeline.
 */
@EqualsAndHashCode(callSuper = true)
@Data()
@Accessors(prefix = "m")
@Deprecated
public class TelemetryRelayException extends BaseException  {
    private static final long serialVersionUID = -1543623857511895210L;

    public static final String INITIALIZATION_FAILED = "initialization_failed";
    public static final String NOT_INITIALIZED = "not_initialized";

    public TelemetryRelayException(final @Nullable String message, final Throwable cause, final @Nonnull String errorCode) {
        super(errorCode, message, cause);
    }

    public TelemetryRelayException(final @Nonnull Throwable cause, final @Nonnull String errorCode) {
        this(null, cause, errorCode);
    }

    public TelemetryRelayException(final @Nonnull String message, final @Nonnull String errorCode) {
        this(message, null, errorCode);
    }
}
