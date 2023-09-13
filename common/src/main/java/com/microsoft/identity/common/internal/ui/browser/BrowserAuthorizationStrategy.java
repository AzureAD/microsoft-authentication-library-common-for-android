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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.providers.oauth2.AndroidAuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivityFactory;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

// Suppressing rawtype warnings due to the generic types OAuth2Strategy, AuthorizationRequest and AuthorizationResult
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class BrowserAuthorizationStrategy<
        GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest>
        extends AndroidAuthorizationStrategy<GenericOAuth2Strategy, GenericAuthorizationRequest> {

    private final static String TAG = BrowserAuthorizationStrategy.class.getSimpleName();

    private CustomTabsManager mCustomTabManager;
    private ResultFuture<AuthorizationResult> mAuthorizationResultFuture;
    private Browser mBrowser;
    private boolean mDisposed;
    private GenericOAuth2Strategy mOAuth2Strategy; //NOPMD
    private GenericAuthorizationRequest mAuthorizationRequest; //NOPMD

    public BrowserAuthorizationStrategy(@NonNull Context applicationContext,
                                        @NonNull Activity activity,
                                        @Nullable Fragment fragment) {
        super(applicationContext, activity, fragment);
    }

    public void setBrowser(final Browser browser) {
        mBrowser = browser;
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(
            GenericAuthorizationRequest authorizationRequest,
            GenericOAuth2Strategy oAuth2Strategy)
            throws ClientException {
        final String methodTag = TAG + ":requestAuthorization";
        checkNotDisposed();
        final Context context = getApplicationContext();
        mOAuth2Strategy = oAuth2Strategy;
        mAuthorizationRequest = authorizationRequest;
        mAuthorizationResultFuture = new ResultFuture<>();

        //ClientException will be thrown if no browser found.
        final Intent[] authIntent = new Intent[1];
        if (mBrowser.isCustomTabsServiceSupported()) {
            Logger.info(
                    methodTag,
                    "CustomTabsService is supported."
            );
            //create customTabsIntent
            mCustomTabManager = new CustomTabsManager(context);
            if (!mCustomTabManager.bind(context, mBrowser.getPackageName())) {
                //create browser auth intent
                authIntent[0] = new Intent(Intent.ACTION_VIEW);
            } else {
                final Bundle headersBundle = new Bundle();

                final Map<String, String> headers = authorizationRequest.getRequestHeaders();

                for (final String headerName : headers.keySet()) {
                    headersBundle.putString(headerName, headers.get(headerName));
                }

                final CustomTabsSession customTabsSession = mCustomTabManager.getClient().newSession(
                        new CustomTabsCallback() {
                            @Override
                            public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin,
                                                                       boolean result, @Nullable Bundle extras) {
                                try {
                                    // Launch custom tabs intent after session was validated as the same origin.
                                    authIntent[0] = mCustomTabManager
                                            .getCustomTabsIntent()
                                            .intent.putExtra(
                                                    android.provider.Browser.EXTRA_HEADERS,
                                                    headersBundle
                                            );

                                    authIntent[0].setPackage(mBrowser.getPackageName());
                                    final URI requestUrl = authorizationRequest.getAuthorizationRequestAsHttpRequest();

                                    authIntent[0].setData(Uri.parse(requestUrl.toString()));

                                    final Intent intent = buildAuthorizationActivityStartIntent(authIntent[0], requestUrl);
                                    launchIntent(intent);
                                } catch (ClientException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                );

                final URI requestUrl = authorizationRequest.getAuthorizationRequestAsHttpRequest();

                // Validate the session as the same origin to allow cross origin headers.
                customTabsSession.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN,
                        Uri.parse(requestUrl.toString()), null);
            }
        } else {
            Logger.warn(
                    methodTag,
                    "CustomTabsService is NOT supported"
            );
            //create browser auth intent
            authIntent[0] = new Intent(Intent.ACTION_VIEW);
        }

//        authIntent[0].setPackage(mBrowser.getPackageName());
//        final URI requestUrl = authorizationRequest.getAuthorizationRequestAsHttpRequest();
//
//        authIntent[0].setData(Uri.parse(requestUrl.toString()));
//
//        final Intent intent = buildAuthorizationActivityStartIntent(authIntent[0], requestUrl);
//        launchIntent(intent);

        return mAuthorizationResultFuture;
    }

    // Suppressing unchecked warnings during casting to HashMap<String,String> due to no generic type with mAuthorizationRequest
    @SuppressWarnings(WarningType.unchecked_warning)
    private Intent buildAuthorizationActivityStartIntent(Intent authIntent, URI requestUrl) {
        final Intent intent = AuthorizationActivityFactory.getAuthorizationActivityIntent(
                getApplicationContext(),
                authIntent,
                requestUrl.toString(),
                mAuthorizationRequest.getRedirectUri(),
                mAuthorizationRequest.getRequestHeaders(),
                AuthorizationAgent.BROWSER,
                true,
                true);
        setIntentFlag(intent);
        return intent;
    }

    protected abstract void setIntentFlag(@NonNull final Intent intent);

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }


    @Override
    public void completeAuthorization(int requestCode, @NonNull final RawAuthorizationResult data) {
        final String methodTag = TAG + ":completeAuthorization";
        if (requestCode == BROWSER_FLOW) {
            dispose();

            //Suppressing unchecked warnings due to method createAuthorizationResult being a member of the raw type AuthorizationResultFactory
            @SuppressWarnings(WarningType.unchecked_warning) final AuthorizationResult result = mOAuth2Strategy
                    .getAuthorizationResultFactory().createAuthorizationResult(
                            data,
                            mAuthorizationRequest
                    );
            mAuthorizationResultFuture.setResult(result);
        } else {
            Logger.warnPII(methodTag, "Unknown request code " + requestCode);
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