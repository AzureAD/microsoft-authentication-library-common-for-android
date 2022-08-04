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
