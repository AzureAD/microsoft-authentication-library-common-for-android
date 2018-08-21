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
package com.microsoft.identity.common.internal.ui.webview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFuture;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

/**
 * Serve as a class to do the OAuth2 auth code grant flow with Android embedded web view.
 */
public class EmbeddedWebViewAuthorizationStrategy extends AuthorizationStrategy {

    private static final String TAG = StringExtensions.class.getSimpleName();
    private AuthorizationConfiguration mConfiguration;
    private WeakReference<Activity> mReferencedActivity;
    private AuthorizationResultFuture mAuthorizationResultFuture;

    /**
     * Constructor of EmbeddedWebViewAuthorizationStrategy.
     *
     * @param activity The app activity which invoke the interactive auth request.
     */
    public EmbeddedWebViewAuthorizationStrategy(Activity activity, @NonNull final AuthorizationConfiguration configuration) {
        mConfiguration = configuration;
        mReferencedActivity =  new WeakReference<>(activity);
    }

    /**
     * RequestAuthorization could not return the authorization result.
     * The activity result is set in Authorization.setResult() and passed to the onActivityResult() of the calling activity.
     *
     * @param requestUrl authorization request url
     */
    @Override
    public Future<AuthorizationResult> requestAuthorization(final Uri requestUrl) {
        mAuthorizationResultFuture = new AuthorizationResultFuture();
        Logger.verbose(TAG, "Perform the authorization request with embedded webView.");
        final Intent authIntent = AuthorizationActivity.createStartIntent(
                mReferencedActivity.get().getApplicationContext(),
                null,
                requestUrl.toString(),
                mConfiguration);
        mReferencedActivity.get().startActivityForResult(authIntent, BROWSER_FLOW);
        return mAuthorizationResultFuture;
    }

    @Override
    public void completeAuthorization(int requestCode, int resultCode, Intent data) {
        if (requestCode != BROWSER_FLOW) {
            throw new IllegalStateException("Unknown request code");
        }

        final AuthorizationResult result = new MicrosoftStsAuthorizationResultFactory().createAuthorizationResult(resultCode, data);
        mAuthorizationResultFuture.setAuthorizationResult(result);
    }
}
