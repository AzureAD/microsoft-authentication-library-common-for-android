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

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A login component handler for Google IdP.
 */
public class GoogleLoginComponentHandler extends AbstractB2CLoginComponentHandler {

    private final static boolean RECOVERY_EMAIL_PROMPT_EXPECTED = false;
    private final static String TAG = GoogleLoginComponentHandler.class.getSimpleName();
    private final static String RECOVERY_EMAIL_BUTTON_TEXT = "Confirm your recovery email";
    private final static String RECOVERY_EMAIL_INPUT_RESOURCE_ID = "knowledge-preregistered-email-response";
    private final static  String RECOVERY_EMAIL = "msidlabint@microsoft.com";

    @Override
    protected String getHandlerName() {
        return B2CProviderWrapper.Google.getProviderName();
    }

    @Override
    public void handleEmailField(@NonNull final String username) {
        UiAutomatorUtils.handleInput("identifierId", username);
        handleNextButton();
    }

    @Override
    public void handlePasswordField(@NonNull final String password) {
        Logger.i(TAG, "Handle Google Login Password UI..");
        UiAutomatorUtils.handleInput("password", password);
        final UiObject passwordBox = UiAutomatorUtils.obtainUiObjectWithResourceId("password");

        try {
            final UiObject passwordInput = passwordBox.getChild(
                    new UiSelector().className(EditText.class)
            );

            passwordInput.setText(password);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        handleNextButton();
    }

    @Override
    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    @Override
    public void handleNextButton() {
        final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithText("Next");
        try {
            nextBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public void handleRecoveryEmail() {
        if(RECOVERY_EMAIL_PROMPT_EXPECTED) {
            Logger.i(TAG, "Handle Google Recovery Email UI..");
            final UiObject confirmationEmailButton = UiAutomatorUtils.obtainUiObjectWithText(RECOVERY_EMAIL_BUTTON_TEXT);
            if (confirmationEmailButton.exists()) {
                try {
                    confirmationEmailButton.click();
                    UiAutomatorUtils.handleInput(RECOVERY_EMAIL_INPUT_RESOURCE_ID, RECOVERY_EMAIL);
                    handleNextButton();
                } catch (final UiObjectNotFoundException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }
}
