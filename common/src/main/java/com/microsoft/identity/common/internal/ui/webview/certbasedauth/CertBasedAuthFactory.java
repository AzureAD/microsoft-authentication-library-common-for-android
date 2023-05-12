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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;
import android.os.Build;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthChoice;
import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

/**
 * Instantiates handlers for certificate based authentication.
 */
public class CertBasedAuthFactory {
    private static final String TAG = CertBasedAuthFactory.class.getSimpleName();
    private static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    private static final String NON_APPLICABLE = "N/A";
    private final Activity mActivity;
    private final AbstractUsbSmartcardCertBasedAuthManager mUsbSmartcardCertBasedAuthManager;
    private final AbstractNfcSmartcardCertBasedAuthManager mNfcSmartcardCertBasedAuthManager;
    private final IDialogHolder mDialogHolder;
    private boolean wasCertBasedAuthInitiated;

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
        wasCertBasedAuthInitiated = false;
        if (mUsbSmartcardCertBasedAuthManager != null) {
            //Connection and disconnection callbacks for discovery are set in the SmartcardCertBasedAuthChallengeHandlers.
            mUsbSmartcardCertBasedAuthManager.startDiscovery(activity);
        }
    }

    /**
     * Creates an instance of CertBasedAuthFactory for testing purposes.
     * @param activity host activity.
     * @param usbManager manager for usb connections.
     * @param nfcManager manager for nfc connections.
     * @param dialogHolder IDialogHolder instance.
     */
    @VisibleForTesting
    protected CertBasedAuthFactory(@NonNull final Activity activity,
                                   @NonNull final AbstractUsbSmartcardCertBasedAuthManager usbManager,
                                   @NonNull final AbstractNfcSmartcardCertBasedAuthManager nfcManager,
                                   @NonNull final IDialogHolder dialogHolder) {
        mActivity = activity;
        mUsbSmartcardCertBasedAuthManager = usbManager;
        mNfcSmartcardCertBasedAuthManager = nfcManager;
        mDialogHolder = dialogHolder;
        wasCertBasedAuthInitiated = false;
    }

    /**
     * Asynchronously chooses and returns an AbstractCertBasedAuthChallengeHandler.
     * @param callback logic to run after an AbstractCertBasedAuthChallengeHandler is chosen.
     */
    public void createCertBasedAuthChallengeHandler(@NonNull final CertBasedAuthChallengeHandlerCallback callback) {
        final ICertBasedAuthTelemetryHelper telemetryHelper = new CertBasedAuthTelemetryHelper();
        telemetryHelper.setUserChoice(CertBasedAuthChoice.NON_APPLICABLE);
        telemetryHelper.setCertBasedAuthChallengeHandler(NON_APPLICABLE);
        telemetryHelper.setPublicKeyAlgoType(NON_APPLICABLE);
        wasCertBasedAuthInitiated = true;

        if (mUsbSmartcardCertBasedAuthManager != null
                && mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
            telemetryHelper.setUserChoice(CertBasedAuthChoice.SMARTCARD_CHOICE);
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
                    telemetryHelper.setUserChoice(CertBasedAuthChoice.ON_DEVICE_CHOICE);
                    callback.onReceived(new OnDeviceCertBasedAuthChallengeHandler(
                            mActivity,
                            telemetryHelper));
                    return;
                }
                telemetryHelper.setUserChoice(CertBasedAuthChoice.SMARTCARD_CHOICE);
                setUpForSmartcardCertBasedAuth(callback, telemetryHelper);
            }
        }, new ICancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                onCancelHelper(callback, telemetryHelper);
            }
        });
    }

    /**
     * Helper method for logic to be run upon user cancelling out of CBA.
     * @param callback logic to run after an AbstractCertBasedAuthChallengeHandler is chosen.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    private void onCancelHelper(@NonNull final CertBasedAuthChallengeHandlerCallback callback,
                                @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        mDialogHolder.dismissDialog();
        telemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
        mNfcSmartcardCertBasedAuthManager.clearConnectionCallback();
        mUsbSmartcardCertBasedAuthManager.clearConnectionCallback();
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
                                                @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        //If smartcard is already plugged in, go straight to cert picker.
        if (mUsbSmartcardCertBasedAuthManager != null
                && mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
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
            mDialogHolder.showSmartcardNfcReminderDialog(new IDismissCallback() {
                @Override
                public void onDismiss() {
                    //If smartcard is already plugged in, go straight to cert picker.
                    if (mUsbSmartcardCertBasedAuthManager != null
                            && mUsbSmartcardCertBasedAuthManager.isDeviceConnected()) {
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
                                                                   @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        mDialogHolder.showSmartcardPromptDialog(new ICancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                if (mNfcSmartcardCertBasedAuthManager != null) {
                    mNfcSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
                }
                onCancelHelper(challengeHandlerCallback, telemetryHelper);
            }
        });

        if (mUsbSmartcardCertBasedAuthManager != null) {
            mUsbSmartcardCertBasedAuthManager.setConnectionCallback(new IConnectionCallback() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onCreateConnection() {
                    if (mNfcSmartcardCertBasedAuthManager != null) {
                        mNfcSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
                        clearAllSmartcardConnectionAndDisconnectionCallbacks();
                    }
                    challengeHandlerCallback.onReceived(new UsbSmartcardCertBasedAuthChallengeHandler(
                            mActivity,
                            mUsbSmartcardCertBasedAuthManager,
                            mDialogHolder,
                            telemetryHelper));
                }
            });
        }

        if (mNfcSmartcardCertBasedAuthManager == null) {
            return;
        }
        mNfcSmartcardCertBasedAuthManager.setConnectionCallback(new IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection() {
                if (mUsbSmartcardCertBasedAuthManager != null) {
                    clearAllSmartcardConnectionAndDisconnectionCallbacks();
                }
                mDialogHolder.showSmartcardNfcLoadingDialog();
                challengeHandlerCallback.onReceived(new NfcSmartcardCertBasedAuthChallengeHandler(
                        mActivity,
                        mNfcSmartcardCertBasedAuthManager,
                        mDialogHolder,
                        telemetryHelper));
            }
        });
    }

    /**
     * Clears all connection and disconnection callbacks for smartcard managers.
     */
    public void clearAllSmartcardConnectionAndDisconnectionCallbacks() {
        mUsbSmartcardCertBasedAuthManager.clearConnectionCallback();
        mUsbSmartcardCertBasedAuthManager.clearDisconnectionCallback();
        mNfcSmartcardCertBasedAuthManager.clearConnectionCallback();
    }

    /**
     * Cleanup to be done when host activity is being destroyed.
     */
    public void onDestroy() {
        final String methodTag = TAG + ":onDestroy";

        if (mUsbSmartcardCertBasedAuthManager != null) {
            mUsbSmartcardCertBasedAuthManager.onDestroy(mActivity);
        }
        if (mNfcSmartcardCertBasedAuthManager != null) {
            mNfcSmartcardCertBasedAuthManager.onDestroy(mActivity);
        }
        if (wasCertBasedAuthInitiated) {
            //For CBA, we need to clear the certificate choice cache here so that
            // the user will be able to login with multiple accounts with CBA
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                WebView.clearClientCertPreferences(null);
            } else {
                Logger.warn(methodTag, "Client Cert Preferences cache not cleared due to SDK version < 21 (LOLLIPOP). " +
                        "Subsequent CBA attempts will fail due to the cached action, so the user must restart the app before attempting to login with CBA again.");
            }
        }
    }

    /**
     * Callback interface for createCertBasedAuthChallengeHandler.
     */
    public interface CertBasedAuthChallengeHandlerCallback {
        /**
         * Callback object that should contain logic to run after a CertBasedAuthChallengeHandler is chosen by the factory.
         * @param challengeHandler An AbstractCertBasedAuthChallengeHandler implementation instance, or null if user cancels out of CBA.
         */
        void onReceived(@Nullable final AbstractCertBasedAuthChallengeHandler challengeHandler);
    }
}
