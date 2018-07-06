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
package com.microsoft.identity.common.internal.ui.embeddedwebview.challengehandlers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.support.annotation.NonNull;
import android.webkit.ClientCertRequest;

import com.microsoft.identity.common.internal.logging.Logger;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public final class ClientCertAuthChallengeHandler implements IChallengeHandler {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private ClientCertRequest mClientCertRequest;
    private Activity mActivity;

    public ClientCertAuthChallengeHandler(@NonNull final ClientCertRequest request,
                                          @NonNull final Activity activity) {
        mClientCertRequest = request;
        mActivity = activity;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void process() {
        final Principal[] acceptableCertIssuers = mClientCertRequest.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains("CN=MS-Organization-Access")) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    Logger.verbose(TAG, "Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.");
                    mClientCertRequest.cancel();
                    return;
                }
            }
        }

        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
                    @Override
                    public void alias(String alias) {
                        if (alias == null) {
                            Logger.verbose(TAG, "No certificate chosen by user, cancelling the TLS request.");
                            mClientCertRequest.cancel();
                            return;
                        }

                        try {
                            final X509Certificate[] certChain = KeyChain.getCertificateChain(
                                    mActivity.getApplicationContext(), alias);
                            final PrivateKey privateKey = KeyChain.getPrivateKey(
                                    mActivity, alias);

                            Logger.verbose(TAG, "Certificate is chosen by user, proceed with TLS request.");
                            mClientCertRequest.proceed(privateKey, certChain);
                            return;
                        } catch (final KeyChainException e) {
                            Logger.errorPII(TAG, "KeyChain exception", e);
                        } catch (final InterruptedException e) {
                            Logger.errorPII(TAG, "InterruptedException exception", e);
                        }

                        mClientCertRequest.cancel();
                    }
                },
                mClientCertRequest.getKeyTypes(),
                mClientCertRequest.getPrincipals(),
                mClientCertRequest.getHost(),
                mClientCertRequest.getPort(),
                null);
    }
}