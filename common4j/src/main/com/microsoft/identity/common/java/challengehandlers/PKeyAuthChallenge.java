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
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;

import com.microsoft.identity.common.java.AuthenticationSettings;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.JWSBuilder;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A class/builder that represents PKeyAuth challenge.
 * see {@link PKeyAuthChallengeFactory}.
 *
 * Spec: https://microsoft.sharepoint.com/teams/aad/devex/Shared%20Documents/Core%20Design/Device%20authentication%20using%20PKeyAuth.docx?web=1
 * */
@Builder
@Getter
@Accessors(prefix = "m")
public class PKeyAuthChallenge {
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
    @Nullable
    private final List<String> mCertAuthorities;

    /**
     * The thumbprint of the device certificate that the client MUST present in order to complete proof of possession.
     * This is used by the client in order to determine the right certificate to be presented by the client in order to perform device authentication.
     */
    @Nullable
    private final String mThumbprint;

    /**
     * The version number of this challenge response protocol.
     */
    private final String mVersion;

    /**
     * The url to submit PKeyAuth response to.
     */
    private final String mSubmitUrl;

    @Builder.Default
    private final JWSBuilder mJwsBuilder = new JWSBuilder();

    /**
     * Home tenant ID of the account that is being challenged.
     */
    @Nullable
    private final String mTenantId;

    /**
     * Generate a header to be returned with the PKeyAuth Response.
     */
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

        final IDeviceCertificateLoader certificateLoader = AuthenticationSettings.INSTANCE.getCertificateLoader();
        if (certificateLoader == null) {
            Logger.warn(TAG + methodName, "Device Certificate loader is not initialized.");
            return getChallengeHeaderWithoutSignedJwt();
        }

        final IDeviceCertificate deviceCertProxy = certificateLoader.loadCertificate(mTenantId);
        if (deviceCertProxy == null) {
            Logger.warn(TAG + methodName, "Device Certificate not found.");
            return getChallengeHeaderWithoutSignedJwt();
        }

        if (deviceCertProxy.isValidIssuer(mCertAuthorities)){
            Logger.info(TAG + methodName,
                    "Found a certificate matching the provided authority.");
            return getChallengeHeaderWithSignedJwt(deviceCertProxy);
        }
        //Note that we aren't validating thumbprint hints anymore, which is in line with the iOS team.

        return getChallengeHeaderWithoutSignedJwt();
    }

    /**
     * Returns a "we can do nothing" response to the server.
     */
    private Map<String, String> getChallengeHeaderWithoutSignedJwt() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(CHALLENGE_RESPONSE_HEADER,
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE, mContext, PKEYAUTH_VERSION));
        return headers;
    }

    /**
     * Generates a signed jwt and return it as part of the challenge header.
     */
    private Map<String, String> getChallengeHeaderWithSignedJwt(@NonNull final IDeviceCertificate deviceCertProxy) throws ClientException {
        final String methodName = ":getChallengeHeaderWithSignedJwt";

        // This should NEVER happen, but things might change in the future.
        if (!StringUtil.equalsIgnoreCase(mVersion, PKEYAUTH_VERSION)) {
            Logger.warn(TAG + methodName,
                    "PKeyAuth version mismatch, server provides: " + mVersion +
                            "We support: " + PKEYAUTH_VERSION +
                            "Proceed anyway with " + PKEYAUTH_VERSION
                    );
        }

        final String jwt = mJwsBuilder.generateSignedJWT(
                mNonce,
                mSubmitUrl,
                deviceCertProxy);

        Logger.info(TAG + methodName, "Generated a signed challenge response.");

        final Map<String, String> headers = new HashMap<>();
        headers.put(CHALLENGE_RESPONSE_HEADER,
                String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE, jwt, mContext, PKEYAUTH_VERSION));
        return headers;
    }
}
