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

import com.microsoft.identity.common.internal.ui.webview.SendResultCallback;
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
    protected void pauseForRemoval(@NonNull final IDisconnectionCallback callback) {
        //Helps prevent unnecessary callback trigger. Nfc discovery should only be active when
        // the user is prompted to tap.
        mDialogHolder.showSmartcardRemovalPromptDialog();
        mCbaManager.setDisconnectionCallback(new IDisconnectionCallback() {
            @Override
            public void onClosedConnection() {
                mDialogHolder.dismissDialog();
                mCbaManager.clearDisconnectionCallback();
                mCbaManager.stopDiscovery(mActivity);
                callback.onClosedConnection();
            }
        });
        mCbaManager.disconnect();
    }

    /**
     * Helper method to log and show a disconnection error.
     *
     * @param methodTag tag from calling method.
     */
    @Override
    protected void indicateDisconnectionError(@NonNull String methodTag) {
        //Nothing needed
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
                mDialogHolder.showSmartcardNfcPromptDialog(new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        getGeneralCancelCbaCallback(request).onCancel();
                        mCbaManager.stopDiscovery(mActivity);
                    }
                });
                mCbaManager.setConnectionCallback(new IConnectionCallback() {
                    @Override
                    public void onCreateConnection() {
                        mDialogHolder.showSmartcardNfcLoadingDialog();
                        if (mCbaManager.isDeviceChanged()) {
                            clearPin(pin);
                            request.cancel();
                            pauseForRemoval(new IDisconnectionCallback() {
                                @Override
                                public void onClosedConnection() {
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
                                pauseForRemoval(new IDisconnectionCallback() {
                                    @Override
                                    public void onClosedConnection() {
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
                getGeneralCancelCbaCallback(request));
        //Update Dialog to indicate that an incorrect attempt was made.
        mDialogHolder.setPinDialogErrorMode();
    }

    /**
     * TODO
     * @param callback
     */
    @Override
    public void promptSmartcardRemovalForResult(@NonNull final SendResultCallback callback) {
        if (mCbaManager.isDeviceConnected()) {
            pauseForRemoval(new IDisconnectionCallback() {
                @Override
                public void onClosedConnection() {
                    callback.onResultReady();
                }
            });
            return;
        }
        callback.onResultReady();
    }
}
