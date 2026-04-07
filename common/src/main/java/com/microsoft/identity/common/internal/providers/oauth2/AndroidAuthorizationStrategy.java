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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;

import java.lang.ref.WeakReference;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Android's {@link IAuthorizationStrategy} implementation.
 */
// Suppressing rawtype warnings due to the generic types OAuth2Strategy and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class AndroidAuthorizationStrategy<
        GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest>
        implements IAuthorizationStrategy<GenericOAuth2Strategy, GenericAuthorizationRequest> {

    private static final String TAG = AndroidAuthorizationStrategy.class.getSimpleName();

    private final WeakReference<Context> mReferencedApplicationContext;
    private final WeakReference<Activity> mReferencedActivity;
    private final WeakReference<Fragment> mReferencedFragment;

    /**
     * Constructor of AndroidAuthorizationStrategy.
     */
    public AndroidAuthorizationStrategy(@NonNull Context applicationContext,
                                        @NonNull Activity activity,
                                        @Nullable Fragment fragment) {
        mReferencedApplicationContext = new WeakReference<>(applicationContext);
        mReferencedActivity = new WeakReference<>(activity);
        mReferencedFragment = new WeakReference<>(fragment);
    }

    protected Context getApplicationContext() {
        return mReferencedApplicationContext.get();
    }

    /**
     * If fragment is provided, add AuthorizationFragment on top of that fragment.
     * Otherwise, launch AuthorizationActivity.
     * <p>
     * For browser-based flows (non-WebView), validates that no other application is registered for
     * the same custom URL scheme before starting the authorization UI. If another app is found, a
     * {@link ClientException} with error code
     * {@link com.microsoft.identity.common.java.exception.ErrorStrings#MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME}
     * is thrown so that it propagates correctly through the command pipeline to MSAL's adapter layer.
     * WebView flows are intentionally excluded from this check.
     */
    protected void launchIntent(@NonNull Intent intent) throws ClientException {
        // Perform the multiple-app URL scheme validation for non-WebView flows.
        // This is done here (rather than in the factory) so that the ClientException always
        // propagates through this method's declared throws clause, regardless of whether we're
        // using the fragment-embedded or full-Activity path.
        // A null authorizationAgent is treated the same as BROWSER (default browser flow).
        final AuthorizationAgent authorizationAgent =
                (AuthorizationAgent) intent.getSerializableExtra(
                        AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT);
        if (authorizationAgent != AuthorizationAgent.WEBVIEW) {
            final Context appContext = getApplicationContext();
            if (appContext == null) {
                Logger.warn(TAG + ":launchIntent",
                        "Application context is null; skipping multiple-app URL scheme validation.");
            } else {
                final String redirectUri = intent.getStringExtra(
                        AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI);
                if (redirectUri == null) {
                    Logger.warn(TAG + ":launchIntent",
                            "Redirect URI is null in the intent; skipping multiple-app URL scheme validation.");
                } else {
                    BrowserRedirectValidator.validateNoMultipleAppsListening(
                            appContext,
                            redirectUri,
                            LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()
                    );
                }
            }
        }

        final Fragment fragment = mReferencedFragment.get();

        if (fragment != null) {
            final Fragment authFragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntentWithState(intent, intent.getExtras());

            final FragmentManager fragmentManager = fragment.getFragmentManager();
            if (fragmentManager == null) {
                throw new ClientException(ClientException.NULL_OBJECT, "Fragment Manager is null");
            }

            fragmentManager.beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .add(fragment.getId(), authFragment, Fragment.class.getName())
                    .commit();
            return;
        }

        final Activity activity = mReferencedActivity.get();
        if (activity == null) {
            throw new ClientException(ClientException.NULL_OBJECT, "Referenced activity is null");
        }
        activity.startActivity(intent);
    }
}
