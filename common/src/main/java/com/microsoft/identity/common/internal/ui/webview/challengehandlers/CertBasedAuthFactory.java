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
 * Instantiates handlers for certificate based authentication.
 */
public class CertBasedAuthFactory {

    private static final String TAG = CertBasedAuthFactory.class.getSimpleName();
    private final Activity mActivity;
    private final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;

    /**
     * Creates an instance of CertBasedAuthFactory.
     * Instantiates relevant implementations of ISmartcardCertBasedAuthManagers.
     * @param activity current host activity.
     */
    public CertBasedAuthFactory(@NonNull final Activity activity) {
        final String methodTag = TAG + ":CertBasedAuthFactory";
        mActivity = activity;
        mSmartcardCertBasedAuthManager = SmartcardCertBasedAuthManagerFactory.createSmartcardCertBasedAuthManager(mActivity.getApplicationContext());
        if (mSmartcardCertBasedAuthManager == null) {
            return;
        }
        mSmartcardCertBasedAuthManager.setDiscoveryExceptionCallback(new AbstractSmartcardCertBasedAuthManager.IDiscoveryExceptionCallback() {
            @Override
            public void onException(@NonNull final Exception exception) {
                //Logging, but may also want to emit telemetry.
                //This method is not currently being called, but it could be
                // used in future SmartcardCertBasedAuthManager implementations.
                Logger.error(methodTag, "Exception thrown upon starting smartcard usb discovery: " + exception.getMessage(), exception);
            }
        });
        //Connection and disconnection callbacks for discovery are set in the SmartcardCertBasedAuthChallengeHandlers.
        mSmartcardCertBasedAuthManager.startUsbDiscovery();
    }

    /**
     * Creates and returns an applicable instance of ICertBasedAuthChallengeHandler.
     * @return An ICertBasedAuthChallengeHandler implementation instance.
     */
    @NonNull
    public ICertBasedAuthChallengeHandler createCertBasedAuthChallengeHandler() {
        if (mSmartcardCertBasedAuthManager == null) {
            //Smartcard CBA is not available, so default to on-device.
            return new OnDeviceCertBasedAuthChallengeHandler(mActivity);
        }
        else if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
            return new SmartcardCertBasedAuthChallengeHandler(mActivity, mSmartcardCertBasedAuthManager, new DialogHolder(mActivity), false);
        }
        return new UserChoiceCertBasedAuthChallengeHandler(mActivity, mSmartcardCertBasedAuthManager, new DialogHolder(mActivity));
    }

    /**
     * Cleanup to be done when host activity is being destroyed.
     */
    public void onDestroy() {
        if (mSmartcardCertBasedAuthManager == null) {
            return;
        }
        mSmartcardCertBasedAuthManager.onDestroy();
    }
}
