package com.microsoft.identity.client.ui.automation.interaction;

import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class FirstPartyAppPromptHandlerParameters extends MicrosoftStsPromptHandlerParameters {

    // whether we are expecting at least one account in TSL
    private final boolean expectingNonZeroAccountsInTSL;

    // whether we are expecting the account / token to be in TSL
    private final boolean expectingProvidedAccountInTSL;
}
