package com.microsoft.identity.client.ui.automation.interaction;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
/**
 * A set of values that can be used to denote the behaviour we expect to observe during an oauth
 * authorization request. These values can then be supplied to a {@link AbstractPromptHandler} and
 * used to handle prompt in the manner as expected during the execution of a given test.
 * The test should fail if the actual behaviour observed deviates from what is denoted as expected
 * via these parameters.
 */
public class PromptHandlerParameters {

    @NonNull
    // the prompt behaviour we expect
    private PromptParameter prompt;

    // whether session is expected or not (via presence of a cookie)
    private boolean sessionExpected;

    // whether login hint was supplied or not to the interactive request
    private boolean loginHintProvided;

    // whether we expect to receive consent page or not
    private boolean consentPageExpected;

    // whether we expect to receive speed bump or not
    private boolean speedBumpExpected;

    // whether we are expecting at least one account in broker
    private boolean expectingNonZeroAccountsInBroker;

    // whether we are expecting at least one account in the browser/webview cookie
    private boolean expectingNonZeroAccountsInCookie;

    @Builder.Default
    // The way in which we want to respond to consent page for this request
    private UiResponse consentPageResponse = UiResponse.ACCEPT;

    @Builder.Default
    // The way in which we want to respond to speed bump page for this request
    private UiResponse speedBumpResponse = UiResponse.ACCEPT;

    // The broker that should be being used for this request
    private ITestBroker broker;
}
