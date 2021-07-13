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
package com.microsoft.identity.client.ui.automation.interaction.b2c;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.interaction.AbstractPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.IOAuth2LoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;

/**
 * A Prompt handler for MSIDLAB B2C SISO Policy.
 */
public class IdLabB2cSisoPolicyPromptHandler extends AbstractPromptHandler {

    private final static String TAG = IdLabB2cSisoPolicyPromptHandler.class.getSimpleName();

    public IdLabB2cSisoPolicyPromptHandler(@NonNull final B2CPromptHandlerParameters parameters) {
        super(getAppropriateLoginComponentHandler(parameters), parameters);
    }

    @Override
    public void handlePrompt(@NonNull final String username, @NonNull final String password) {
        Logger.i(TAG, "IdLab B2c Siso Policy Prompt Handler..");
        final B2CPromptHandlerParameters b2CPromptHandlerParameters =
                (B2CPromptHandlerParameters) parameters;

        final B2CProvider b2CProvider = b2CPromptHandlerParameters.getB2cProvider();

        final boolean isExternalIdP = b2CProvider != B2CProvider.Local;

        if (isExternalIdP) {
            assert b2CProvider.getIdpSelectionBtnResourceId() != null;
            UiAutomatorUtils.handleButtonClick(b2CProvider.getIdpSelectionBtnResourceId());
        }

        final boolean loginHintProvided = !TextUtils.isEmpty(parameters.getLoginHint());

        if ((!isExternalIdP && !loginHintProvided) ||
                (isExternalIdP && !parameters.isSessionExpected())) {
            loginComponentHandler.handleEmailField(username);
        }

        if (!parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);

            if (loginComponentHandler instanceof GoogleLoginComponentHandler &&
                    b2CProvider == B2CProvider.Google){
                ((GoogleLoginComponentHandler) loginComponentHandler).handleRecoveryEmail();
            }
        }
    }

    protected static IOAuth2LoginComponentHandler getAppropriateLoginComponentHandler(@NonNull final B2CPromptHandlerParameters parameters) {
        Logger.i(TAG, "Get Appropriate Login Component Handler..");
        switch (parameters.getB2cProvider().getProviderName()) {
            case LabConstants.B2CProvider.LOCAL:
                return new B2CIdLabLocalLoginComponentHandler();
            case LabConstants.B2CProvider.GOOGLE:
                return new GoogleLoginComponentHandler();
            case LabConstants.B2CProvider.FACEBOOK:
                return new FacebookLoginComponentHandler();
            case LabConstants.B2CProvider.MICROSOFT:
                return new AadLoginComponentHandler();
            default:
                throw new UnsupportedOperationException("Unsupported B2C Provider for this policy");
        }
    }
}
