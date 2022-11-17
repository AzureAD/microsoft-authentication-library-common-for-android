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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

/**
 * Instantiates handlers for certificate based authentication.
 */
public class CertBasedAuthFactory {
    private static final String TAG = CertBasedAuthFactory.class.getSimpleName();
    private static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    private static final String ON_DEVICE_CHOICE = "on-device";
    private static final String SMARTCARD_CHOICE = "smartcard";
    private final Activity mActivity;
    private final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;

    /**
     * Creates an instance of CertBasedAuthFactory.
     * Instantiates relevant implementations of ISmartcardCertBasedAuthManagers.
     * @param activity current host activity.
     */
    public CertBasedAuthFactory(@NonNull final Activity activity) {
        final String methodTag = TAG + ":CertBasedAuthFactory";
        mActivity = activity;
        mSmartcardCertBasedAuthManager = SmartcardCertBasedAuthManagerFactory.createSmartcardCertBasedAuthManager(mActivity.getApplicationContext());
        mDialogHolder = new DialogHolder(mActivity);
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
     * Asynchronously chooses and returns a ICertBasedAuthChallengeHandler.
     * @param callback logic to run after a ICertBasedAuthChallengeHandler is chosen.
     */
    public void createCertBasedAuthChallengeHandler(@NonNull final CertBasedAuthChallengeHandlerCallback callback) {
        final CertBasedAuthTelemetryHelper telemetryHelper = new CertBasedAuthTelemetryHelper();
        telemetryHelper.setUserChoice(null);
        telemetryHelper.setCertBasedAuthChallengeHandler(null);
        if (mSmartcardCertBasedAuthManager == null) {
            //Smartcard CBA is not available, so default to on-device.
            callback.onReceived(new OnDeviceCertBasedAuthChallengeHandler(
                    mActivity,
                    telemetryHelper));
            return;
        }
        else if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
            telemetryHelper.setUserChoice(SMARTCARD_CHOICE);
            callback.onReceived(new SmartcardCertBasedAuthChallengeHandler(
                    mActivity,
                    mSmartcardCertBasedAuthManager,
                    mDialogHolder,
                    telemetryHelper,
                    false));
            return;
        }
        //Need input from user to determine which CertBasedAuthChallengeHandler to return.
        mDialogHolder.showUserChoiceDialog(new UserChoiceDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(final int checkedPosition) {
                //Position 0 -> On-device
                //Position 1 -> Smartcard
                if (checkedPosition == 0) {
                    mDialogHolder.dismissDialog();
                    telemetryHelper.setUserChoice(ON_DEVICE_CHOICE);
                    callback.onReceived(new OnDeviceCertBasedAuthChallengeHandler(
                            mActivity,
                            telemetryHelper));
                    return;
                }
                telemetryHelper.setUserChoice(SMARTCARD_CHOICE);
                setUpForSmartcardCertBasedAuth(callback, telemetryHelper);
            }
        }, new UserChoiceDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                onCancelHelper(callback, telemetryHelper);
            }
        });
    }

    /**
     * Helper method for logic to be run upon user cancelling out of CBA.
     * @param callback logic to run after a ICertBasedAuthChallengeHandler is chosen.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    private void onCancelHelper(@NonNull final  CertBasedAuthChallengeHandlerCallback callback,
                                @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        mDialogHolder.dismissDialog();
        telemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
        callback.onReceived(null);
    }

    /**
     * Handles user choice of smartcard CBA.
     * Proceeds with a certificate picker if a smartcard is already connected.
     * Otherwise, shows a dialog prompting user to connect a smartcard.
     * @param callback logic to run after a CertBasedAuthChallengeHandler is created.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    private void setUpForSmartcardCertBasedAuth(
            @NonNull final CertBasedAuthChallengeHandlerCallback callback,
            @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        //If smartcard is already plugged in, go straight to cert picker.
        if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
            callback.onReceived(new SmartcardCertBasedAuthChallengeHandler(
                    mActivity,
                    mSmartcardCertBasedAuthManager,
                    mDialogHolder,
                    telemetryHelper,
                    false));
            return;
        }

        if (mSmartcardCertBasedAuthManager.startNfcDiscovery(mActivity)) {
            //Inform user to turn on NFC if they want to use NFC.
            mDialogHolder.showSmartcardNfcReminderDialog(new SmartcardNfcReminderDialog.DismissCallback() {
                @Override
                public void onClick() {
                    //If smartcard is already plugged in, go straight to cert picker.
                    if (mSmartcardCertBasedAuthManager.isUsbDeviceConnected()) {
                        callback.onReceived(new SmartcardCertBasedAuthChallengeHandler(
                                mActivity,
                                mSmartcardCertBasedAuthManager,
                                mDialogHolder,
                                telemetryHelper,
                                false));
                        return;
                    }
                    showSmartcardPromptDialogAndSetConnectionCallback(callback, telemetryHelper);
                }
            });
            return;
        }
        showSmartcardPromptDialogAndSetConnectionCallback(callback, telemetryHelper);
    }

    /**
     * Helper method that shows smartcard prompt dialog and sets connection callback.
     * @param challengeHandlerCallback logic to run after a CertBasedAuthChallengeHandler is chosen.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    private void showSmartcardPromptDialogAndSetConnectionCallback(@NonNull final CertBasedAuthChallengeHandlerCallback challengeHandlerCallback,
                                                                   @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        mDialogHolder.showSmartcardPromptDialog(new SmartcardPromptDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                onCancelHelper(challengeHandlerCallback, telemetryHelper);
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
                challengeHandlerCallback.onReceived(new SmartcardCertBasedAuthChallengeHandler(
                        mActivity,
                        mSmartcardCertBasedAuthManager,
                        mDialogHolder,
                        telemetryHelper,
                        isNfc));
            }

            @Override
            public void onClosedConnection() {
                //ConnectionCallback will be changed before ever reaching this method.
            }
        });
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

    /**
     * Callback interface for createCertBasedAuthChallengeHandler.
     */
    public interface CertBasedAuthChallengeHandlerCallback {
        /**
         * Callback object that should contain logic to run after a CertBasedAuthChallengeHandler is chosen by the factory.
         * @param challengeHandler An ICertBasedAuthChallengeHandler implementation instance, or null if user cancels out of CBA.
         */
        void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler);
    }
}
