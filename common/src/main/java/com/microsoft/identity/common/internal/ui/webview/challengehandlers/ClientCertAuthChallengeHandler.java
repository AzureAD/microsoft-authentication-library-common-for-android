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

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
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

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Handles Certificate Based Authentication by means of certificates provisioned onto external smartcards or the mobile devices themselves.
 * Note that CBA requires API >= 21 (ClientCertRequest class available with API >= 21)
 */
public final class ClientCertAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private final Activity mActivity;
    //Smartcard CBA variables
    protected final ISmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;
    //Booleans to help determine if a CBA flow is being completed so that we can emit telemetry for the results.
    private boolean mIsOnDeviceCertBasedAuthProceeding;
    private boolean mIsSmartcardCertBasedAuthProceeding;

    /**
     * Creates new instance of ClientCertAuthChallengeHandler.
     * A manager for smartcard CBA is retrieved, and discovery for USB devices is started.
     * @param activity current host activity.
     * @param smartcardCertBasedAuthManager ISmartcardCertBasedAuthManager instance.
     */
    public ClientCertAuthChallengeHandler(@NonNull final Activity activity,
                                          @NonNull final ISmartcardCertBasedAuthManager smartcardCertBasedAuthManager) {
        final String methodTag = TAG + ":ClientCertAuthChallengeHandler";
        mActivity = activity;
        mDialogHolder = new DialogHolder(mActivity);
        mIsOnDeviceCertBasedAuthProceeding = false;
        mIsSmartcardCertBasedAuthProceeding = false;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
        mSmartcardCertBasedAuthManager.startDiscovery(new ISmartcardCertBasedAuthManager.IStartDiscoveryCallback() {
            @Override
            public void onStartDiscovery() {
                //Reset DialogHolder to null if necessary.
                //In this case, DialogHolder would be an ErrorDialog if not null.
                mDialogHolder.dismissDialog();
            }

            @Override
            public void onClosedConnection() {
                //Show an error dialog informing users that they have unplugged their device only if a dialog is still showing.
                if (mDialogHolder.isDialogShowing()) {
                    mDialogHolder.onCancelCba();
                    mDialogHolder.showErrorDialog(R.string.smartcard_early_unplug_dialog_title, R.string.smartcard_early_unplug_dialog_message);
                    Logger.verbose(TAG, "Smartcard was disconnected while dialog was still displayed.");
                }
            }

            @Override
            public void onException() {
                //Logging, but may also want to emit telemetry.
                //This method is not currently being called, but it could be
                // used in future SmartcardCertBasedAuthManager implementations.
                Logger.error(methodTag, "Unable to start smartcard usb discovery.", null);
            }
        });
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Prompts the user to choose a certificate to authenticate with based on whether or not a smartcard device is plugged in and has permission to be connected.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null in either case.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":processChallenge";
        //If a smartcard device is connected, proceed with smartcard CBA.
        if (mSmartcardCertBasedAuthManager.isDeviceConnected()) {
            handleSmartcardCertAuth(request);
            return null;
        }
        //Else, proceed with user certificates stored on device.
        handleOnDeviceCertAuth(request);
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

        mSmartcardCertBasedAuthManager.requestDeviceSession(new ISmartcardCertBasedAuthManager.ISessionCallback() {
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
                    Logger.info(methodTag,  "No PIV certificates found on YubiKey device.");
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
            public void onException(@NonNull Exception e) {
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
                mSmartcardCertBasedAuthManager.requestDeviceSession(new ISmartcardCertBasedAuthManager.ISessionCallback() {
                    @Override
                    public void onGetSession(@NonNull ISmartcardSession session) throws Exception {
                        tryUsingSmartcardWithPin(pin, certDetails, request, session);
                        clearPin(pin);
                    }

                    @Override
                    public void onException(@NonNull Exception e) {
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
     * Handles the logic for on-device certificate based authentication.
     * Makes use of Android's KeyChain.choosePrivateKeyAlias method, which shows a certificate picker that allows users to choose their on-device user certificate to authenticate with.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleOnDeviceCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleOnDeviceCertAuth";
        final Principal[] acceptableCertIssuers = request.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(ACCEPTABLE_ISSUER)) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    Logger.info(methodTag,"Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.");
                    request.cancel();
                    return;
                }
            }
        }

        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
                    @Override
                    public void alias(String alias) {
                        if (alias == null) {
                            Logger.info(methodTag,"No certificate chosen by user, cancelling the TLS request.");
                            request.cancel();
                            return;
                        }

                        try {
                            final X509Certificate[] certChain = KeyChain.getCertificateChain(
                                    mActivity.getApplicationContext(), alias);
                            final PrivateKey privateKey = KeyChain.getPrivateKey(
                                    mActivity, alias);

                            Logger.info(methodTag,"Certificate is chosen by user, proceed with TLS request.");
                            //Set mIsOnDeviceCertBasedAuthProceeding to true so telemetry is emitted for the result.
                            mIsOnDeviceCertBasedAuthProceeding = true;
                            request.proceed(privateKey, certChain);
                            return;
                        } catch (final KeyChainException e) {
                            Logger.errorPII(methodTag,"KeyChain exception", e);
                        } catch (final InterruptedException e) {
                            Logger.errorPII(methodTag,"InterruptedException exception", e);
                        }

                        request.cancel();
                    }
                },
                request.getKeyTypes(),
                request.getPrincipals(),
                request.getHost(),
                request.getPort(),
                null);
    }

    /**
     * Allows AzureActiveDirectoryWebViewClient to stop the local SmartcardCertBasedAuthManager's discovery mode.
     */
    public void stopSmartcardUsbDiscovery() {
        //Should be called when host fragment is destroyed.
        mSmartcardCertBasedAuthManager.stopDiscovery();
    }

    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    public void emitTelemetryForCertBasedAuthResults(@NonNull final RawAuthorizationResult response) {
        if (mIsOnDeviceCertBasedAuthProceeding || mIsSmartcardCertBasedAuthProceeding) {
            //Emit telemetry for results based on which type of CBA occurred.
            final CertBasedAuthResultEvent certBasedAuthResultEvent;
            if (mIsOnDeviceCertBasedAuthProceeding) {
               certBasedAuthResultEvent =  new CertBasedAuthResultEvent(TelemetryEventStrings.Event.CERT_BASED_AUTH_RESULT_ON_DEVICE_EVENT);
               mIsOnDeviceCertBasedAuthProceeding = false;
            } else {
                certBasedAuthResultEvent =  new CertBasedAuthResultEvent(TelemetryEventStrings.Event.CERT_BASED_AUTH_RESULT_SMARTCARD_EVENT);
                mIsSmartcardCertBasedAuthProceeding = false;
            }
            //Put response code and emit.
            Telemetry.emit(certBasedAuthResultEvent.putResponseCode(response.getResultCode().toString()));
            //If an exception was provided, emit an ErrorEvent.
            final BaseException exception = response.getException();
            if (exception != null) {
                Telemetry.emit(new ErrorEvent().putException(exception));
            }
        }
    }
}