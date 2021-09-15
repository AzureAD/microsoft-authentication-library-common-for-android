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
package com.microsoft.identity.common.java.challengehandlers;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;

import com.microsoft.identity.common.java.AuthenticationSettings;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.JWSBuilder;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A class/builder that represents PKeyAuth challenge.
 * see {@link PKeyAuthChallengeFactory}.
 * */
public class PKeyAuthChallenge implements Serializable {
    private static final String TAG = PKeyAuthChallenge.class.getSimpleName();

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint
    }

    /**
     * Serial version id.
     */

    private final String mNonce;

    private final String mContext;

    /**
     * Authorization endpoint will return accepted authorities.
     * The mCertAuthorities could be empty when either no certificate or no permission for ADFS
     * service account for the Device container in AD.
     */
    private final List<String> mCertAuthorities;

    /**
     * Token endpoint will return thumbprint.
     */
    private final String mThumbprint;

    private final String mVersion;

    private final String mSubmitUrl;

    protected PKeyAuthChallenge(final Builder builder) {
        mNonce = builder.mNonce;
        mContext = builder.mContext;
        mCertAuthorities = builder.mCertAuthorities;
        mThumbprint = builder.mThumbprint;
        mVersion = builder.mVersion;
        mSubmitUrl = builder.mSubmitUrl;
    }

    public static class Builder {
        private String mNonce = "";
        private String mContext = "";
        private List<String> mCertAuthorities;
        private String mThumbprint = "";
        private String mVersion;
        private String mSubmitUrl;

        public Builder setNonce(final String nonce) {
            mNonce = nonce;
            return self();
        }

        public Builder setContext(final String context) {
            mContext = context;
            return self();
        }

        public Builder setCertAuthorities(final List<String> certAuthorities) {
            mCertAuthorities = certAuthorities;
            return self();
        }

        public Builder setThumbprint(final String thumbprint) {
            mThumbprint = thumbprint;
            return self();
        }

        public Builder setVersion(final String version) {
            mVersion = version;
            return self();
        }

        public Builder setSubmitUrl(final String submitUrl) {
            mSubmitUrl = submitUrl;
            return self();
        }

        public Builder self() {
            return this;
        }

        public PKeyAuthChallenge build() {
            return new PKeyAuthChallenge(this);
        }
    }

    public String getNonce() {
        return mNonce;
    }

    public String getContext() {
        return mContext;
    }

    public List<String> getCertAuthorities() {
        return mCertAuthorities;
    }

    public String getThumbprint() {
        return mThumbprint;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getSubmitUrl() {
        return mSubmitUrl;
    }

    public Map<String, String> getChallengeHeader() throws ClientException {
        final String methodName = ":getChallengeHeader";

        String authorizationHeaderValue = String.format("%s Context=\"%s\",Version=\"%s\"",
                CHALLENGE_RESPONSE_TYPE, mContext, mVersion);

        // If not device cert exists, alias or private key will not exist on the device
        // Suppressing unchecked warnings due to the generic type not provided in the object returned from method getDeviceCertificateProxy
        @SuppressWarnings(WarningType.unchecked_warning)
        final Class<IDeviceCertificate> certClazz =
                (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE.getDeviceCertificateProxy();

        if (certClazz != null) {
            IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
            if (deviceCertProxy.isValidIssuer(mCertAuthorities)
                    || StringUtil.equalsIgnoreCase(deviceCertProxy.getThumbPrint(), mThumbprint)) {
                final PrivateKey privateKey = deviceCertProxy.getPrivateKey();
                if (privateKey == null) {
                    throw new ClientException(ErrorStrings.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
                }
                final String jwt = (new JWSBuilder()).generateSignedJWT(
                        mNonce,
                        mSubmitUrl,
                        privateKey,
                        deviceCertProxy.getPublicKey(),
                        deviceCertProxy.getCertificate());
                authorizationHeaderValue = String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE, jwt, mContext, mVersion);
                Logger.info(TAG + methodName, "Receive challenge response. ");
            }
        }

        final Map<String, String> headers = new HashMap<>();
        headers.put(CHALLENGE_RESPONSE_HEADER,
                authorizationHeaderValue);
        return headers;
    }

    private static IDeviceCertificate getWPJAPIInstance(@NonNull final Class<IDeviceCertificate> certClazz)
            throws ClientException {
        final IDeviceCertificate deviceCertProxy;
        final Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificate) constructor.newInstance((Object[]) null);
        } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new ClientException(ErrorStrings.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        }
        return deviceCertProxy;
    }
}
