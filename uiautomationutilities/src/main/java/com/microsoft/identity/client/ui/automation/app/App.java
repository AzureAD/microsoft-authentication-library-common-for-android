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
package com.microsoft.identity.client.ui.automation.app;

import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.PlayStoreUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class App implements IApp {

    private String packageName;

    @Setter
    private String appName;

    public App(String packageName) {
        this.packageName = packageName;
    }

    public App(String packageName, String appName) {
        this.packageName = packageName;
        this.appName = appName;
    }

    @Override
    public void install() {
        PlayStoreUtils.installApp(appName != null ? appName : packageName);
    }

    @Override
    public void launch() {
        CommonUtils.launchApp(packageName);
    }

    @Override
    public void clear() {
        CommonUtils.clearApp(packageName);
    }

    @Override
    public void uninstall() {
        CommonUtils.removeApp(packageName);
    }
}
