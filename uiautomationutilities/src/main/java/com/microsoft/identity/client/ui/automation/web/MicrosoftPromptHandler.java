package com.microsoft.identity.client.ui.automation.web;

import com.microsoft.identity.client.ui.automation.PromptParameter;
import com.microsoft.identity.client.ui.automation.UiResponse;

public class MicrosoftPromptHandler extends AbstractPromptHandler {

    public MicrosoftPromptHandler(
            final PromptHandlerParameters parameters) {
        super(
                new MicrosoftLoginComponentHandler(),
                parameters
        );
    }

    public void handlePrompt(final String username, final String password) {
        if (!parameters.isLoginHintProvided()) {
            if (parameters.getBroker() != null && parameters.isExpectingNonZeroAccountsInBroker()) {
                parameters.getBroker().handleAccountPicker(username);
            } else if (parameters.isExpectingNonZeroAccountsInCookie()) {
                loginComponentHandler.handleAccountPicker(username);
            } else {
                loginComponentHandler.handleEmailField(username);
            }
        }

        if (parameters.getPrompt() == PromptParameter.LOGIN || !parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);
        }

        if (parameters.isConsentPageExpected() || parameters.getPrompt() == PromptParameter.CONSENT) {
            final UiResponse consentPageResponse = parameters.getConsentPageResponse();
            if (consentPageResponse == UiResponse.ACCEPT) {
                loginComponentHandler.acceptConsent();
            } else {
                loginComponentHandler.declineConsent();
            }
        }

        if (parameters.isSpeedBumpExpected()) {
            loginComponentHandler.handleSpeedBump();
        }
    }
}
