package com.microsoft.identity.client.ui.automation.interaction;

public interface ILoginComponentHandler {

    /**
     * Enters the supplied username in the email field of a login page
     *
     * @param username the email of the user
     */
    void handleEmailField(final String username);

    /**
     * Enters the supplied password in the password field of a login page
     *
     * @param password the password of the user
     */
    void handlePasswordField(final String password);

    /**
     * Clicks the back button on a login page
     */
    void handleBackButton();

    /**
     * Clicks the next button on a login page
     */
    void handleNextButton();

    /**
     * Clicks (selects) the list item on the account picker screen associated to the supplied username
     *
     * @param username the upn of the user to select
     */
    void handleAccountPicker(final String username);

    /**
     * Used to assert if the oauth consent page is received or not
     */
    void confirmConsentPageReceived();

    /**
     * Accept the consent on the oauth consent page
     */
    void acceptConsent();

    /**
     * Decline the consent on the oauth consent page
     */
    void declineConsent();

    /**
     * Respond to the speed bump encountered during an authorization request
     */
    void handleSpeedBump();
}
