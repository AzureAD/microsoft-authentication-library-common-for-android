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
package com.microsoft.identity.client.ui.automation.interaction.microsoftsts;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.interaction.AbstractPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.client.ui.automation.logging.Logger;

/**
 * A Prompt Handler for Microsoft STS login flows.
 */
public class MicrosoftStsPromptHandler extends AbstractPromptHandler {

    private final static String TAG = MicrosoftStsPromptHandler.class.getSimpleName();

    public MicrosoftStsPromptHandler(
            @NonNull MicrosoftStsPromptHandlerParameters parameters) {
        super(
                parameters.isFederated() ? new AdfsLoginComponentHandler() : new AadLoginComponentHandler(),
                parameters
        );
        Logger.i(TAG, "Initializing Microsoft Sts Prompt Handler..");
    }

    public MicrosoftStsPromptHandler(
            @NonNull final IMicrosoftStsLoginComponentHandler loginComponentHandler,
            @NonNull final PromptHandlerParameters parameters) {
        super(
                loginComponentHandler,
                parameters
        );
        Logger.i(TAG, "Initializing Microsoft Sts Prompt Handler..");
    }

    @Override
    public void handlePrompt(@NonNull final String username, @NonNull final String password) {
        final boolean loginHintProvided = !TextUtils.isEmpty(parameters.getLoginHint());

        final IMicrosoftStsLoginComponentHandler aadLoginComponentHandler =
                (IMicrosoftStsLoginComponentHandler) loginComponentHandler;

        // if login hint was not provided, then we need to handle either account picker or email
        // field. If it was provided, then we expect to go straight to password field.
        if (!loginHintProvided) {
            if (parameters.getBroker() != null && parameters.isExpectingBrokerAccountChooserActivity()) {
                parameters.getBroker().handleAccountPicker(username);
            } else if (parameters.isExpectingLoginPageAccountPicker()) {
                loginComponentHandler.handleAccountPicker(username);
            } else {
                loginComponentHandler.handleEmailField(username);
            }
        } else if (!parameters.getLoginHint().equalsIgnoreCase(username)) {
            loginComponentHandler.handleEmailField(username);
        }

        if (parameters.isHowWouldYouLikeToSignInExpected()) {
            aadLoginComponentHandler.handleHowWouldYouLikeToSignIn();
        }

        if (parameters.isPasswordPageExpected() || parameters.getPrompt() == PromptParameter.LOGIN || !parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);
        }

        if (parameters.isVerifyYourIdentityPageExpected()) {
            aadLoginComponentHandler.handleVerifyYourIdentity();
        }

        if (parameters.isConsentPageExpected() || parameters.getPrompt() == PromptParameter.CONSENT) {
            final UiResponse consentPageResponse = parameters.getConsentPageResponse();
            if (consentPageResponse == UiResponse.ACCEPT) {
                loginComponentHandler.acceptConsent();
            } else {
                loginComponentHandler.declineConsent();
            }
        }

        if (parameters.isStaySignedInPageExpected()) {
            aadLoginComponentHandler.handleStaySignedIn(parameters.getStaySignedInResponse());
        }

        if (parameters.isSpeedBumpExpected()) {
            aadLoginComponentHandler.handleSpeedBump();
        }

        if (parameters.isRegisterPageExpected()) {
            aadLoginComponentHandler.handleRegistration();
        }

        if (parameters.isEnrollPageExpected()) {
            final UiResponse enrollPageResponse = parameters.getEnrollPageResponse();
            if (enrollPageResponse == UiResponse.ACCEPT) {
                aadLoginComponentHandler.acceptEnroll();
            } else {
                aadLoginComponentHandler.declineEnroll();
            }
        }
    }
}
