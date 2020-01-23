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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.exception.ClientException;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

/**
 * Abstracts the behavior associated with gathering a user authorization for an access token (oAuth)
 * and/or authentication information (OIDC)
 * Possible implementations include: EmbeddedWebViewAuthorizationStrategy, SystemWebViewAuthorizationStrategy, Device Code, etc...
 */
public abstract class AuthorizationStrategy<GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest> {
    private WeakReference<Context> mReferencedApplicationContext;
    private WeakReference<Activity> mReferencedActivity;
    private WeakReference<Fragment> mReferencedFragment;

    /**
     * Constructor of AuthorizationStrategy.
     */
    public AuthorizationStrategy(@NonNull Context applicationContext,
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
     */
    protected void launchIntent(@NonNull Intent intent) {
        final Fragment fragment = mReferencedFragment.get();

        if (fragment != null) {
            final AuthorizationFragment authFragment = AuthorizationActivity.getAuthorizationFragmentFromStartIntent(intent);

            fragment.getFragmentManager()
                    .beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .add(fragment.getId(), authFragment, AuthorizationFragment.class.getName())
                    .commit();
            return;
        }

        mReferencedActivity.get().startActivity(intent);
    }

    /**
     * Perform the authorization request.
     */
    public abstract Future<AuthorizationResult> requestAuthorization(GenericAuthorizationRequest authorizationRequest,
                                                                     GenericOAuth2Strategy oAuth2Strategy)
            throws ClientException, UnsupportedEncodingException;

    public abstract void completeAuthorization(int requestCode, int resultCode, final Intent data);
}
