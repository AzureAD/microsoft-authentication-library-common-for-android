package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;

/**
 * An interface describing a first party application and the actions that can be performed on them.
 */
public interface IFirstPartyApp {

    /**
     * Add the first user account to this first party app
     *
     * @param username                the username of the account to add
     * @param password                the password of the account to add
     * @param promptHandlerParameters the prompt handler parameters indicating how to handle prompt
     */
    void addFirstAccount(@NonNull final String username,
                         @NonNull final String password,
                         @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    /**
     * Add another account to this first party app. This must only be called if the an account was
     * previously added to this first party app.
     *
     * @param username                the username of the account to add
     * @param password                the password of the account to add
     * @param promptHandlerParameters the prompt handler parameters indicating how to handle prompt
     */
    void addAnotherAccount(final String username,
                           final String password,
                           final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    /**
     * This method can be called handle welcome screens in the first party app that appear on the
     * successful addition of an account to that first party app
     */
    void onAccountAdded();

    /**
     * Confirms whether the supplied user exists (signed in) in this first party app
     *
     * @param username the username of the account to confirm
     */
    void confirmAccount(@NonNull final String username);


}
