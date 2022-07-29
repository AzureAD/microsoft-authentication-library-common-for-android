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
 * Event specifically for emitting certificate-based authentication (CBA) info.
 */
public class CertBasedAuthEvent extends BaseEvent {

    /**
     * Creates a new instance of CertBasedAuthEvent.
     * @param eventName Telemetry string designating the type of CBA being proceeded with (on-device or smartcard).
     */
    public CertBasedAuthEvent(final String eventName) {
        super();
        names(eventName);
        types(TelemetryEventStrings.EventType.CERT_BASED_AUTH_EVENT);
    }

    /**
     * Puts a Boolean that describes whether or not a PivProvider instance was already present in the Security static provider list
     *  at the time of proceeding with smartcard CBA.
     * @param isPresent true when a PivProvider instance was already present. false otherwise.
     * @return The Event object.
     */
    public CertBasedAuthEvent putIsExistingPivProviderPresent(final boolean isPresent) {
        put(TelemetryEventStrings.Key.IS_EXISTING_PIVPROVIDER_PRESENT, String.valueOf(isPresent));
        return this;
    }

    /**
     * Puts a Boolean that describes whether or not a PivProvider instance is being removed from the Security static provider list
     *  at the time of YubiKey removal.
     * @param isRemoved true when a PivProvider instance is being removed. false otherwise.
     * @return The Event object.
     */
    public CertBasedAuthEvent putPivProviderRemoved(final boolean isRemoved) {
        put(TelemetryEventStrings.Key.PIVPROVIDER_REMOVED, String.valueOf(isRemoved));
        return this;
    }

    /**
     * Puts a ResultCode received from a RawAuthorizationResult as a result of a completed or failed CBA flow.
     * @param code ResultCode as a string.
     * @return The Event object.
     */
    public CertBasedAuthEvent putResponseCode(final String code) {
        put(TelemetryEventStrings.Key.RESULT_CODE, code);
        return this;
    }

    /**
     * Puts details for an Exception received from a RawAuthorizationResult as a result of a failed CBA flow.
     * Adapted from {@link com.microsoft.identity.common.java.telemetry.events.ErrorEvent#putException(Exception)}.
     * @param exception BaseException received from RawAuthorizationResult.
     * @return The Event object.
     */
    public CertBasedAuthEvent putResponseException(@NonNull final BaseException exception) {
        //Code adapted from ErrorEvent.putException
        final StackTraceElement[] stackTraceElements = exception.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            final StackTraceElement errorLocation = stackTraceElements[0];
            final String tag = exception.getClass().getSimpleName() + exception.getMessage() + errorLocation.getClassName() + errorLocation.getMethodName() + exception.getErrorCode();

            // we add Integer.MAX_VALUE to ensure we don't get negatives. This is to ensure consistency and avoid confusion in the tags generated.
            // For example, if we allow negatives we would generate tags like tag_-123456 and tag_123456, which may seem as similar.
            final String errorTag = ErrorEvent.ERROR_TAG_PREFIX + (((long) tag.hashCode()) + Integer.MAX_VALUE);
            put(TelemetryEventStrings.Key.ERROR_TAG, errorTag);
        }

        put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, exception.getClass().getSimpleName());
        put(TelemetryEventStrings.Key.ERROR_DESCRIPTION, exception.getMessage()); // pii
        if (exception.getCause() != null) {
            put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, exception.getCause().getClass().getSimpleName());
        }

        put(TelemetryEventStrings.Key.ERROR_CODE, exception.getErrorCode());
        put(TelemetryEventStrings.Key.SERVER_ERROR_CODE, exception.getCliTelemErrorCode());
        put(TelemetryEventStrings.Key.SERVER_SUBERROR_CODE, exception.getCliTelemSubErrorCode());

        return this;
    }
}
