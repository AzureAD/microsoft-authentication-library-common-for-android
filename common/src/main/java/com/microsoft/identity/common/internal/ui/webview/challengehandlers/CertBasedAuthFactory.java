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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;

/**
 * Instantiates handlers for certificate based authentication.
 */
public class CertBasedAuthFactory {
    private static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    private static final String ON_DEVICE_CHOICE = "on-device";
    private static final String SMARTCARD_CHOICE = "smartcard";
    private static final String NON_APPLICABLE = "N/A";
    private final Activity mActivity;
    private final AbstractUsbSmartcardCertBasedAuthManager mUsbSmartcardCertBasedAuthManager;
    private final AbstractNfcSmartcardCertBasedAuthManager mNfcSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;

    /**
     * Creates an instance of CertBasedAuthFactory.
     * Instantiates relevant implementations of ISmartcardCertBasedAuthManagers.
     * @param activity current host activity.
     */
    public CertBasedAuthFactory(@NonNull final Activity activity) {
        mActivity = activity;
        mUsbSmartcardCertBasedAuthManager = SmartcardCertBasedAuthManagerFactory.createUsbSmartcardCertBasedAuthManager(mActivity.getApplicationContext());
        mNfcSmartcardCertBasedAuthManager = SmartcardCertBasedAuthManagerFactory.createNfcSmartcardCertBasedAuthManager(mActivity.getApplicationContext());
        mDialogHolder = new DialogHolder(mActivity);
        if (mUsbSmartcardCertBasedAuthManager == null) {
            //This means that a user cannot use a smartcard via USB.
            //For consistency, we aren't going to allow the user to authenticate via smartcard CBA.
            //In the rare case a user is able to use NFC but not USB, this will require some different design work.
            return;
        }
        //Connection and disconnection callbacks for discovery are set in the SmartcardCertBasedAuthChallengeHandlers.
        mUsbSmartcardCertBasedAuthManager.startDiscovery(activity);
    }

    /**
     * Asynchronously chooses and returns a ICertBasedAuthChallengeHandler.
     * @param callback logic to run after a ICertBasedAuthChallengeHandler is chosen.
     */
    public void createCertBasedAuthChallengeHandler(@NonNull final CertBasedAuthChallengeHandlerCallback callback) {
        final CertBasedAuthTelemetryHelper telemetryHelper = new CertBasedAuthTelemetryHelper();
        telemetryHelper.setUserChoice(NON_APPLICABLE);
        telemetryHelper.setCertBasedAuthChallengeHandler(NON_APPLICABLE);
        if (mUsbSmartcardCertBasedAuthManager == null) {
            //Smartcard CBA is not available, so default to on-device.
            callback.onReceived(new OnDeviceCertBasedAuthChallengeHandler(
                    mActivity,
                    telemetryHelper));
            return;
        }

        if (mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
            telemetryHelper.setUserChoice(SMARTCARD_CHOICE);
            callback.onReceived(new UsbSmartcardCertBasedAuthChallengeHandler(
                    mActivity,
                    mUsbSmartcardCertBasedAuthManager,
                    mDialogHolder,
                    telemetryHelper));
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
    private void onCancelHelper(@NonNull final CertBasedAuthChallengeHandlerCallback callback,
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
    private void setUpForSmartcardCertBasedAuth(@NonNull final CertBasedAuthChallengeHandlerCallback callback,
                                                @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        //If smartcard is already plugged in, go straight to cert picker.
        if (mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
            callback.onReceived(new UsbSmartcardCertBasedAuthChallengeHandler(
                    mActivity,
                    mUsbSmartcardCertBasedAuthManager,
                    mDialogHolder,
                    telemetryHelper));
            return;
        }

        if (mNfcSmartcardCertBasedAuthManager != null
                && mNfcSmartcardCertBasedAuthManager.startDiscovery(mActivity)) {
            //Inform user to turn on NFC if they want to use NFC.
            mDialogHolder.showSmartcardNfcReminderDialog(new SmartcardNfcReminderDialog.DismissCallback() {
                @Override
                public void onClick() {
                    //If smartcard is already plugged in, go straight to cert picker.
                    if (mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
                        callback.onReceived(new UsbSmartcardCertBasedAuthChallengeHandler(
                                mActivity,
                                mUsbSmartcardCertBasedAuthManager,
                                mDialogHolder,
                                telemetryHelper));
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

        mUsbSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection() {
                if (mNfcSmartcardCertBasedAuthManager != null) {
                    mNfcSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
                }
                challengeHandlerCallback.onReceived(new UsbSmartcardCertBasedAuthChallengeHandler(
                        mActivity,
                        mUsbSmartcardCertBasedAuthManager,
                        mDialogHolder,
                        telemetryHelper));
            }

            @Override
            public void onClosedConnection() {
                //ConnectionCallback will be changed before ever reaching this method.
            }
        });

        if (mNfcSmartcardCertBasedAuthManager == null) {
            return;
        }
        mNfcSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection() {
                mUsbSmartcardCertBasedAuthManager.setConnectionCallback(null);
                mDialogHolder.showSmartcardNfcLoadingDialog();
                challengeHandlerCallback.onReceived(new NfcSmartcardCertBasedAuthChallengeHandler(
                        mActivity,
                        mNfcSmartcardCertBasedAuthManager,
                        mDialogHolder,
                        telemetryHelper));
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
        if (mUsbSmartcardCertBasedAuthManager != null) {
            mUsbSmartcardCertBasedAuthManager.onDestroy(mActivity);
        }
        if (mNfcSmartcardCertBasedAuthManager != null) {
            mNfcSmartcardCertBasedAuthManager.onDestroy(mActivity);
        }
    }

    /**
     * Callback interface for createCertBasedAuthChallengeHandler.
     */
    public interface CertBasedAuthChallengeHandlerCallback {
        /**
         * Callback object that should contain logic to run after a CertBasedAuthChallengeHandler is chosen by the factory.
         * @param challengeHandler An ICertBasedAuthChallengeHandler implementation instance, or null if user cancels out of CBA.
         */
        void onReceived(@Nullable final ICertBasedAuthChallengeHandler challengeHandler);
    }
}
