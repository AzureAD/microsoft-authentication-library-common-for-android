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

import android.os.Bundle;
import android.util.Log;
import android.webkit.ClientCertRequest;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.java.challengehandlers.IChallengeHandler;
import com.microsoft.identity.common.logging.Logger;

import java.util.concurrent.locks.ReentrantLock;

public class AuthorizationActivity extends DualScreenActivity {

    public static final String TAG = AuthorizationActivity.class.getSimpleName();

    private AuthorizationFragment mFragment;

    //Holds ChallengeHander that handles CBA.
    public static IChallengeHandler<ClientCertRequest, Void> sClientCertAuthChallengeHandler;
    public static final ReentrantLock sLock = new ReentrantLock();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String methodTag = TAG + ":onCreate";
        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());
        if (fragment instanceof AuthorizationFragment) {
            mFragment = (AuthorizationFragment) fragment;
            mFragment.setInstanceState(getIntent().getExtras());
            if (mFragment instanceof WebViewAuthorizationFragment) {
                sLock.lock();
                try {
                    ((WebViewAuthorizationFragment) mFragment).injectClientCertAuthChallengeHandler(sClientCertAuthChallengeHandler);
                } finally {
                    sLock.unlock();
                }
            }
        } else {
            final IllegalStateException ex = new IllegalStateException("Unexpected fragment type.");
            Logger.error(methodTag, "Did not receive AuthorizationFragment from factory", ex);
        }
        setFragment(mFragment);


    }

    @Override
    public void onBackPressed() {
        if (!mFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //If mChallengeHandler is of type BrokerClientCertAuthChallengeHandler, make sure to stop the YubiKitManager's Usb Discovery.
        //This method does nothing for all other ChallengeHandlers.
        sLock.lock();
        try {
            if (sClientCertAuthChallengeHandler != null) {
                sClientCertAuthChallengeHandler.stopYubiKitManagerUsbDiscovery();
            }
        } finally {
            sLock.unlock();
        }
    }
}
