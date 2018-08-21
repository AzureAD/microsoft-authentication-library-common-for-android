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
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFuture;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

public class BrowserAuthorizationStrategy extends AuthorizationStrategy {
    private final static String TAG = BrowserAuthorizationStrategy.class.getSimpleName();

    private final AuthorizationConfiguration mConfiguration;
    private CustomTabsManager mCustomTabManager;
    private WeakReference<Activity> mReferencedActivity;
    private AuthorizationResultFuture mAuthorizationResultFuture;
    private boolean mDisposed;

    public BrowserAuthorizationStrategy(Activity activity, @NonNull AuthorizationConfiguration configuration) {
        mConfiguration = configuration;
        mReferencedActivity =  new WeakReference<>(activity);
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(final Uri requestUrl) throws ClientException {
        checkNotDisposed();
        mAuthorizationResultFuture = new AuthorizationResultFuture();
        final Browser browser = BrowserSelector.select(mReferencedActivity.get().getApplicationContext());

        //ClientException will be thrown if no browser found.
        Intent authIntent;
        if (browser.isCustomTabsServiceSupported()) {
            //create customTabsIntent
            mCustomTabManager = new CustomTabsManager(mReferencedActivity.get());
            mCustomTabManager.bind(browser.getPackageName());
            authIntent = mCustomTabManager.getCustomTabsIntent().intent;
        } else {
            //create browser auth intent
            authIntent = new Intent(Intent.ACTION_VIEW);
        }

        authIntent.setPackage(browser.getPackageName());
        authIntent.setData(requestUrl);
        mReferencedActivity.get().startActivityForResult(AuthorizationActivity.createStartIntent(mReferencedActivity.get().getApplicationContext(),authIntent, requestUrl.toString(), mConfiguration),BROWSER_FLOW);

        return mAuthorizationResultFuture;
    }

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }


    @Override
    public void completeAuthorization(int requestCode, int resultCode, Intent data) {
        if (requestCode == BROWSER_FLOW) {
            //TODO need to implement OAuth2StrategyFactory.getByType().getAuthorizationResult();
            dispose();
            final AuthorizationResult result = new MicrosoftStsAuthorizationResultFactory().createAuthorizationResult(resultCode, data);
            mAuthorizationResultFuture.setAuthorizationResult(result);
        } else {
            Logger.warnPII(TAG, "Unknown request code " + requestCode);
        }
    }

    /**
     * Disposes state that will not normally be handled by garbage collection. This should be
     * called when the authorization service is no longer required, including when any owning
     * activity is paused or destroyed (i.e. in {@link android.app.Activity#onStop()}).
     */
    public void dispose() {
        if (mDisposed) {
            return;
        }
        mCustomTabManager.unbind();
        mDisposed = true;
    }
}