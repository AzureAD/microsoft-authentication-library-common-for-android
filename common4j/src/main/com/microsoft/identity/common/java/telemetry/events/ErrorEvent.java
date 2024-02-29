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
package com.microsoft.identity.common.java.telemetry.events;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

/**
 * An error event captures an Exception and extracts details from it.
 */
@Deprecated
public class ErrorEvent extends BaseEvent {

    public static final String ERROR_TAG_PREFIX = "tag_";

    public ErrorEvent() {
        super();
        types(TelemetryEventStrings.EventType.ERROR_EVENT);
    }

    /**
     * Generate a tag for this exception based on four parameters.
     * 1. exception class name
     * 2. exception message,
     * 3. class name for where the exception was thrown
     * 4. method name for where the exception was thrown
     *
     * @param exception         the exception
     * @param stackTraceElement the first stack trace element
     * @return a string that uniquely identifies the exception based on where it was thrown.
     * We don't factor in the line number since that may change over time.
     */
    private String generateErrorTag(final Exception exception, final StackTraceElement stackTraceElement) {
        String tag = exception.getClass().getSimpleName() + exception.getMessage() + stackTraceElement.getClassName() + stackTraceElement.getMethodName();

        if (exception instanceof BaseException) {
            tag += ((BaseException) exception).getErrorCode();
        }

        // we add Integer.MAX_VALUE to ensure we don't get negatives. This is to ensure consistency and avoid confusion in the tags generated.
        // For example, if we allow negatives we would generate tags like tag_-123456 and tag_123456, which may seem as similar.
        return ERROR_TAG_PREFIX + (((long) tag.hashCode()) + Integer.MAX_VALUE);
    }

    public ErrorEvent putException(final Exception exception) {
        if (exception == null) {
            return this;
        }

        final StackTraceElement[] stackTraceElements = exception.getStackTrace();

        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement errorLocation = stackTraceElements[0];
            // generate a tag from the trace element. We will use this to know whether an exception is thrown from the same code point.
            final String errorTag = generateErrorTag(exception, errorLocation);

            put(TelemetryEventStrings.Key.ERROR_TAG, errorTag);
            put(TelemetryEventStrings.Key.ERROR_LOCATION_CLASS_NAME, errorLocation.getClassName());
            put(TelemetryEventStrings.Key.ERROR_LOCATION_LINE_NUMBER, String.valueOf(errorLocation.getLineNumber()));
            put(TelemetryEventStrings.Key.ERROR_LOCATION_METHOD_NAME, errorLocation.getMethodName());
        }

        put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, exception.getClass().getSimpleName());
        put(TelemetryEventStrings.Key.ERROR_DESCRIPTION, exception.getMessage()); // pii

        if (exception instanceof BaseException) {
            final BaseException adaptedException = (BaseException) exception;

            if (adaptedException.getCause() != null) {
                put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, adaptedException.getCause().getClass().getSimpleName());
            }

            put(TelemetryEventStrings.Key.ERROR_CODE, adaptedException.getErrorCode());
            put(TelemetryEventStrings.Key.SERVER_ERROR_CODE, adaptedException.getCliTelemErrorCode());
            put(TelemetryEventStrings.Key.SERVER_SUBERROR_CODE, adaptedException.getCliTelemSubErrorCode());
        }

        return this;
    }
}
