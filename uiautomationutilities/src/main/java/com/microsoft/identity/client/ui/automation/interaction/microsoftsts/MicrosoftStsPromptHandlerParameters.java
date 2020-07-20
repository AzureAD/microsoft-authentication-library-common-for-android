package com.microsoft.identity.client.ui.automation.interaction.microsoftsts;

import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class MicrosoftStsPromptHandlerParameters extends PromptHandlerParameters {

    private boolean isFederated;
}
