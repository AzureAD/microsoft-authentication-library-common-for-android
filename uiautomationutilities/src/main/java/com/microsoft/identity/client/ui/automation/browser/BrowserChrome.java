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
package com.microsoft.identity.client.ui.automation.browser;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

public class BrowserChrome extends App {

    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";
    private static final String CHROME_APP_NAME = "Google Chrome";

    public BrowserChrome() {
        super(CHROME_PACKAGE_NAME, CHROME_APP_NAME);
    }

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/terms_accept");
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/negative_button");
    }
}
