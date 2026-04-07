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
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;

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
     * For browser-based flows (non-WebView), this method validates that no other application is
     * registered for the same custom URL scheme before starting the authorization UI. If another
     * app is found, a {@link ClientException} with error code
     * {@link com.microsoft.identity.common.java.exception.ErrorStrings#MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME}
     * is thrown so that it propagates correctly through the command pipeline.
     */
    protected void launchIntent(@NonNull Intent intent) throws ClientException {
        final Fragment fragment = mReferencedFragment.get();

        if (fragment != null) {
            // Fragment path: validation is performed inside the factory (context is passed).
            final Fragment authFragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntentWithState(intent, intent.getExtras(), getApplicationContext());

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

        // Activity path: validate here, before starting the Activity, so that any ClientException
        // can propagate through this method's declared throws clause and reach the command pipeline.
        final Context appContext = getApplicationContext();
        if (appContext != null) {
            final String redirectUri = intent.getStringExtra(
                    AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI);
            if (redirectUri != null) {
                BrowserRedirectValidator.validateNoMultipleAppsListening(
                        appContext,
                        redirectUri,
                        LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()
                );
            }
        }

        final Activity activity = mReferencedActivity.get();
        if (activity == null) {
            throw new ClientException(ClientException.NULL_OBJECT, "Referenced activity is null");
        }
        activity.startActivity(intent);
    }
}
