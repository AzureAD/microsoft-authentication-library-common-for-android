// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.ui.browser;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;

public class BrowserAuthorizationStrategy <GenericAuthorizationRequest extends AuthorizationRequest> extends AuthorizationStrategy <GenericAuthorizationRequest> {

    private final Activity mActivity; //NOPMD
    private final AuthorizationConfiguration mAuthorizationConfiguration; //NOPMD //TODO Heidi
    private CustomTabsManager mCustomTabManager;

    public BrowserAuthorizationStrategy(@NonNull final Activity activity,
                                              @NonNull AuthorizationConfiguration configuration) {
        mActivity = activity;
        mAuthorizationConfiguration = configuration;
    }

    // 1. Initial
    // 2. Select the browser
    // mBrowser = BrowserSelector.select(mActivityRef.get().getApplicationContext());
    // 3.a. If custom tab enabled, use bind custom tab session
    // 3.b. If custom tab disabled, launch the url with browser
    @Override
    public void requestAuthorization(GenericAuthorizationRequest request) throws ClientException {
        //TODO
        final Browser browser = BrowserSelector.select(mActivity.getApplicationContext());
        if (browser.isCustomTabsServiceSupported()) {
            mCustomTabManager = new CustomTabsManager(mActivity);
            mCustomTabManager.bind(browser.getPackageName());
        } else {

        }
    }
}
