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

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

import javax.annotation.Nonnull;

@Deprecated
public class DeprecatedApiUsageEvent extends BaseEvent {

    public DeprecatedApiUsageEvent() {
        super();
        names(TelemetryEventStrings.Event.DEPRECATED_API_USAGE_EVENT)
                .types(TelemetryEventStrings.EventType.LIBRARY_CONSUMER_EVENT);
    }

    public DeprecatedApiUsageEvent putDeprecatedClassUsage(@Nonnull final Class<?> deprecatedClass) {
        put(TelemetryEventStrings.Key.PACKAGE_NAME, deprecatedClass.getPackage().toString());
        put(TelemetryEventStrings.Key.CLASS_NAME, deprecatedClass.getSimpleName());
        return this;
    }

    public DeprecatedApiUsageEvent putDeprecatedMethodUsage(@Nonnull final Class<?> methodClass, @Nonnull final String methodName) {
        put(TelemetryEventStrings.Key.CLASS_METHOD, methodName);
        return putDeprecatedClassUsage(methodClass);
    }
}
