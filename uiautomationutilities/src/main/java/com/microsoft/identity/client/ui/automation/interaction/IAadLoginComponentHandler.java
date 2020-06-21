package com.microsoft.identity.client.ui.automation.interaction;

public interface IAadLoginComponentHandler extends IOAuth2LoginComponentHandler {

    /**
     * Respond to the speed bump encountered during an authorization request
     */
    void handleSpeedBump();

    void confirmEnrollPageReceived();

    void acceptEnroll();

    void declineEnroll();
}
