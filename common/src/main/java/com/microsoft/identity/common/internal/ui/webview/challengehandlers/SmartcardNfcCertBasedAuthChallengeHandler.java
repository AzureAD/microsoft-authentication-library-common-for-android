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

public class SmartcardNfcCertBasedAuthChallengeHandler extends AbstractSmartcardCertBasedAuthChallengeHandler{
    /**
     * Creates new instance of SmartcardNfcCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param smartcardNfcCertBasedAuthManager SmartcardNfcCertBasedAuthManager instance.
     * @param dialogHolder DialogHolder instance.
     * @param telemetryHelper CertBasedAuthTelemetryHelder instance.
     */
    public SmartcardNfcCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                     @NonNull final AbstractSmartcardCertBasedAuthManager smartcardNfcCertBasedAuthManager,
                                                     @NonNull final DialogHolder dialogHolder,
                                                     @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        super(activity, smartcardNfcCertBasedAuthManager, dialogHolder, telemetryHelper, SmartcardNfcCertBasedAuthChallengeHandler.class.getSimpleName());
        mSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @Override
            public void onCreateConnection() {
                //Reset DialogHolder to null if necessary.
                //In this case, DialogHolder would be an ErrorDialog if not null.
                mDialogHolder.dismissDialog();
            }

            @Override
            public void onClosedConnection() {
                //Nothing needed.
            }
        });
    }

    /**
     * Additional logic to run upon before the end of processChallenge.
     */
    @Override
    protected void onGetCertSessionExtendedLogic() {
        mSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
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
                        request.cancel();
                    }
                });
                mSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
                    @Override
                    public void onCreateConnection() {
                        mDialogHolder.showDialog(new SmartcardNfcLoadingDialog(mActivity));
                        mSmartcardCertBasedAuthManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
                            @Override
                            public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                                tryUsingSmartcardWithPin(pin, certDetails, request, session);
                                clearPin(pin);
                            }

                            @Override
                            public void onException(@NonNull final Exception e) {
                                indicateGeneralException(methodTag, e);
                                request.cancel();
                                clearPin(pin);
                                mSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
                            }
                        });
                    }

                    @Override
                    public void onClosedConnection() {
                        //Nothing needed.
                    }
                });
                mSmartcardCertBasedAuthManager.startDiscovery(mActivity);
            }
        };
    }

    /**
     * Checks to see if PIN for smartcard is correct.
     * If so, proceed to attempt authentication.
     * Otherwise, handle the incorrect PIN based on how many PIN attempts are remaining.
     * @param pin char array containing PIN attempt.
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @param session An ISmartcardSession created to help with interactions pertaining to certificates.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void tryUsingSmartcardWithPin(@NonNull final char[] pin,
                                          @NonNull final ICertDetails certDetails,
                                          @NonNull final ClientCertRequest request,
                                          @NonNull final ISmartcardSession session)
            throws Exception {
        final String methodTag = TAG + ":tryUsingSmartcardWithPin";
        if (session.verifyPin(pin)) {
            //If pin is successfully verified, we will use the certificate to perform the rest of the logic for authentication.
            useSmartcardCertForAuth(certDetails, pin, session, request);
            return;
        }
        final int attemptsRemaining = session.getPinAttemptsRemaining();
        mSmartcardCertBasedAuthManager.stopDiscovery(mActivity);
        // If the number of attempts is 0, no more attempts will be allowed.
        if (attemptsRemaining == 0) {
            //We must display a dialog informing the user that they have made too many incorrect attempts,
            // and the user will need to figure out a way to reset their key outside of our library.
            indicateTooManyFailedAttempts(methodTag);
            request.cancel();
            return;
        }
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
}
