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

import android.content.Intent;
import androidx.annotation.NonNull;
import android.webkit.WebView;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.IDeviceCertificate;
import com.microsoft.identity.common.adal.internal.JWSBuilder;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

public final class PKeyAuthChallengeHandler implements IChallengeHandler<PKeyAuthChallenge, Void> {
    private static final String TAG = PKeyAuthChallengeHandler.class.getSimpleName();
    private WebView mWebView;
    private IAuthorizationCompletionCallback mChallengeCallback;

    /**
     * @param view
     * @param completionCallback
     */
    public PKeyAuthChallengeHandler(@NonNull final WebView view,
                                    @NonNull IAuthorizationCompletionCallback completionCallback) {
        mWebView = view;
        mChallengeCallback = completionCallback;
    }

    @Override
    public Void processChallenge(final PKeyAuthChallenge pKeyAuthChallenge) {
        mWebView.stopLoading();
        mChallengeCallback.setPKeyAuthStatus(true);

        try {
            //Get no device cert response
            final Map<String, String> header = getChallengeHeader(pKeyAuthChallenge);

            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    String loadUrl = pKeyAuthChallenge.getSubmitUrl();
                    Logger.info(TAG, "Respond to pkeyAuth challenge");
                    Logger.infoPII(TAG, "Challenge submit url:" + pKeyAuthChallenge.getSubmitUrl());

                    mWebView.loadUrl(loadUrl, header);
                }
            });
        } catch (final ClientException e) {
            // It should return error code and finish the
            // activity, so that onActivityResult implementation
            // returns errors to callback.
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, e);
            //TODO log the request info
            mChallengeCallback.onChallengeResponseReceived(
                    AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                    resultIntent
            );
        }

        return null;
    }

    public static Map<String, String> getChallengeHeader(final PKeyAuthChallenge pKeyAuthChallenge) throws ClientException {
        String authorizationHeaderValue = String.format("%s Context=\"%s\",Version=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, pKeyAuthChallenge.getContext(),
                pKeyAuthChallenge.getVersion());

        // If not device cert exists, alias or private key will not exist on the device
        Class<IDeviceCertificate> certClazz = (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        if (certClazz != null) {
            IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
            if (deviceCertProxy.isValidIssuer(pKeyAuthChallenge.getCertAuthorities())
                    || deviceCertProxy.getThumbPrint() != null && deviceCertProxy.getThumbPrint()
                    .equalsIgnoreCase(pKeyAuthChallenge.getThumbprint())) {
                RSAPrivateKey privateKey = deviceCertProxy.getRSAPrivateKey();
                if (privateKey == null) {
                    throw new ClientException(ErrorStrings.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
                }
                final String jwt = (new JWSBuilder()).generateSignedJWT(
                        pKeyAuthChallenge.getNonce(),
                        pKeyAuthChallenge.getSubmitUrl(),
                        privateKey,
                        deviceCertProxy.getRSAPublicKey(),
                        deviceCertProxy.getCertificate());
                authorizationHeaderValue = String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, jwt,
                        pKeyAuthChallenge.getContext(), pKeyAuthChallenge.getVersion());
                Logger.info(TAG, "Receive challenge response. ");
            }
        }

        final Map<String, String> headers = new HashMap<>();
        headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                authorizationHeaderValue);
        return headers;
    }

    private static IDeviceCertificate getWPJAPIInstance(Class<IDeviceCertificate> certClazz)
            throws ClientException {
        final IDeviceCertificate deviceCertProxy;
        final Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificate) constructor.newInstance((Object[]) null);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new ClientException(ErrorStrings.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        }
        return deviceCertProxy;
    }

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint
    }
}