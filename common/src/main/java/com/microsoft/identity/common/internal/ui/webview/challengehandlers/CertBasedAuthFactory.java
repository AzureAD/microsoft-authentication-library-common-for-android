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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.logging.Logger;

/**
 * Instantiates handlers and managers for certificate based authentication.
 */
public class CertBasedAuthFactory {

    private static final String TAG = CertBasedAuthFactory.class.getSimpleName();
    private Activity mActivity;
    private AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;

    /**
     * Creates an instance of CertBasedAuthFactory.
     * Instantiates relevant implementations of ISmartcardCertBasedAuthManagers.
     * @param activity current host activity.
     */
    public CertBasedAuthFactory(@NonNull final Activity activity) {
        initialize(activity, new YubiKitCertBasedAuthManager(mActivity.getApplicationContext()));
    }

    /**
     * Creates an instance of CertBasedAuthFactory.
     * Allows injection of AbstractSmartcardCertBasedAuthManager for testing purposes.
     * @param activity current host activity.
     * @param manager AbstractSmartcardCertBasedAuthManager to be used for smartcard CBA.
     */
    protected CertBasedAuthFactory(@NonNull final Activity activity,
                                   @NonNull final AbstractSmartcardCertBasedAuthManager manager) {
        initialize(activity, manager);
    }

    /**
     * Sets class variables and starts smartcard usb discovery.
     * @param activity current host activity.
     * @param manager AbstractSmartcardCertBasedAuthManager to be used for smartcard CBA.
     */
    private void initialize(@NonNull final Activity activity,
                       @NonNull final AbstractSmartcardCertBasedAuthManager manager) {
        final String methodTag = TAG + ":initialize";
        mActivity = activity;
        mSmartcardCertBasedAuthManager = manager;
        mSmartcardCertBasedAuthManager.setDiscoveryExceptionCallback(new AbstractSmartcardCertBasedAuthManager.IDiscoveryExceptionCallback() {
            @Override
            public void onException() {
                //Logging, but may also want to emit telemetry.
                //This method is not currently being called, but it could be
                // used in future SmartcardCertBasedAuthManager implementations.
                Logger.error(methodTag, "Unable to start smartcard usb discovery.", null);
            }
        });
        //Connection and disconnection callbacks for discovery are set in the SmartcardCertBasedAuthChallengeHandlers.
        mSmartcardCertBasedAuthManager.startDiscovery();
    }

    /**
     * Creates and returns an applicable instance of ICertBasedAuthChallengeHandler.
     * @return An ICertBasedAuthChallengeHandler implementation instance.
     */
    @NonNull
    public ICertBasedAuthChallengeHandler createCertBasedAuthChallengeHandler() {
        if (mSmartcardCertBasedAuthManager.isDeviceConnected()) {
            return new SmartcardCertBasedAuthChallengeHandler(mSmartcardCertBasedAuthManager, new DialogHolder(mActivity));
        } else {
            return new OnDeviceCertBasedAuthChallengeHandler(mActivity);
        }
    }

    /**
     * Cleanup to be done when host activity is being destroyed.
     */
    public void onDestroy() {
        mSmartcardCertBasedAuthManager.onDestroy();
    }
}
