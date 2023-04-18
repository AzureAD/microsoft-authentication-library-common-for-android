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
import android.security.keystore.KeyProperties;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Handles a received ClientCertRequest by prompting the user to choose from certificates
 *  stored on the Android device.
 */
public class OnDeviceCertBasedAuthChallengeHandler extends AbstractCertBasedAuthChallengeHandler {
    private static final String TAG = OnDeviceCertBasedAuthChallengeHandler.class.getSimpleName();
    private static final String ECDSA_CONSTANT = "ECDSA";
    private final Activity mActivity;

    /**
     * Creates new instance of OnDeviceCertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param telemetryHelper CertBasedAuthTelemetryHelder instance.
     */
    public OnDeviceCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                 @NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {
        mActivity = activity;
        mTelemetryHelper = telemetryHelper;
        mTelemetryHelper.setCertBasedAuthChallengeHandler(TAG);
        mIsCertBasedAuthProceeding = false;
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Makes use of Android's KeyChain.choosePrivateKeyAlias method, which shows a certificate picker that allows users to choose their on-device user certificate to authenticate with.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Void processChallenge(ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
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
                            mIsCertBasedAuthProceeding = true;
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
                mapKeyTypes(request.getKeyTypes()),
                request.getPrincipals(),
                request.getHost(),
                request.getPort(),
                null);
        return null;
    }

    /**
     * Clean up logic to run when OnDeviceCertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {
        //Nothing needed at the moment.
    }

    /**
     * Map instances of key types without a literal reference in {@link KeyProperties} to corresponding constants in KeyProperties.
     * @param keyTypes array of key types.
     * @return array of mapped key types.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    public String[] mapKeyTypes(@Nullable final String[] keyTypes) {
        if (keyTypes == null) {
            return null;
        }
        for (int i = 0; i < keyTypes.length; i++) {
            if (keyTypes[i].equals(ECDSA_CONSTANT)) {
                keyTypes[i] = KeyProperties.KEY_ALGORITHM_EC;
                break;
            }
        }
        return keyTypes;
    }
}
