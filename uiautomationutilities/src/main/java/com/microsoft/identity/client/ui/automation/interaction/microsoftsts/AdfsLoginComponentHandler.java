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

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject2;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A login component handler for ADFS.
 */
public class AdfsLoginComponentHandler extends AadLoginComponentHandler {

    private final static String TAG = AdfsLoginComponentHandler.class.getSimpleName();

    @Override
    public void handleEmailField(@NonNull final String username) {
        try {
            UiAutomatorUtils.handleInput("userNameInput", username, CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
        } catch (AssertionError e) {
            // If we can't find email field with resource id, let's try to find edit text objects
            final UiObject2 usernameObject = UiAutomatorUtils.obtainAllEditTextObjects(CommonUtils.FIND_UI_ELEMENT_TIMEOUT).get(0);
            usernameObject.setText(username);
        }
    }

    @Override
    public void handlePasswordField(@NonNull final String password) {
        Logger.i(TAG, "Handle Adfs Login Password UI..");
        try {
            UiAutomatorUtils.handleInput("passwordInput", password, CommonUtils.FIND_UI_ELEMENT_TIMEOUT_SHORT);
            UiAutomatorUtils.handleButtonClick("submitButton");
        } catch (AssertionError e) {
            // If we can't find password field with resource id, let's try to find edit text objects
            final UiObject2 passwordObject = UiAutomatorUtils.obtainAllEditTextObjects(CommonUtils.FIND_UI_ELEMENT_TIMEOUT).get(1);
            passwordObject.setText(password);
            UiAutomatorUtils.handleButtonClickForObjectWithExactText("Sign in");
        }
    }

    /**
     * Enters username, password in email, password fields of an enrollment page
     */
    public void handleEnrollmentPrompt(@NonNull final String username, @NonNull final String password) {
        Logger.i(TAG, "Handle prompt in ADFS login page for enrolling");
        // handle AAD login page for username
        UiAutomatorUtils.handleInput("i0116", username);
        UiAutomatorUtils.handleButtonClick("idSIButton9");
    }
}
