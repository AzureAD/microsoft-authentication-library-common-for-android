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
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.logging.Logger;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on the Android device.
 */
public class OnDeviceCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    private static final String TAG = OnDeviceCertBasedAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private final Activity mActivity;
    private final CertBasedAuthTelemetryHelper mTelemetryHelper;
    private boolean mIsOnDeviceCertBasedAuthProceeding;

    /**
     * Creates new instance of OnDeviceCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param telemetryHelper CertBasedAuthTelemetryHelder instance.
     */
    public OnDeviceCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                 @NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        mActivity = activity;
        mTelemetryHelper = telemetryHelper;
        mTelemetryHelper.setCertBasedAuthChallengeHandler(TAG);
        mIsOnDeviceCertBasedAuthProceeding = false;
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Makes use of Android's KeyChain.choosePrivateKeyAlias method, which shows a certificate picker that allows users to choose their on-device user certificate to authenticate with.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
        final Principal[] acceptableCertIssuers = request.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (final Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(ACCEPTABLE_ISSUER)) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    final String message = "Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.";
                    Logger.info(methodTag, message);
                    mTelemetryHelper.setResultFailure(message);
                    request.cancel();
                    return null;
                }
            }
        }

        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
                    @Override
                    public void alias(String alias) {
                        if (alias == null) {
                            final String message = "No certificate chosen by user, cancelling the TLS request.";
                            Logger.info(methodTag, message);
                            mTelemetryHelper.setResultFailure(message);
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
                            mTelemetryHelper.setResultFailure(e);
                        } catch (final InterruptedException e) {
                            Logger.errorPII(methodTag,"InterruptedException exception", e);
                            mTelemetryHelper.setResultFailure(e);
                        }
                        mTelemetryHelper.setResultFailure("ClientCertRequest unexpectedly cancelled.");
                        request.cancel();
                    }
                },
                request.getKeyTypes(),
                request.getPrincipals(),
                request.getHost(),
                request.getPort(),
                null);
        return null;
    }

    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     *
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull final RawAuthorizationResult response) {
        if (mIsOnDeviceCertBasedAuthProceeding) {
            final RawAuthorizationResult.ResultCode resultCode = response.getResultCode();
            if (resultCode == RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR
                    || resultCode == RawAuthorizationResult.ResultCode.SDK_CANCELLED
                    || resultCode == RawAuthorizationResult.ResultCode.CANCELLED) {
                final BaseException exception = response.getException();
                if (exception != null) {
                    mTelemetryHelper.setResultFailure(exception);
                } else {
                    //Putting result code as message.
                    mTelemetryHelper.setResultFailure(resultCode.toString());
                }
            } else {
                mTelemetryHelper.setResultSuccess();
            }
        }
    }

    /**
     * Clean up logic to run when ICertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {
        //Nothing needed at the moment.
    }
}
