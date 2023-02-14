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
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.internal.ui.webview.FinalizeResultCallback;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;

/**
 * Handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on a smartcard device connected via NFC.
 */
public class NfcSmartcardCertBasedAuthChallengeHandler extends AbstractSmartcardCertBasedAuthChallengeHandler<AbstractNfcSmartcardCertBasedAuthManager> {
    /**
     * Creates new instance of NfcSmartcardCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param nfcSmartcardCertBasedAuthManager AbstractNfcSmartcardCertBasedAuthManager instance.
     * @param dialogHolder DialogHolder instance.
     * @param telemetryHelper CertBasedAuthTelemetryHelder instance.
     */
    public NfcSmartcardCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                     @NonNull final AbstractNfcSmartcardCertBasedAuthManager nfcSmartcardCertBasedAuthManager,
                                                     @NonNull final IDialogHolder dialogHolder,
                                                     @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        super(activity, nfcSmartcardCertBasedAuthManager, dialogHolder, telemetryHelper, NfcSmartcardCertBasedAuthChallengeHandler.class.getSimpleName());
    }

    /**
     * Pauses smartcard discovery, if the particular authentication method isn't meant to have
     *  discovery active throughout the entire flow.
     */
    @Override
    protected void pauseForRemoval(@NonNull final SmartcardRemovalPromptDialog.RemovalCallback callback) {
        //Helps prevent unnecessary callback trigger. Nfc discovery should only be active when
        // the user is prompted to tap.
        mDialogHolder.showSmartcardRemovalPromptDialog(callback);
        mCbaManager.disconnect(new AbstractSmartcardCertBasedAuthManager.IDisconnectCallback() {
            @Override
            public void onDisconnect() {
                mDialogHolder.onSmartcardRemoval();
                mCbaManager.stopDiscovery(mActivity);
            }
        });
    }

    /**
     * Upon a positive button click in the smartcard PIN dialog, verify the provided PIN and handle the results.
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardPinDialog.
     */
    @Override
    protected SmartcardPinDialog.PositiveButtonListener getSmartcardPinDialogPositiveButtonListener(@NonNull final ICertDetails certDetails,
                                                                                                    @NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":getSmartcardPinDialogPositiveButtonListener";

        return new SmartcardPinDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(@NonNull final char[] pin) {
                //For NFC, we need another dialog prompting the user to hold the smartcard to the phone again.
                mDialogHolder.showSmartcardNfcPromptDialog(new SmartcardNfcPromptDialog.CancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        mDialogHolder.dismissDialog();
                        mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                        mCbaManager.stopDiscovery(mActivity);
                        request.cancel();
                    }
                });
                mCbaManager.setConnectionCallback(new IConnectionCallback() {
                    @Override
                    public void onCreateConnection() {
                        mDialogHolder.showSmartcardNfcLoadingDialog();
                        if (mCbaManager.isDeviceChanged()) {
                            clearPin(pin);
                            request.cancel();
                            pauseForRemoval(new SmartcardRemovalPromptDialog.RemovalCallback() {
                                @Override
                                public void onRemoved() {
                                    //In a future version, an error dialog with a custom message could be shown here instead of a general error.
                                    indicateGeneralException(methodTag, new Exception("Device connected via NFC is different from initially connected device."));
                                }
                            });
                            return;
                        }
                        mCbaManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
                            @Override
                            public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                                tryUsingSmartcardWithPin(pin, certDetails, request, session);
                                clearPin(pin);
                            }

                            @Override
                            public void onException(@NonNull final Exception e) {
                                clearPin(pin);
                                request.cancel();
                                pauseForRemoval(new SmartcardRemovalPromptDialog.RemovalCallback() {
                                    @Override
                                    public void onRemoved() {
                                        indicateGeneralException(methodTag, e);
                                    }
                                });
                            }
                        });
                    }
                });
                mCbaManager.startDiscovery(mActivity);
            }
        };
    }

    /**
     * Shows PIN dialog, if not already showing, and sets dialog to error mode.
     *
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request     ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void setPinDialogForIncorrectAttempt(@NonNull ICertDetails certDetails,
                                                   @NonNull ClientCertRequest request) {
        mDialogHolder.showPinDialog(
                getSmartcardPinDialogPositiveButtonListener(certDetails, request),
                new SmartcardPinDialog.CancelCbaCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onCancel() {
                        mDialogHolder.dismissDialog();
                        mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                        request.cancel();
                    }
                });
        //Update Dialog to indicate that an incorrect attempt was made.
        mDialogHolder.setPinDialogErrorMode();
    }

    @Override
    public void promptSmartcardRemovalForResult(@NonNull final FinalizeResultCallback callback) {
        if (mCbaManager.isDeviceConnected()) {
            mDialogHolder.showSmartcardRemovalPromptDialog(new SmartcardRemovalPromptDialog.RemovalCallback() {
                @Override
                public void onRemoved() {
                    callback.onResultReady();
                }
            });
            mCbaManager.disconnect(new AbstractSmartcardCertBasedAuthManager.IDisconnectCallback() {
                @Override
                public void onDisconnect() {
                    mDialogHolder.onSmartcardRemoval();
                    mCbaManager.stopDiscovery(mActivity);
                }
            });
            return;
        }
        callback.onResultReady();
    }
}
