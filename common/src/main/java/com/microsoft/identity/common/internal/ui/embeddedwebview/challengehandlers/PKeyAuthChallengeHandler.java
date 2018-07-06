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

import android.content.Intent;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.IDeviceCertificate;
import com.microsoft.identity.common.adal.internal.JWSBuilder;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

public final class PKeyAuthChallengeHandler implements IChallengeHandler {
    private static final String TAG = PKeyAuthChallengeHandler.class.getSimpleName();
    /**
     * Certificate authorities are passed with delimiter.
     */
    private static final String CHALLENGE_REQUEST_CERT_AUTH_DELIMITER = ";";
    private WebView mWebView;
    private String mRedirectUri;
    private AuthorizationRequest mRequest;
    private IChallengeCompletionCallback mChallengeCallback;

    /**
     * @param redirectUri        Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *                           &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *                           &SubmitUrl=<URL to submit response>&Context=<server state that
     *                           client must convey back>
     * @param view
     * @param completionCallback
     * @throws ClientException
     */
    public PKeyAuthChallengeHandler(@NonNull final String redirectUri,
                                    @NonNull final WebView view,
                                    @NonNull final AuthorizationRequest request,
                                    @NonNull IChallengeCompletionCallback completionCallback) {
        mWebView = view;
        mRedirectUri = redirectUri;
        mRequest = request;
        mChallengeCallback = completionCallback;
    }

    public void process() {
        mWebView.stopLoading();
        mChallengeCallback.setPKeyAuthStatus(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final PkeyAuthChallengeResponse challengeResponse = getChallengeResponseFromUri();
                    final Map<String, String> headers = new HashMap<>();
                    headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                            challengeResponse.getAuthorizationHeaderValue());
                    mWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            String loadUrl = challengeResponse.getSubmitUrl();
                            Logger.verbose(TAG, "Respond to pkeyAuth challenge");
                            Logger.verbosePII(TAG, "Challenge submit url:" + challengeResponse.getSubmitUrl());

                            mWebView.loadUrl(loadUrl, headers);
                        }
                    });
                } catch (final ClientException e) {
                    // It should return error code and finish the
                    // activity, so that onActivityResult implementation
                    // returns errors to callback.
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, e);
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO, mRequest);
                    mChallengeCallback.sendResponse(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION, resultIntent);
                }
            }
        }).start();
    }

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     *
     * @return Return Device challenge response
     */
    private PkeyAuthChallengeResponse getChallengeResponseFromUri() throws ClientException {
        final PkeyAuthChallengeRequest challengeRequest = getChallengeRequestFromUri(mRedirectUri);

        //Get no device cert response
        PkeyAuthChallengeResponse response = new PkeyAuthChallengeResponse();
        response.mSubmitUrl = challengeRequest.mSubmitUrl;
        response.mAuthorizationHeaderValue = String.format("%s Context=\"%s\",Version=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, challengeRequest.mContext,
                challengeRequest.mVersion);

        // If not device cert exists, alias or privatekey will not exist on the device
        Class<IDeviceCertificate> certClazz = (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        if (certClazz != null) {

            IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
            if (deviceCertProxy.isValidIssuer(challengeRequest.mCertAuthorities)
                    || deviceCertProxy.getThumbPrint() != null && deviceCertProxy.getThumbPrint()
                    .equalsIgnoreCase(challengeRequest.mThumbprint)) {
                RSAPrivateKey privateKey = deviceCertProxy.getRSAPrivateKey();
                if (privateKey == null) {
                    throw new ClientException(ErrorStrings.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
                }
                String jwt = (new JWSBuilder()).generateSignedJWT(challengeRequest.mNonce, challengeRequest.mSubmitUrl,
                        privateKey, deviceCertProxy.getRSAPublicKey(),
                        deviceCertProxy.getCertificate());
                response.mAuthorizationHeaderValue = String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, jwt,
                        challengeRequest.mContext, challengeRequest.mVersion);
                Logger.verbosePII(TAG, "Receive challenge response. ",
                        "Challenge response:" + response.mAuthorizationHeaderValue);
            }
        }

        return response;
    }

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     *
     * @param redirectUri Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *                    &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *                    &SubmitUrl=<URL to submit response>&Context=<server state that
     *                    client must convey back>
     * @return Return PKeyAuth challenge response
     */
    private PkeyAuthChallengeRequest getChallengeRequestFromUri(
            @NonNull final String redirectUri)
            throws ClientException {
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        validateChallengeRequest(parameters);
        final PkeyAuthChallengeRequest challenge = new PkeyAuthChallengeRequest(
                parameters.get(RequestField.Nonce.name().toLowerCase(Locale.US)),
                parameters.get(RequestField.Context.name()),
                StringExtensions.getStringTokens(
                        parameters.get(RequestField.CertAuthorities.name()),
                        CHALLENGE_REQUEST_CERT_AUTH_DELIMITER),
                null,
                parameters.get(RequestField.Version.name()),
                parameters.get(RequestField.SubmitUrl.name()));
        return challenge;
    }

    private static void validateChallengeRequest(Map<String, String> headerItems) throws
            ClientException {
        if (!(headerItems.containsKey(RequestField.Nonce.name()) || headerItems
                .containsKey(RequestField.Nonce.name().toLowerCase(Locale.US)))) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce is empty.");
        }
        if (!headerItems.containsKey(RequestField.Version.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Version name is empty");
        }
        if (!headerItems.containsKey(RequestField.SubmitUrl.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "SubmitUrl is empty");
        }
        if (!headerItems.containsKey(RequestField.Context.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Context is empty");
        }
        if (!headerItems.containsKey(RequestField.CertAuthorities.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "CertAuthorities is empty");
        }
    }

    private IDeviceCertificate getWPJAPIInstance(Class<IDeviceCertificate> certClazz)
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

    class PkeyAuthChallengeRequest {
        private String mNonce = "";

        private String mContext = "";

        /**
         * Authorization endpoint will return accepted authorities.
         * The mCertAuthorities could be empty when either no certificate or no permission for ADFS
         * service account for the Device container in AD.
         */
        private List<String> mCertAuthorities;

        /**
         * Token endpoint will return thumbprint.
         */
        private String mThumbprint = "";

        private String mVersion = null;

        private String mSubmitUrl = "";

        PkeyAuthChallengeRequest(final String nonce,
                                 final String context,
                                 final List<String> certAuthorities,
                                 final String thumbprint,
                                 final String version,
                                 final String submitUrl) {
            mNonce = nonce;
            mContext = context;
            mCertAuthorities = certAuthorities;
            mThumbprint = thumbprint;
            mVersion = version;
            mSubmitUrl = submitUrl;
        }
    }

    class PkeyAuthChallengeResponse {
        private String mSubmitUrl;

        private String mAuthorizationHeaderValue;

        String getSubmitUrl() {
            return mSubmitUrl;
        }

        void setSubmitUrl(String submitUrl) {
            mSubmitUrl = submitUrl;
        }

        String getAuthorizationHeaderValue() {
            return mAuthorizationHeaderValue;
        }

        void setAuthorizationHeaderValue(String authorizationHeaderValue) {
            mAuthorizationHeaderValue = authorizationHeaderValue;
        }
    }

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint
    }
}