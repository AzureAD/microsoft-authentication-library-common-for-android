//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.interaction;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * A set of values that can be used to denote the behaviour we expect to observe during an oauth
 * authorization request. These values can then be supplied to a {@link AbstractPromptHandler} and
 * used to handle prompt in the manner as expected during the execution of a given test.
 * The test should fail if the actual behaviour observed deviates from what is denoted as expected
 * via these parameters.
 */
@SuperBuilder
@Getter
public class PromptHandlerParameters {

    // the prompt behaviour we expect
    @NonNull
    private final PromptParameter prompt;

    // whether session is expected or not (via presence of a cookie)
    private final boolean sessionExpected;

    // whether login hint was supplied or not to the interactive request
    private final boolean loginHintProvided;

    // whether we expect to receive consent page or not
    private final boolean consentPageExpected;

    // whether we expect to receive speed bump or not
    private final boolean speedBumpExpected;

    // whether we are expecting at least one account in broker
    private final boolean expectingNonZeroAccountsInBroker;

    // whether we are expecting at least one account in the browser/webview cookie
    private final boolean expectingNonZeroAccountsInCookie;

    // The way in which we want to respond to consent page for this request
    @Builder.Default
    private final UiResponse consentPageResponse = UiResponse.ACCEPT;

    // The way in which we want to respond to speed bump page for this request
    @Builder.Default
    private final UiResponse speedBumpResponse = UiResponse.ACCEPT;

    // The broker that should be being used for this request
    private final ITestBroker broker;
}
