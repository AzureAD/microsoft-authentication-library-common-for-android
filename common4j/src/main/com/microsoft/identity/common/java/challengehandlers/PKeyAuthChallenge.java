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
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import sun.rmi.runtime.Log;

/**
 * A class/builder that represents PKeyAuth challenge.
 * see {@link PKeyAuthChallengeFactory}.
 *
 * Spec: https://microsoft.sharepoint.com/teams/aad/devex/Shared%20Documents/Core%20Design/Device%20authentication%20using%20PKeyAuth.docx?web=1
 * */
@Builder
@Getter
@Accessors(prefix = "m")
public class PKeyAuthChallenge implements Serializable {
    private static final String TAG = PKeyAuthChallenge.class.getSimpleName();

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint, TenantId
    }

    /**
     * A challenge code issued to the client. The client is expected to return this nonce value in its response.
     */
    private final String mNonce;

    /**
     * An encrypted blob that is opaque to the client and contains information about the challenge
     * such as the nonce and the timestamp at which the challenge was issued.
     *
     * This is expected to be replayed by the client to the server and provides
     * a way for the server to validate the response
     */
    private final String mContext;

    /**
     * An array of strings consisting of all the trusted certificate issuers for the device certificate –
     * typically this is the DRS.
     * This is used by the client in order to determine the right certificate to be presented
     * by the client in order to perform device authentication.
     *
     * The mCertAuthorities could be empty when either no certificate or no permission for ADFS
     * service account for the Device container in AD.
     */
    private final List<String> mCertAuthorities;

    /**
     * The thumbprint of the device certificate that the client MUST present in order to complete proof of possession.
     * This is used by the client in order to determine the right certificate to be presented by the client in order to perform device authentication.
     */
    private final String mThumbprint;

    /**
     * The version number of this challenge response protocol.
     */
    private final String mVersion;

    private final String mSubmitUrl;

    public Map<String, String> getChallengeHeader() throws ClientException {
        final String methodName = ":getChallengeHeader";

        // Either cert thumbprint or authorities must present
        // Otherwise, we won't have enough information to look for the cert.
        //
        // In such case, Client should ideally ignore this scenario /
        // send a response which is equivalent to no certificate present on client.
        if ((mCertAuthorities == null || mCertAuthorities.size() == 0) &&
                StringUtil.isNullOrEmpty(mThumbprint)) {
            Logger.info(TAG + methodName,
                    "Both cert Authorities and Thumbprint are not provided." +
                            "Sending a response which is equivalent to no certificate present on client.");
            return getChallengeHeaderWithoutSignedJwt();
        }

        // If not device cert exists, alias or private key will not exist on the device
        // Suppressing unchecked warnings due to the generic type not provided in the object returned from method getDeviceCertificateProxy
        @SuppressWarnings(WarningType.unchecked_warning)
        final Class<IDeviceCertificate> certClazz =
                (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE.getDeviceCertificateProxy();

        if (certClazz == null) {
            Logger.warn(TAG + methodName, "Device Certificate Proxy is not initialized");
            return getChallengeHeaderWithoutSignedJwt();
        }

        final IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
        if (!deviceCertProxy.isValidIssuer(mCertAuthorities)
                && !StringUtil.equalsIgnoreCase(deviceCertProxy.getThumbPrint(), mThumbprint)) {
            Logger.info(TAG + methodName,
                    "Cannot find a certificate matching the provided authority.");
            return getChallengeHeaderWithoutSignedJwt();
        }

        return getChallengeHeaderWithSignedJwt(deviceCertProxy);
    }

    /**
     * Returns a "we can do nothing" response to the server.
     */
    private Map<String, String> getChallengeHeaderWithoutSignedJwt() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(CHALLENGE_RESPONSE_HEADER,
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE, mContext, mVersion));
        return headers;
    }

    /**
     * Generates a signed jwt and return it as part of the challenge header.
     */
    private Map<String, String> getChallengeHeaderWithSignedJwt(@NonNull final IDeviceCertificate deviceCertProxy) throws ClientException {
        final String methodName = ":generateChallengeResponse";

        final PrivateKey privateKey = deviceCertProxy.getPrivateKey();
        if (privateKey == null) {
            throw new ClientException(ErrorStrings.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
        }
        final PublicKey publicKey = deviceCertProxy.getPublicKey();
        final X509Certificate certificate = deviceCertProxy.getCertificate();
        if (certificate == null) {
            throw new ClientException(ErrorStrings.KEY_CHAIN_CERTIFICATE_EXCEPTION);
        }
        final String jwt = (new JWSBuilder()).generateSignedJWT(
                mNonce,
                mSubmitUrl,
                privateKey,
                publicKey,
                certificate);

        Logger.info(TAG + methodName, "Generated a signed challenge response.");

        final Map<String, String> headers = new HashMap<>();
        headers.put(CHALLENGE_RESPONSE_HEADER,
                String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE, jwt, mContext, mVersion));
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
