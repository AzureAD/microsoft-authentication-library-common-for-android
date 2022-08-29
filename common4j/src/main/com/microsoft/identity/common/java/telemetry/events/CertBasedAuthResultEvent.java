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

import lombok.NonNull;

/**
 * Event specifically for emitting certificate-based authentication (CBA) result information.
 */
public class CertBasedAuthResultEvent extends BaseEvent {

    /**
     * Creates a new instance of CertBasedAuthResultEvent.
     * @param eventName Telemetry string designating the type of CBA being proceeded with (on-device or smartcard).
     */
    public CertBasedAuthResultEvent(final String eventName) {
        super();
        names(eventName);
        types(TelemetryEventStrings.EventType.CERT_BASED_AUTH_EVENT);
    }

    /**
     * Puts a ResultCode received from a RawAuthorizationResult as a result of a completed or failed CBA flow.
     * @param code ResultCode as a string.
     * @return The Event object.
     */
    public CertBasedAuthResultEvent putResponseCode(final String code) {
        put(TelemetryEventStrings.Key.RESULT_CODE, code);
        return this;
    }
}
