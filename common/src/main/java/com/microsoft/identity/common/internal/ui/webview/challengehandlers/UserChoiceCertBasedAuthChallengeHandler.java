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
import android.os.Build;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

public class UserChoiceCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    private static final String TAG = UserChoiceCertBasedAuthChallengeHandler.class.getSimpleName();
    private final Activity mActivity;
    protected final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;

    public UserChoiceCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                   @NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                   @NonNull final DialogHolder dialogHolder) {
        mActivity = activity;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
        mDialogHolder = dialogHolder;
    }


    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     *
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull RawAuthorizationResult response) {

    }

    /**
     * Clean up logic to run when ICertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {

    }

    /**
     * Process difference kinds of challenge request.
     *
     * @param request challenge request
     * @return GenericResponse
     */
    @Override
    public Void processChallenge(ClientCertRequest request) {
        mDialogHolder.showSmartcardPromptDialog(new SmartcardPromptDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                mDialogHolder.dismissDialog();
                request.cancel();
            }
        });
        /*
        new SmartcardPromptDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick() {
                mSmartcardCertBasedAuthManager.stopNfcDiscovery(mActivity);
                new OnDeviceCertBasedAuthChallengeHandler(mActivity).processChallenge(request);
            }
        }
         */
        mSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection(final boolean isNfc) {
                if (isNfc) {
                    //Show loading
                    mDialogHolder.showSmartcardNfcLoadingDialog();
                }
                new SmartcardCertBasedAuthChallengeHandler(mActivity, mSmartcardCertBasedAuthManager, mDialogHolder, isNfc).processChallenge(request);
            }

            @Override
            public void onClosedConnection() {

            }
        });
        mSmartcardCertBasedAuthManager.startNfcDiscovery(mActivity);
        return null;
    }
}
