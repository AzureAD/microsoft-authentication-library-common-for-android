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
import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Future;

public class BrowserAuthorizationStrategy<GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest> extends AuthorizationStrategy<GenericOAuth2Strategy, GenericAuthorizationRequest> {
    private final static String TAG = BrowserAuthorizationStrategy.class.getSimpleName();

    private CustomTabsManager mCustomTabManager;
    private WeakReference<Activity> mReferencedActivity;
    private ResultFuture<AuthorizationResult> mAuthorizationResultFuture;
    private List<BrowserDescriptor> mBrowserSafeList;
    private boolean mDisposed;
    private GenericOAuth2Strategy mOAuth2Strategy; //NOPMD
    private GenericAuthorizationRequest mAuthorizationRequest; //NOPMD
    private boolean mIsRequestFromBroker;

    public BrowserAuthorizationStrategy(@NonNull Activity activity, @NonNull boolean isRequestFromBroker) {
        mReferencedActivity = new WeakReference<>(activity);
        mIsRequestFromBroker = isRequestFromBroker;
    }

    public void setBrowserSafeList(final List<BrowserDescriptor> browserSafeList) {
        mBrowserSafeList = browserSafeList;
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(
            GenericAuthorizationRequest authorizationRequest,
            GenericOAuth2Strategy oAuth2Strategy)
            throws ClientException, UnsupportedEncodingException {
        final String methodName = ":requestAuthorization";
        checkNotDisposed();
        mOAuth2Strategy = oAuth2Strategy;
        mAuthorizationRequest = authorizationRequest;
        mAuthorizationResultFuture = new ResultFuture<>();
        final Browser browser = BrowserSelector.select(mReferencedActivity.get().getApplicationContext(), mBrowserSafeList);

        //ClientException will be thrown if no browser found.
        Intent authIntent;
        if (browser.isCustomTabsServiceSupported()) {
            Logger.info(
                    TAG + methodName,
                    "CustomTabsService is supported."
            );
            //create customTabsIntent
            mCustomTabManager = new CustomTabsManager(mReferencedActivity.get().getApplicationContext());
            mCustomTabManager.bind(browser.getPackageName());
            authIntent = mCustomTabManager.getCustomTabsIntent().intent;
        } else {
            Logger.warn(
                    TAG + methodName,
                    "CustomTabsService is NOT supported"
            );
            //create browser auth intent
            authIntent = new Intent(Intent.ACTION_VIEW);
        }

        authIntent.setPackage(browser.getPackageName());
        final Uri requestUrl = authorizationRequest.getAuthorizationRequestAsHttpRequest();
        authIntent.setData(requestUrl);

        final Intent intent = AuthorizationActivity.createStartIntent(
                mReferencedActivity.get().getApplicationContext(),
                authIntent,
                requestUrl.toString(),
                mAuthorizationRequest.getRedirectUri(),
                mAuthorizationRequest.getRequestHeaders(),
                AuthorizationAgent.BROWSER);
        // singleTask launchMode is required for the authorization redirect is from an external browser
        // in the browser authorization flow
        // For broker request we need to clear all activities in the task and bring Authorization Activity to the
        // top. If we do not add FLAG_ACTIVITY_CLEAR_TASK, Authorization Activity on finish can land on
        // Authenticator's or Company Portal's active activity which would be confusing to the user.
        if(mIsRequestFromBroker) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mReferencedActivity.get().startActivity(intent);

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
            dispose();
            final AuthorizationResult result = mOAuth2Strategy
                    .getAuthorizationResultFactory().createAuthorizationResult(
                            resultCode,
                            data,
                            mAuthorizationRequest
                    );
            mAuthorizationResultFuture.setResult(result);
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
        if (mCustomTabManager != null) {
            mCustomTabManager.unbind();
        }
        mDisposed = true;
    }
}