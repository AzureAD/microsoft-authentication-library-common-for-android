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

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.logging.Logger;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract class which handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on a smartcard device.
 */
public abstract class AbstractSmartcardCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    protected static final String MAX_ATTEMPTS_MESSAGE = "User has reached the maximum failed attempts allowed.";
    protected static final String NO_PIV_CERTS_FOUND_MESSAGE = "No PIV certificates found on smartcard device.";
    protected static final String USER_CANCEL_MESSAGE = "User canceled smartcard CBA flow.";
    protected final String TAG;
    protected final Activity mActivity;
    protected final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    protected final DialogHolder mDialogHolder;
    protected final CertBasedAuthTelemetryHelper mTelemetryHelper;
    protected boolean mIsSmartcardCertBasedAuthProceeding;

    /**
     * Creates new instance of AbstractSmartcardCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param smartcardCertBasedAuthManager AbstractSmartcardCertBasedAuthManager implementation instance.
     * @param dialogHolder DialogHolder instance.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     * @param tag name of challenge handler, for logging purposes.
     */
    public AbstractSmartcardCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                          @NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                          @NonNull final DialogHolder dialogHolder,
                                                          @NonNull final CertBasedAuthTelemetryHelper telemetryHelper,
                                                          @NonNull final String tag) {
        TAG = tag;
        mActivity = activity;
        mIsSmartcardCertBasedAuthProceeding = false;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
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
        mSmartcardCertBasedAuthManager.requestDeviceSession(new AbstractSmartcardCertBasedAuthManager.ISessionCallback() {
            @Override
            public void onGetSession(@NonNull final ISmartcardSession session) throws Exception {
                if (session.getPinAttemptsRemaining() == 0) {
                    indicateTooManyFailedAttempts(methodTag);
                    request.cancel();
                    onGetSessionFinished();
                    return;
                }
                //Create List that contains cert details only pertinent to the cert picker.
                final List<ICertDetails> certList = session.getCertDetailsList();
                //If no certs were found, cancel flow.
                if (certList.isEmpty()) {
                    Logger.info(methodTag,  NO_PIV_CERTS_FOUND_MESSAGE);
                    mTelemetryHelper.setResultFailure(NO_PIV_CERTS_FOUND_MESSAGE);
                    mDialogHolder.showErrorDialog(
                            R.string.smartcard_no_cert_dialog_title,
                            R.string.smartcard_no_cert_dialog_message);
                    request.cancel();
                    onGetSessionFinished();
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
                                mTelemetryHelper.setResultFailure(USER_CANCEL_MESSAGE);
                                request.cancel();
                            }
                        });

                onGetSessionFinished();
            }

            @Override
            public void onException(@NonNull final Exception e) {
                indicateGeneralException(methodTag, e);
                request.cancel();
            }
        });
        return null;
    }

    /**
     * Additional logic to run upon before the end of processChallenge.
     */
    protected abstract void onGetSessionFinished();

    /**
     * Helper method to log and show an error dialog with messages indicating that
     *  too many failed login attempts have occurred.
     * @param methodTag tag from calling method.
     */
    protected void indicateTooManyFailedAttempts(@NonNull final String methodTag) {
        Logger.info(methodTag,  MAX_ATTEMPTS_MESSAGE);
        mTelemetryHelper.setResultFailure(MAX_ATTEMPTS_MESSAGE);
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
    protected void indicateGeneralException(@NonNull final String methodTag, @NonNull final Exception e) {
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
                //Need to prompt user for pin and verify pin. The positive button listener will handle the rest of the CBA flow.
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
        mSmartcardCertBasedAuthManager.initBeforeProceedingWithRequest(mTelemetryHelper);
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
    protected void clearPin(@NonNull final char[] pin) {
        Arrays.fill(pin, Character.MIN_VALUE);
    }

    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull final RawAuthorizationResult response) {
        if (!mIsSmartcardCertBasedAuthProceeding) {
            return;
        }
        final RawAuthorizationResult.ResultCode resultCode = response.getResultCode();
        if (resultCode == RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR
                || resultCode == RawAuthorizationResult.ResultCode.SDK_CANCELLED
                || resultCode == RawAuthorizationResult.ResultCode.CANCELLED) {
            final BaseException exception = response.getException();
            if (exception != null) {
                mTelemetryHelper.setResultFailure(exception);
                return;
            }
            //Putting result code as message.
            mTelemetryHelper.setResultFailure(resultCode.toString());
            return;
        }
        mTelemetryHelper.setResultSuccess();
    }

    /**
     * Clean up logic to run when ICertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {
        mDialogHolder.dismissDialog();
        //Reset IConnectionCallback local variable of mSmartcardCertBasedAuthManager.
        mSmartcardCertBasedAuthManager.setConnectionCallback(null);
    }
}
