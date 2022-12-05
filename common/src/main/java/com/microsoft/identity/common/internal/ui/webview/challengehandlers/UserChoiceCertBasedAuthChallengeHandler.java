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

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

/**
 * A CertBasedAuthChallengeHandler implementation that routes a received ClientCertRequest
 *  to the challenge handlers for on-device or smartcard CBA based on user input.
 */
public class UserChoiceCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    private static final String TAG = UserChoiceCertBasedAuthChallengeHandler.class.getSimpleName();
    private static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    private final Activity mActivity;
    protected final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;
    private final CertBasedAuthTelemetryHelper mTelemetryHelper;
    private ICertBasedAuthChallengeHandler mCertBasedAuthChallengeHandler;

    /**
     * Creates a new instance of UserChoiceCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param smartcardCertBasedAuthManager AbstractSmartcardCertBasedAuthManager instance.
     * @param dialogHolder DialogHolder instance.
     * @param telemetryHelper CertBasedAuthTelemetryHelder instance.
     */
    public UserChoiceCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                   @NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                   @NonNull final DialogHolder dialogHolder,
                                                   @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        mActivity = activity;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
        mDialogHolder = dialogHolder;
        mTelemetryHelper = telemetryHelper;
        mTelemetryHelper.setCertBasedAuthChallengeHandler(TAG);
    }


    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     *
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull RawAuthorizationResult response) {
        if (mCertBasedAuthChallengeHandler != null) {
            mCertBasedAuthChallengeHandler.emitTelemetryForCertBasedAuthResults(response);
        }
    }

    /**
     * Clean up logic to run when ICertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {
        if (mCertBasedAuthChallengeHandler != null) {
            mCertBasedAuthChallengeHandler.cleanUp();
            return;
        }
        mDialogHolder.dismissDialog();
        //Reset IConnectionCallback local variable of mSmartcardCertBasedAuthManager.
        mSmartcardCertBasedAuthManager.setConnectionCallback(null);
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Prompts user to choose between on-device and smartcard CBA.
     * @param request challenge request
     * @return GenericResponse
     */
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        mDialogHolder.showUserChoiceDialog(new UserChoiceDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(final int checkedPosition) {
                //Position 0 -> On-device
                //Position 1 -> Smartcard
                if (checkedPosition == 0) {
                    mDialogHolder.dismissDialog();
                    mCertBasedAuthChallengeHandler = new OnDeviceCertBasedAuthChallengeHandler(mActivity, mTelemetryHelper);
                    mCertBasedAuthChallengeHandler.processChallenge(request);
                } else {
                    setUpForSmartcardCertBasedAuth(request);
                }
            }
        }, new UserChoiceDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                mDialogHolder.dismissDialog();
                mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                request.cancel();
            }
        });
        return null;
    }

    /**
     * Handles user choice of smartcard CBA.
     * Proceeds with a certificate picker if a smartcard is already connected.
     * Otherwise, shows a dialog prompting user to connect a smartcard.
     * @param request challenge request
     */
    private void setUpForSmartcardCertBasedAuth(@NonNull final ClientCertRequest request) {
        //If smartcard is already plugged in, go straight to cert picker.
        if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
            createSmartcardChallengeHandlerAndProcess(request, false);
            return;
        }

        if (mSmartcardCertBasedAuthManager.startNfcDiscovery(mActivity)) {
            //Inform user to turn on NFC if they want to use NFC.
            mDialogHolder.showSmartcardNfcReminderDialog(new SmartcardNfcReminderDialog.DismissCallback() {
                @Override
                public void onClick() {
                    //If smartcard is already plugged in, go straight to cert picker.
                    if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
                        createSmartcardChallengeHandlerAndProcess(request, false);
                        return;
                    }
                    showSmartcardPromptDialogAndSetCallback(request);
                }
            });
            return;
        }
        showSmartcardPromptDialogAndSetCallback(request);
    }

    /**
     * Helper method that shows smartcard prompt dialog and sets connection callback.
     * @param request challenge request
     */
    private void showSmartcardPromptDialogAndSetCallback(@NonNull final ClientCertRequest request) {
        mDialogHolder.showSmartcardPromptDialog(new SmartcardPromptDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                mDialogHolder.dismissDialog();
                mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                request.cancel();
            }
        });
        mSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection(final boolean isNfc) {
                if (isNfc) {
                    //User needs to keep smartcard in place.
                    mDialogHolder.showSmartcardNfcLoadingDialog();
                }
                createSmartcardChallengeHandlerAndProcess(request, isNfc);
            }

            @Override
            public void onClosedConnection() {
                //ConnectionCallback will be changed before ever reaching this method.
            }
        });
    }

    /**
     * Helper method that creates and sets a SmartcardCertBasedAuthChallengeHandler.
     * Afterwards, the challenge handler proceeds with the ClientCertRequest.
     * @param request challenge request
     * @param isNfc true if connection is NFC, false if usb.
     */
    private void createSmartcardChallengeHandlerAndProcess(@NonNull final ClientCertRequest request, final boolean isNfc) {
        mCertBasedAuthChallengeHandler = new SmartcardCertBasedAuthChallengeHandler(
                mActivity,
                mSmartcardCertBasedAuthManager,
                mDialogHolder,
                mTelemetryHelper,
                isNfc);
        mCertBasedAuthChallengeHandler.processChallenge(request);
    }
}
