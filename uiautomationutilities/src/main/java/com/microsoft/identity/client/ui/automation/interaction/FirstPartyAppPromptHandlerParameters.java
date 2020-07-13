package com.microsoft.identity.client.ui.automation.interaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class FirstPartyAppPromptHandlerParameters extends PromptHandlerParameters {

    // whether we are expecting at least one account in TSL
    private final boolean expectingNonZeroAccountsInTSL;

    // whether we are expecting the account / token to be in TSL
    private final boolean expectingProvidedAccountInTSL;
}
