package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;

public interface IFirstPartyApp {

    void addFirstAccount(@NonNull final String username,
                         @NonNull final String password,
                         @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    void addAnotherAccount(final String username,
                           final String password,
                           final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    void onAccountAdded();

    void confirmAccount(@NonNull final String username);




}
