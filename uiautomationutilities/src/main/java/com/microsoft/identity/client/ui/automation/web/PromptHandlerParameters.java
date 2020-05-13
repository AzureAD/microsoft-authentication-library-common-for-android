package com.microsoft.identity.client.ui.automation.web;

import com.microsoft.identity.client.ui.automation.PromptParameter;
import com.microsoft.identity.client.ui.automation.UiResponse;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class PromptHandlerParameters {

    @NonNull
    private PromptParameter prompt;
    private boolean sessionExpected;
    private boolean loginHintProvided;
    private boolean consentPageExpected;
    private boolean speedBumpExpected;
    private boolean expectingNonZeroAccountsInBroker;
    private boolean expectingNonZeroAccountsInCookie;

    @Builder.Default
    private UiResponse consentPageResponse = UiResponse.ACCEPT;

    @Builder.Default
    private UiResponse speedBumpResponse = UiResponse.ACCEPT;

    private ITestBroker broker;
}
