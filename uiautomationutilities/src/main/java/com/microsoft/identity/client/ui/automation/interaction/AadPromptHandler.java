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

import androidx.annotation.NonNull;

public class AadPromptHandler extends AbstractPromptHandler {

    public AadPromptHandler(@NonNull final PromptHandlerParameters parameters) {
        super(new AadLoginComponentHandler(), parameters);
    }

    public void handlePrompt(@NonNull final String username, @NonNull final String password) {
        // if login hint was not provided, then we need to handle either account picker or email
        // field. If it was provided, then we expect to go straight to password field.
        if (!parameters.isLoginHintProvided()) {
            if (parameters.getBroker() != null && parameters.isExpectingNonZeroAccountsInBroker()) {
                parameters.getBroker().handleAccountPicker(username);
            } else if (parameters.isExpectingNonZeroAccountsInCookie()) {
                loginComponentHandler.handleAccountPicker(username);
            } else {
                loginComponentHandler.handleEmailField(username);
            }
        }

        if (parameters.getPrompt() == PromptParameter.LOGIN || !parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);
        }

        if (parameters.isConsentPageExpected() || parameters.getPrompt() == PromptParameter.CONSENT) {
            final UiResponse consentPageResponse = parameters.getConsentPageResponse();
            if (consentPageResponse == UiResponse.ACCEPT) {
                loginComponentHandler.acceptConsent();
            } else {
                loginComponentHandler.declineConsent();
            }
        }

        if (parameters.isSpeedBumpExpected()) {
            loginComponentHandler.handleSpeedBump();
        }
    }
}
