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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

public class PKeyAuthChallenge implements Serializable {
    /**
     * Certificate authorities are passed with delimiter.
     */
    private static final String CHALLENGE_REQUEST_CERT_AUTH_DELIMITER = ";";
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 1035116074451575588L;

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

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     *
     * @param redirectUri Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *                    &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *                    &SubmitUrl=<URL to submit response>&Context=<server state that
     *                    client must convey back>
     * @return Return PKeyAuth challenge object
     */
    public PKeyAuthChallenge(@NonNull final String redirectUri) throws ClientException {
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        validatePKeyAuthChallenge(parameters);
        mNonce = parameters.get(PKeyAuthChallengeHandler.RequestField.Nonce.name().toLowerCase(Locale.US));
        mContext = parameters.get(PKeyAuthChallengeHandler.RequestField.Context.name());
        mCertAuthorities = StringExtensions.getStringTokens(
                parameters.get(PKeyAuthChallengeHandler.RequestField.CertAuthorities.name()),
                CHALLENGE_REQUEST_CERT_AUTH_DELIMITER);
        mThumbprint = null;
        mVersion = parameters.get(PKeyAuthChallengeHandler.RequestField.Version.name());
        mSubmitUrl = parameters.get(PKeyAuthChallengeHandler.RequestField.SubmitUrl.name());
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

    private static void validatePKeyAuthChallenge(Map<String, String> headerItems) throws
            ClientException {
        if (!(headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.Nonce.name()) || headerItems
                .containsKey(PKeyAuthChallengeHandler.RequestField.Nonce.name().toLowerCase(Locale.US)))) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce is empty.");
        }
        if (!headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.Version.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Version name is empty");
        }
        if (!headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.SubmitUrl.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "SubmitUrl is empty");
        }
        if (!headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.Context.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Context is empty");
        }
        if (!headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.CertAuthorities.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "CertAuthorities is empty");
        }
    }
}