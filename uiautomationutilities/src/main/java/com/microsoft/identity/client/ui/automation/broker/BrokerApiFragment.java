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
package com.microsoft.identity.client.ui.automation.broker;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import java.util.ArrayList;
import java.util.List;

public class BrokerApiFragment extends AbstractBrokerHost {
    // Resource Id for the buttons
    public final static String GET_ACCOUNTS_BUTTON_ID = "button_get_accounts";
    public final static String REMOVE_ACCOUNTS_BUTTON_ID = "button_remove_account";
    public final static String GET_SSO_TOKEN_BUTTON_ID = "button_get_sso_token";
    // Resource Id for the edit text
    public final static String SSO_TOKEN_EDIT_TEXT_ID = "edit_sso_token";
    public final static String NONCE_EDIT_TEXT_ID = "edit_text_nonce";

    public List<String> getAccounts() {
        List<String> accounts = new ArrayList<>();
        clickButton(GET_ACCOUNTS_BUTTON_ID);
        UiObject dialogBox;
        do {
            final String accountName = dismissDialogBoxAndGetText();
            if (accountName != null && !accountName.contains("No accounts")) {
                accounts.add(accountName);
            }
            delay(2);
            dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(DIALOG_BOX_RESOURCE_ID);
        } while (dialogBox.exists());
        return accounts;
    }

    public void removeAccounts(@NonNull final String username) {
        fillTextBox(USERNAME_EDIT_TEXT, username);
        clickButton(REMOVE_ACCOUNTS_BUTTON_ID);
        dismissDialogBoxAndGetText();
    }

    public String acquireSsoToken(@NonNull final String nonce) {
        fillTextBox(NONCE_EDIT_TEXT_ID, nonce);
        clickButton(GET_SSO_TOKEN_BUTTON_ID);
        return readTextBox(SSO_TOKEN_EDIT_TEXT_ID);
    }

    @Override
    public void launch() {
        launch(BrokerHostNavigationMenuItem.BROKER_API);
    }
}
