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

    /**
     * The prompt behaviour expected to be observed during an interactive token request.
     */
    @NonNull
    private final PromptParameter prompt;

    /**
     * The login hint that was provided to the interactive token request.
     */
    private final String loginHint;

    /**
     * Denotes whether session is expected during an interactive token request.
     */
    private final boolean sessionExpected;

    /**
     * Denotes the broker that is expected to be used during an interactive token request.
     */
    private final ITestBroker broker;

    /**
     * Denotes whether the broker account chooser activity is expected to appear during an
     * interactive token request.
     */
    private final boolean expectingBrokerAccountChooserActivity;

    /**
     * Denotes whether the account being used during an interactive token request is expected to
     * exist in the broker.
     */
    private final boolean expectingProvidedAccountInBroker;

    /**
     * Denotes whether the AAD login page account picker is expected to appear during an
     * interactive token request.
     */
    private final boolean expectingLoginPageAccountPicker;

    /**
     * Denotes whether the account being used during an interactive token request is expected to
     * exist in the AAD login web page cookies. This determines if that account would appear in
     * AAD account picker.
     */
    private final boolean expectingProvidedAccountInCookie;

    /**
     * Denotes whether or not the consent page is expected to appear during an interactive token
     * request.
     */
    private final boolean consentPageExpected;

    /**
     * Denotes whether or not the password page is expected to appear during an interactive token
     * request.
     */
    private final boolean passwordPageExpected;

    /**
     * Denotes whether or not the speed bump page is expected to appear during an interactive token
     * request.
     */
    private final boolean speedBumpExpected;

    /**
     * Denotes whether or not the register page is expected to appear during an interactive token
     * request.
     */
    private final boolean registerPageExpected;

    /**
     * Denotes whether or not the enroll page is expected to appear during an interactive token
     * request.
     */
    private final boolean enrollPageExpected;

    /**
     * Denotes whether or not the Stay signed in page is expected to appear during an interactive
     * token request
     */
    private final boolean staySignedInPageExpected;

    /**
     * Denotes whether or not the Verify Your Identity page is expected to appear during an interactive
     * token request
     */
    private final boolean verifyYourIdentityPageExpected;

    /**
     * Denotes name of the certificate to use to sign the account in rather than a password
     */
    private final String userCertificate;

    /**
     * Denotes the way in which we want to respond to the enroll page for this request.
     */
    @Builder.Default
    private final UiResponse enrollPageResponse = UiResponse.ACCEPT;

    /**
     * Denotes the way in which we want to respond to the consent page for this request.
     */
    @Builder.Default
    private final UiResponse consentPageResponse = UiResponse.ACCEPT;

    /**
     * Denotes the way in which we want to respond to the register page for this request.
     */
    @Builder.Default
    private final UiResponse speedBumpResponse = UiResponse.ACCEPT;

    @Builder.Default
    private final UiResponse staySignedInResponse = UiResponse.ACCEPT;
}
