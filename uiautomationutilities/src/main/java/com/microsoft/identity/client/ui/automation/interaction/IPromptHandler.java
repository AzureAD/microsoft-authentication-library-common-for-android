package com.microsoft.identity.client.ui.automation.interaction;

public interface IPromptHandler {

    /**
     * Responds to the prompt for credentials during an authorization request
     * @param username
     * @param password
     */
    void handlePrompt(String username, String password);
}
