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

import android.os.Build;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.events.CertBasedAuthResultEvent;
import com.microsoft.identity.common.java.telemetry.events.ErrorEvent;
import com.microsoft.identity.common.logging.Logger;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on a connected smartcard device.
 */
public class SmartcardCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    private static final String TAG = SmartcardCertBasedAuthChallengeHandler.class.getSimpleName();
    protected final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;
    private boolean mIsSmartcardCertBasedAuthProceeding;

    /**
     * Creates new instance of SmartcardCertBasedAuthChallengeHandler.
     * A manager for smartcard CBA is retrieved, and discovery for USB devices is started.
     * @param smartcardCertBasedAuthManager AbstractSmartcardCertBasedAuthManager instance.
     * @param dialogHolder DialogHolder instance.
     */
    public SmartcardCertBasedAuthChallengeHandler(@NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                  @NonNull final DialogHolder dialogHolder) {
        mIsSmartcardCertBasedAuthProceeding = false;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
        mDialogHolder = dialogHolder;
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Prompts the user to choose a certificate to authenticate with.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(ClientCertRequest request) {
        handleSmartcardCertAuth(request);
        return null;
    }

    /**
     * Collects ICertDetails from PIV certificates on smartcard device.
     * If certificates are found on smartcard, the method proceeds with the smartcard certificate picker.
     * Otherwise, appropriate error dialogs are shown.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handleSmartcardCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleSmartcardCertAuth";
        mSmartcardCertBasedAuthManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
            @Override
            public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                if (session.getPinAttemptsRemaining() == 0) {
                    indicateTooManyFailedAttempts(methodTag);
                    request.cancel();
                    return;
                }
                //Create List that contains cert details only pertinent to the cert picker.
                final List<ICertDetails> certList = session.getCertDetailsList();
                //If no certs were found, cancel flow.
                if (certList.isEmpty()) {
                    Logger.info(methodTag,  "No PIV certificates found on smartcard device.");
                    mDialogHolder.showErrorDialog(
                            R.string.smartcard_no_cert_dialog_title,
                            R.string.smartcard_no_cert_dialog_message);
                    request.cancel();
                    return;
                }

                //Build and show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                mDialogHolder.showCertPickerDialog(
                        certList,
                        getSmartcardCertPickerDialogPositiveButtonListener(request),
                        new SmartcardCertPickerDialog.CancelCbaCallback() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onCancel() {
                                mDialogHolder.dismissDialog();
                                request.cancel();
                            }
                        });
            }

            @Override
            public void onException(@NonNull final Exception e) {
                indicateGeneralException(methodTag, e);
                request.cancel();
            }
        });
    }

    /**
     * Helper method to log and show an error dialog with messages indicating that
     *  too many failed login attempts have occurred.
     * @param methodTag tag from calling method.
     */
    private void indicateTooManyFailedAttempts(@NonNull final String methodTag) {
        Logger.info(methodTag,  "User has reached the maximum failed attempts allowed.");
        mDialogHolder.showErrorDialog(
                R.string.smartcard_max_attempt_dialog_title,
                R.string.smartcard_max_attempt_dialog_message);
    }

    /**
     * Helper method to log, show general error dialog, and emit telemetry for when
     *  an unexpected exception is thrown.
     * @param methodTag tag from calling method.
     * @param e Exception thrown.
     */
    private void indicateGeneralException(@NonNull final String methodTag, @NonNull final Exception e) {
        Logger.error(methodTag, e.getMessage(), e);
        //Show general error dialog.
        mDialogHolder.showErrorDialog(
                R.string.smartcard_general_error_dialog_title,
                R.string.smartcard_general_error_dialog_message);
        Telemetry.emit(new ErrorEvent().putException(e));
    }

    /**
     * Upon a positive button click in the certificate picker, the listener will proceed with a PIN prompt dialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardCertPickerDialog.
     */
    private SmartcardCertPickerDialog.PositiveButtonListener getSmartcardCertPickerDialogPositiveButtonListener(@NonNull final ClientCertRequest request) {
        return new SmartcardCertPickerDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(@NonNull final ICertDetails certDetails) {
                //Need to prompt user for pin and verify pin. The positive button listener will handle the rest of the CBA flow.
                mDialogHolder.showPinDialog(
                        getSmartcardPinDialogPositiveButtonListener(certDetails, request),
                        new SmartcardPinDialog.CancelCbaCallback() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onCancel() {
                                mDialogHolder.dismissDialog();
                                request.cancel();
                            }
                        });
            }
        };
    }

    /**
     * Upon a positive button click in the smartcard PIN dialog, verify the provided PIN and handle the results.
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardPinDialog.
     */
    private SmartcardPinDialog.PositiveButtonListener getSmartcardPinDialogPositiveButtonListener(@NonNull final ICertDetails certDetails,
                                                                                                  @NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":getSmartcardPinDialogPositiveButtonListener";

        return new SmartcardPinDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(@NonNull final char[] pin) {
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
                    }
                });
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
    private void tryUsingSmartcardWithPin(@NonNull final char[] pin,
                                          @NonNull final ICertDetails certDetails,
                                          @NonNull final ClientCertRequest request,
                                          @NonNull final ISmartcardSession session)
            throws Exception {
        final String methodTag = TAG + ":tryUsingSmartcardWithPin";
        if (session.verifyPin(pin)) {
            //If pin is successfully verified, we will use the certificate to perform the rest of the logic for authentication.
            useSmartcardCertForAuth(certDetails, pin, session, request);
        }
        else {
            // If the number of attempts is 0, no more attempts will be allowed.
            if (session.getPinAttemptsRemaining() == 0) {
                //We must display a dialog informing the user that they have made too many incorrect attempts,
                // and the user will need to figure out a way to reset their key outside of our library.
                indicateTooManyFailedAttempts(methodTag);
                request.cancel();
            } else {
                //Update Dialog to indicate that an incorrect attempt was made.
                mDialogHolder.setPinDialogErrorMode();
            }

        }
    }

    /**
     * Attempts to authenticate using smartcard certificate.
     * @param certDetails ICertDetails of chosen certificate for authentication.
     * @param pin char array containing PIN.
     * @param session An ISmartcardSession created to help with interactions pertaining to certificates.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void useSmartcardCertForAuth(@NonNull final ICertDetails certDetails,
                                         @NonNull final char[] pin,
                                         @NonNull final ISmartcardSession session,
                                         @NonNull final ClientCertRequest request)
            throws Exception {
        //Each type of smartcard manager could have different preparation steps before proceeding with a ClientCertRequest.
        mSmartcardCertBasedAuthManager.prepareForAuth();
        //PivPrivateKey implements PrivateKey. Note that the PIN is copied in pivPrivateKey.
        final PrivateKey privateKey = session.getKeyForAuth(certDetails, pin);
        //Cert chain only needs the cert to be used for authentication.
        final X509Certificate[] chain = new X509Certificate[]{certDetails.getCertificate()};
        //Clear current dialog.
        mDialogHolder.dismissDialog();
        mIsSmartcardCertBasedAuthProceeding = true;
        request.proceed(privateKey, chain);
    }

    /**
     * Sets all chars in the PIN array to 0.
     * @param pin char array containing PIN.
     */
    private void clearPin(@NonNull final char[] pin) {
        Arrays.fill(pin, Character.MIN_VALUE);
    }

    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull final RawAuthorizationResult response) {
        if (mIsSmartcardCertBasedAuthProceeding) {
            final CertBasedAuthResultEvent certBasedAuthResultEvent = new CertBasedAuthResultEvent(TelemetryEventStrings.Event.CERT_BASED_AUTH_RESULT_SMARTCARD_EVENT);
            mIsSmartcardCertBasedAuthProceeding = false;
            Telemetry.emit(certBasedAuthResultEvent.putResponseCode(response.getResultCode().toString()));
            //If an exception was provided, emit an ErrorEvent.
            final BaseException exception = response.getException();
            if (exception != null) {
                Telemetry.emit(new ErrorEvent().putException(exception));
            }
        }
    }
}
