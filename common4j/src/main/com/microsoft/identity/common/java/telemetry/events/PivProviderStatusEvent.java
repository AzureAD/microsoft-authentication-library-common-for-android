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

/**
 * Event for emitting status information of when a PivProvider is present, added, or removed from the Security static list.
 */
public class PivProviderStatusEvent extends BaseEvent {

    /**
     * Creates a new instance of PivProviderStatusEvent.
     */
    public PivProviderStatusEvent() {
        super();
        names(TelemetryEventStrings.Event.PIV_PROVIDER_STATUS_EVENT);
        types(TelemetryEventStrings.EventType.YUBIKEY_EVENT);
    }

    /**
     * Puts a Boolean that describes whether or not a PivProvider instance was already present in the Security static provider list
     *  at the time of proceeding with smartcard CBA.
     * @param isPresent true when a PivProvider instance was already present. false otherwise.
     * @return The Event object.
     */
    public PivProviderStatusEvent putIsExistingPivProviderPresent(final boolean isPresent) {
        put(TelemetryEventStrings.Key.IS_EXISTING_PIVPROVIDER_PRESENT, String.valueOf(isPresent));
        return this;
    }

    /**
     * Puts a Boolean that describes whether or not a PivProvider instance is being removed from the Security static provider list
     *  at the time of YubiKey removal.
     * @param isRemoved true when a PivProvider instance is being removed. false otherwise.
     * @return The Event object.
     */
    public PivProviderStatusEvent putPivProviderRemoved(final boolean isRemoved) {
        put(TelemetryEventStrings.Key.PIVPROVIDER_REMOVED, String.valueOf(isRemoved));
        return this;
    }
}
