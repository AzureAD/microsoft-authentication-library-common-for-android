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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.ui.webview.ISendResultCallback;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

/**
 * Handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on a smartcard device connected via USB.
 */
public class UsbSmartcardCertBasedAuthChallengeHandler extends AbstractSmartcardCertBasedAuthChallengeHandler<AbstractUsbSmartcardCertBasedAuthManager> {

    /**
     * Creates new instance of UsbSmartcardCertBasedAuthChallengeHandler.
     * A manager for smartcard CBA is retrieved, and discovery for USB devices is started.
     *
     * @param activity                         current host activity.
     * @param usbSmartcardCertBasedAuthManager AbstractUsbSmartcardCertBasedAuthManager instance.
     * @param dialogHolder                     DialogHolder instance.
     * @param telemetryHelper                  CertBasedAuthTelemetryHelder instance.
     */
    public UsbSmartcardCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                     @NonNull final AbstractUsbSmartcardCertBasedAuthManager usbSmartcardCertBasedAuthManager,
                                                     @NonNull final IDialogHolder dialogHolder,
                                                     @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        super(activity, usbSmartcardCertBasedAuthManager, dialogHolder, telemetryHelper, UsbSmartcardCertBasedAuthChallengeHandler.class.getSimpleName());
        mCbaManager.setConnectionCallback(new IConnectionCallback() {
            @Override
            public void onCreateConnection() {
                //Reset DialogHolder to null if necessary.
                //In this case, DialogHolder would be an ErrorDialog if not null.
                mDialogHolder.dismissDialog();
            }
        });
        mCbaManager.setDisconnectionCallback(new IDisconnectionCallback() {
            @Override
            public void onClosedConnection() {
                if (mDialogHolder.isDialogShowing()) {
                    mDialogHolder.onUnexpectedUnplug();
                    if (!mDialogHolder.isSmartcardRemovalPromptDialogShowing()) {
                        //Show an error dialog informing users that they have unplugged their device.
                        mDialogHolder.showErrorDialog(R.string.smartcard_early_unplug_dialog_title, R.string.smartcard_early_unplug_dialog_message);
                        Logger.verbose(TAG, "Smartcard was disconnected while dialog was still displayed.");
                    }
                }
            }
        });
    }

    /**
     * To be called when user interaction is needed, or to prepare for any unexpected user interaction.
     * @param nextInteractionCallback the next logic to be run.
     */
    @Override
    protected void prepForNextUserInteraction(@Nullable final IDisconnectionCallback nextInteractionCallback) {
        //Usb discovery and connection should always remain active for the duration of the authentication flow.
        //Therefore, we merely invoke the callback here.
        if (nextInteractionCallback != null) {
            nextInteractionCallback.onClosedConnection();
        }
    }

    /**
     * Helper method to log and show a disconnection error.
     *
     * @param methodName calling method name.
     */
    @Override
    protected void indicateDisconnectionError(@NonNull final String methodName) {
        final String methodTag = TAG + ":" + methodName;
        mDialogHolder.showErrorDialog(R.string.smartcard_early_unplug_dialog_title, R.string.smartcard_early_unplug_dialog_message);
        Logger.verbose(methodTag, "Smartcard was disconnected while dialog was still displayed.");
        mCbaManager.clearDisconnectionCallback();
    }

    /**
     * Upon a positive button click in the smartcard PIN dialog, verify the provided PIN and handle the results.
     *
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request     ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
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
                mCbaManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
                    @Override
                    public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                        tryUsingSmartcardWithPin(pin, certDetails, request, session);
                        clearPin(pin);
                    }

                    @Override
                    public void onException(@NonNull final Exception e) {
                        indicateGeneralException(methodTag, e);
                        clearAllManagerCallbacks();
                        request.cancel();
                        clearPin(pin);
                    }
                });
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
        mDialogHolder.setPinDialogErrorMode();
    }

    /**
     * If a smartcard is currently connected, prompt user to remove the smartcard before
     *  proceeding with results.
     * @param callback {@link ISendResultCallback}
     */
    @Override
    public void promptSmartcardRemovalForResult(@NonNull final ISendResultCallback callback) {
        //If a usb device was originally plugged in, unplugging would cause a config change.
        //So we won't ask the user to unplug in that case.
        if (mCbaManager.isDeviceConnected()
                && !mCbaManager.isUsbDeviceInitiallyPluggedIn()) {
            mCbaManager.setDisconnectionCallback(new IDisconnectionCallback() {
                @Override
                public void onClosedConnection() {
                    mDialogHolder.dismissDialog();
                    callback.onResultReady();
                }
            });
            mDialogHolder.showSmartcardRemovalPromptDialog(new IDismissCallback() {
                @Override
                public void onDismiss() {
                    callback.onResultReady();
                }
            });
            return;
        }
        callback.onResultReady();
    }

    /**
     * Clears appropriate connection and/or disconnection callbacks.
     */
    @Override
    protected void clearAllManagerCallbacks() {
        mCbaManager.clearConnectionCallback();
        mCbaManager.clearDisconnectionCallback();
    }
}
