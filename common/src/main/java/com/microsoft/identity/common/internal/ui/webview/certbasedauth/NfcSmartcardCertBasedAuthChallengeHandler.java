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

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.ui.webview.ISendResultCallback;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

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
     * To be called when user interaction is needed, or to prepare for any unexpected user interaction.
     * When a connection is no longer actively being used, the dialog flow should pause
     * so the user can remove their smartcard before flow can continue.
     * @param nextInteractionCallback the next logic to be run after a user removes their smartcard.
     */
    @Override
    protected void prepForNextUserInteraction(@NonNull final IDisconnectionCallback nextInteractionCallback) {
        if (!mCbaManager.isDeviceConnected()) {
            nextInteractionCallback.onClosedConnection();
            return;
        }
        clearAllManagerCallbacks();
        //This would normally be where we would use the remove() YubiKit method within a wrapper.
        //But there seems to be some concurrency issues that arise when using this method with certain Android devices.
        //So for now, the prompt that tells the user they can remove their smartcard will have a button where they can dismiss it themselves.
        mDialogHolder.showSmartcardRemovalPromptDialog(new IDismissCallback() {
            @Override
            public void onDismiss() {
                //Helps prevent unnecessary callback trigger. Nfc discovery should only be active when
                // the user is prompted to tap.
                mCbaManager.stopDiscovery(mActivity);
                nextInteractionCallback.onClosedConnection();
            }
        });
        mCbaManager.disconnect(new IDisconnectionCallback() {
            @Override
            public void onClosedConnection() {
                mDialogHolder.dismissDialog();
                mCbaManager.stopDiscovery(mActivity);
                nextInteractionCallback.onClosedConnection();
            }
        });
    }

    /**
     * Helper method to log and show a disconnection error.
     * @param methodName calling method name.
     */
    @Override
    protected void indicateDisconnectionError(@NonNull String methodName) {
        //Nothing needed for NFC.
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
                            prepForNextUserInteraction(new IDisconnectionCallback() {
                                @Override
                                public void onClosedConnection() {
                                    //In a future version, an error dialog with a custom message could be shown here instead of a general error.
                                    final String errorMessage = "Device connected via NFC is different from initially connected device.";
                                    Logger.info(methodTag, errorMessage);
                                    mTelemetryHelper.setResultFailure(errorMessage);
                                    //Show general error dialog.
                                    mDialogHolder.showErrorDialog(
                                            R.string.smartcard_general_error_dialog_title,
                                            R.string.smartcard_general_error_dialog_message);
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
                                prepForNextUserInteraction(new IDisconnectionCallback() {
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
     * If a smartcard is currently connected, prompt user to remove the smartcard before
     *  proceeding with results.
     * @param callback {@link ISendResultCallback}
     */
    @Override
    public void promptSmartcardRemovalForResult(@NonNull final ISendResultCallback callback) {
        if (mCbaManager.isDeviceConnected()) {
            prepForNextUserInteraction(new IDisconnectionCallback() {
                @Override
                public void onClosedConnection() {
                    callback.onResultReady();
                }
            });
            return;
        }
        callback.onResultReady();
    }

    /**
     * Clears appropriate connection and disconnection callbacks.
     */
    @Override
    protected void clearAllManagerCallbacks() {
        mCbaManager.clearConnectionCallback();
    }
}
