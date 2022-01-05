/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

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
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.AbstractPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

/**
 * A Prompt Handler for TLS login flows.
 */
public class TlsPromptHandler extends AbstractPromptHandler {

    private final static String TAG = TlsPromptHandler.class.getSimpleName();
    public final static long CHROME_MENU_BUTTON_RETRY_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private final static int CHROME_MENU_BUTTON_MAX_RETRY_ATTEMPTS = 2;

    public TlsPromptHandler(@NonNull final PromptHandlerParameters parameters) {
        super(new AadLoginComponentHandler(), parameters);
        Logger.i(TAG, "Initializing Tls Prompt Handler..");
    }

    @Override
    public void handlePrompt(@NonNull final String username, @NonNull final String password) {
        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final boolean loginHintProvided = !TextUtils.isEmpty(parameters.getLoginHint());
        // handle browser
        final BrowserChrome chrome = new BrowserChrome();
        chrome.handleFirstRun();
        // click on Open in chrome tabs.
        UiAutomatorUtils.obtainUiObjectWithText("Microsoft");
        try {
            final UiObject menuButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.chrome:id/menu_button");
            for (int attempt = 0; attempt < CHROME_MENU_BUTTON_MAX_RETRY_ATTEMPTS; attempt++) {
                if (attempt > 0) {
                    Thread.sleep(CHROME_MENU_BUTTON_RETRY_TIMEOUT);
                }
                if (menuButton.exists()) {
                    menuButton.click();
                }
            }
            final UiObject openInChrome = UiAutomatorUtils.obtainUiObjectWithText("Open in");
            openInChrome.click();
        } catch (final UiObjectNotFoundException | InterruptedException e) {
            throw new AssertionError(e);
        }
        // in url removing x-client-SKU=MSAL.Android.
        final UiObject urlBar = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.chrome:id/url_bar");
        Assert.assertTrue(urlBar.exists());
        try {
            String url = urlBar.getText();
            url = url.replace("x-client-SKU=MSAL.Android", "");
            urlBar.click();
            urlBar.setText(url);
            uiDevice.pressEnter();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        // check parameters
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

        if (parameters.isPasswordPageExpected() || parameters.getPrompt() == PromptParameter.LOGIN || !parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);
        }
        // installing certificate.
        UiAutomatorUtils.handleButtonClick("android:id/button1");

    }
}
