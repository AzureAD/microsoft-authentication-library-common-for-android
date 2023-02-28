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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract class which handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on a smartcard device.
 */
public abstract class AbstractSmartcardCertBasedAuthChallengeHandler<T extends AbstractSmartcardCertBasedAuthManager> extends AbstractCertBasedAuthChallengeHandler {
    protected static final String MAX_ATTEMPTS_MESSAGE = "User has reached the maximum failed attempts allowed.";
    protected static final String NO_PIV_CERTS_FOUND_MESSAGE = "No PIV certificates found on smartcard device.";
    protected static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    protected final String TAG;
    protected final Activity mActivity;
    protected final T mCbaManager;
    protected final IDialogHolder mDialogHolder;

    /**
     * Creates new instance of AbstractSmartcardCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param smartcardCertBasedAuthManager AbstractSmartcardCertBasedAuthManager implementation instance.
     * @param dialogHolder DialogHolder instance.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     * @param tag name of challenge handler, for logging purposes.
     */
    public AbstractSmartcardCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                          @NonNull final T smartcardCertBasedAuthManager,
                                                          @NonNull final IDialogHolder dialogHolder,
                                                          @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper,
                                                          @NonNull final String tag) {
        TAG = tag;
        mActivity = activity;
        mIsCertBasedAuthProceeding = false;
        mCbaManager = smartcardCertBasedAuthManager;
        mDialogHolder = dialogHolder;
        mTelemetryHelper = telemetryHelper;
        mTelemetryHelper.setCertBasedAuthChallengeHandler(TAG);
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Collects ICertDetails from PIV certificates on smartcard device.
     * If certificates are found on smartcard, the method proceeds with the smartcard certificate picker.
     * Otherwise, appropriate error dialogs are shown.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
        mCbaManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
            @Override
            public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                final int pinAttemptsRemaining = session.getPinAttemptsRemaining();
                final List<ICertDetails> certList = session.getCertDetailsList();
                //We have the necessary data, so we don't need the connection to the smartcard (for now)
                prepForNextUserInteraction(new IDisconnectionCallback() {
                    @Override
                    public void onClosedConnection() {
                        if (pinAttemptsRemaining == 0) {
                            promptTooManyFailedPinAttempts(methodTag);
                            request.cancel();
                            return;
                        }
                        //If no certs were found, cancel flow.
                        if (certList.isEmpty()) {
                            Logger.info(methodTag, NO_PIV_CERTS_FOUND_MESSAGE);
                            mTelemetryHelper.setResultFailure(NO_PIV_CERTS_FOUND_MESSAGE);
                            mDialogHolder.showErrorDialog(
                                    R.string.smartcard_no_cert_dialog_title,
                                    R.string.smartcard_no_cert_dialog_message);
                            request.cancel();
                            return;
                        }

                        mDialogHolder.showCertPickerDialog(
                                certList,
                                getSmartcardCertPickerDialogPositiveButtonListener(request),
                                getGeneralCancelCbaCallback(request)
                        );
                    }
                });
            }

            @Override
            public void onException(@NonNull final Exception e) {
                request.cancel();
                prepForNextUserInteraction(new IDisconnectionCallback() {
                    @Override
                    public void onClosedConnection() {
                        indicateGeneralException(methodTag, e);
                    }
                });
            }
        });
        return null;
    }

    /**
     * To be called when user interaction is needed, or to prepare for any user interaction.
     * When a connection is no longer actively being used, the dialog flow should pause
     * so the user can remove their smartcard before flow can continue.
     * @param nextInteractionCallback the next logic to be run.
     */
    protected abstract void prepForNextUserInteraction(@Nullable final IDisconnectionCallback nextInteractionCallback);

    /**
     * Returns a callback that dismisses the current dialog, sends telemetry, and cancels the ClientCertRequest.
     * @param request {@link ClientCertRequest}
     * @return {@link ICancelCbaCallback}
     */
    @NonNull
    protected ICancelCbaCallback getGeneralCancelCbaCallback(@NonNull final ClientCertRequest request) {
        return new ICancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                mDialogHolder.dismissDialog();
                mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                request.cancel();
            }
        };
    }

    /**
     * Helper method to log and show an error dialog with messages indicating that
     *  too many failed login attempts have occurred.
     * @param methodTag tag from calling method.
     */
    protected void promptTooManyFailedPinAttempts(@NonNull final String methodTag) {
        Logger.info(methodTag,  MAX_ATTEMPTS_MESSAGE);
        mTelemetryHelper.setResultFailure(MAX_ATTEMPTS_MESSAGE);
        mDialogHolder.showErrorDialog(
                R.string.smartcard_max_attempt_dialog_title,
                R.string.smartcard_max_attempt_dialog_message);
    }

    /**
     * Helper method to log and show a disconnection error.
     * @param methodName calling method name.
     */
    protected abstract void indicateDisconnectionError(@NonNull final String methodName);

    /**
     * Helper method to log, show general error dialog, and emit telemetry for when
     *  an unexpected exception is thrown.
     * @param methodTag tag from calling method.
     * @param e Exception thrown.
     */
    protected void indicateGeneralException(@NonNull final String methodTag,
                                            @NonNull final Exception e) {
        Logger.error(methodTag, e.getMessage(), e);
        mTelemetryHelper.setResultFailure(e);
        //Show general error dialog.
        mDialogHolder.showErrorDialog(
                R.string.smartcard_general_error_dialog_title,
                R.string.smartcard_general_error_dialog_message);
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
                mDialogHolder.showPinDialog(
                        getSmartcardPinDialogPositiveButtonListener(certDetails, request),
                        getGeneralCancelCbaCallback(request)
                );
            }
        };
    }

    /**
     * Upon a positive button click in the smartcard PIN dialog, verify the provided PIN and handle the results.
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardPinDialog.
     */
    protected abstract SmartcardPinDialog.PositiveButtonListener getSmartcardPinDialogPositiveButtonListener(@NonNull final ICertDetails certDetails,
                                                                                                             @NonNull final ClientCertRequest request);

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
        prepForNextUserInteraction(new IDisconnectionCallback() {
            @Override
            public void onClosedConnection() {
                if (attemptsRemaining == 0) {
                    promptTooManyFailedPinAttempts(methodTag);
                    request.cancel();
                    return;
                }
                setPinDialogForIncorrectAttempt(certDetails, request);
            }
        });
    }

    /**
     * Shows PIN dialog, if not already showing, and sets dialog to error mode.
     * @param certDetails ICertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    protected abstract void setPinDialogForIncorrectAttempt(@NonNull final ICertDetails certDetails,
                                                            @NonNull final ClientCertRequest request);

    /**
     * Authenticates using smartcard certificate.
     * @param certDetails ICertDetails of chosen certificate for authentication.
     * @param pin char array containing PIN.
     * @param session An ISmartcardSession created to help with interactions pertaining to certificates.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void useSmartcardCertForAuth(@NonNull final ICertDetails certDetails,
                                           @NonNull final char[] pin,
                                           @NonNull final ISmartcardSession session,
                                           @NonNull final ClientCertRequest request)
            throws Exception {
        //Each type of smartcard manager could have different preparation steps before proceeding with a ClientCertRequest.
        mCbaManager.initBeforeProceedingWithRequest(mTelemetryHelper);
        //PivPrivateKey implements PrivateKey. Note that the PIN is copied in pivPrivateKey.
        final PrivateKey privateKey = session.getKeyForAuth(certDetails, pin);
        //Cert chain only needs the cert to be used for authentication.
        final X509Certificate[] chain = new X509Certificate[]{certDetails.getCertificate()};
        //Clear current dialog.
        mDialogHolder.dismissDialog();
        mIsCertBasedAuthProceeding = true;
        request.proceed(privateKey, chain);
    }

    /**
     * Sets all chars in the PIN array to 0.
     * @param pin char array containing PIN.
     */
    protected void clearPin(@NonNull final char[] pin) {
        Arrays.fill(pin, Character.MIN_VALUE);
    }

    /**
     * If a smartcard is currently connected, prompt user to remove the smartcard before
     *  proceeding with results.
     * @param callback {@link ISendResultCallback}
     */
    public abstract void promptSmartcardRemovalForResult(@NonNull final ISendResultCallback callback);

    /**
     * Clean-up logic to run when AbstractSmartcardCertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {
        mDialogHolder.dismissDialog();
        clearAllManagerCallbacks();
    }

    /**
     * Clears appropriate connection and disconnection callbacks.
     */
    protected abstract void clearAllManagerCallbacks();
}
