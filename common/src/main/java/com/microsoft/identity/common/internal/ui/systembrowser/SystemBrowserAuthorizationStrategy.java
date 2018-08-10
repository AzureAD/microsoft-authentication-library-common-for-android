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
package com.microsoft.identity.common.internal.ui.systembrowser;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.embeddedwebview.challengehandlers.IChallengeCompletionCallback;

public class SystemBrowserAuthorizationStrategy <GenericAuthorizationRequest extends AuthorizationRequest> extends AuthorizationStrategy {

    private final GenericAuthorizationRequest mAuthorizationRequest; //NOPMD //TODO Heidi
    private final Activity mActivity; //NOPMD
    private final AuthorizationConfiguration mAuthorizationConfiguration; //NOPMD //TODO Heidi


    public SystemBrowserAuthorizationStrategy(@NonNull final Activity activity,
                                              @NonNull GenericAuthorizationRequest authorizationRequest,
                                              @NonNull AuthorizationConfiguration configuration) {
        mActivity = activity;
        mAuthorizationRequest = authorizationRequest;
        mAuthorizationConfiguration = configuration;
    }

    // 1. Initial
    // 2. Select the browser
    // mBrowser = BrowserSelector.select(mActivityRef.get().getApplicationContext());
    // 3.a. If custom tab enabled, use bind custom tab session
    // 3.b. If custom tab disabled, launch the url with browser
    public AuthorizationResult requestAuthorization(AuthorizationRequest request) {
        return null;
    }
}
