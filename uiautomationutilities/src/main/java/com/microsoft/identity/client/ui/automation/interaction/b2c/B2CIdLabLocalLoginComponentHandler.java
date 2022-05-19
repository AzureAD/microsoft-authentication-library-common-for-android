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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

/**
 * A login component handler for B2C Local IdP.
 */
public class B2CIdLabLocalLoginComponentHandler extends AbstractB2CLoginComponentHandler {

    private final static String TAG = B2CIdLabLocalLoginComponentHandler.class.getSimpleName();

    @Override
    protected String getHandlerName() {
        return B2CProviderWrapper.Local.getProviderName();
    }

    @Override
    public void handleEmailField(@NonNull final String username) {
        UiAutomatorUtils.handleInput("logonIdentifier", username);
    }

    @Override
    public void handlePasswordField(@NonNull final String password) {
        Logger.i(TAG, "Handle B2C IdLab Local Login Password UI..");
        UiAutomatorUtils.handleInput("password", password);
        handleNextButton();
    }

    @Override
    public void handleBackButton() {
        UiAutomatorUtils.pressBack();
    }

    @Override
    public void handleNextButton() {
        UiAutomatorUtils.handleButtonClick("next");
    }
}
