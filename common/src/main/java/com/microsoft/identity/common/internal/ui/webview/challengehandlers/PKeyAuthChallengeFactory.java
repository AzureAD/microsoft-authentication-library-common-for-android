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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.IDeviceCertificate;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;
import static com.microsoft.identity.common.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

/**
 * Factory method to get new PKeyAuthChallenge object.
 */
public class PKeyAuthChallengeFactory {
    private static final String TAG = PKeyAuthChallengeFactory.class.getSimpleName();
    /**
     * Certificate authorities are passed with delimiter.
     */
    private static final String CHALLENGE_REQUEST_CERT_AUTH_DELIMITER = ";";

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
    public PKeyAuthChallenge getPKeyAuthChallenge(@NonNull final String redirectUri) throws ClientException {
        //get the PKeyAuthChallenge from redirect Uri sent from authorization endpoint
        final HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        validatePKeyAuthChallenge(parameters);

        final PKeyAuthChallenge.Builder builder = new PKeyAuthChallenge.Builder();
        builder.setNonce(parameters.get(PKeyAuthChallengeHandler.RequestField.Nonce.name().toLowerCase(Locale.US)))
                .setContext(parameters.get(PKeyAuthChallengeHandler.RequestField.Context.name()))
                .setCertAuthorities(StringExtensions.getStringTokens(
                        parameters.get(PKeyAuthChallengeHandler.RequestField.CertAuthorities.name()),
                        CHALLENGE_REQUEST_CERT_AUTH_DELIMITER))
                .setVersion(parameters.get(PKeyAuthChallengeHandler.RequestField.Version.name()))
                .setSubmitUrl(parameters.get(PKeyAuthChallengeHandler.RequestField.SubmitUrl.name()));

        return builder.build();
    }

    /**
     * Create the pkeyauth challenge with headers.
     *
     * @param header
     * @param authority
     * @throws ClientException
     * @throws UnsupportedEncodingException
     */
    public PKeyAuthChallenge getPKeyAuthChallenge(@NonNull final String header, @NonNull final String authority)
            throws ClientException, UnsupportedEncodingException {
        //get the PKeyAuthChallenge from http response headers sent from token endpoint
        validateHeaderForPkeyAuthChallenge(header);
        final Map<String, String> headerItems = getPKeyAuthHeader(header);
        validatePKeyAuthChallenge(headerItems);

        final PKeyAuthChallenge.Builder builder = new PKeyAuthChallenge.Builder();
        builder.setSubmitUrl(authority)
                .setNonce(headerItems.get(PKeyAuthChallengeHandler.RequestField.Nonce.name().toLowerCase(Locale.US)))
                .setVersion(headerItems.get(PKeyAuthChallengeHandler.RequestField.Version.name()))
                .setContext(headerItems.get(PKeyAuthChallengeHandler.RequestField.Context.name()));

        // When pkeyauth header is present, ADFS is always trying to device auth. When hitting token endpoint(device
        // challenge will be returned via 401 challenge), ADFS is sending back an empty cert thumbprint when they found
        // the device is not managed. To account for the behavior of how ADFS performs device auth, below code is checking
        // if it's already workplace joined before checking the existence of cert thumbprint or authority from returned challenge.
        if (!isWorkplaceJoined()) {
            Logger.info(TAG, "Device is not workplace joined. ");
        } else if (!StringExtensions.isNullOrBlank(headerItems.get(PKeyAuthChallengeHandler.RequestField.CertThumbprint.name()))) {
            Logger.info(TAG, "CertThumbprint exists in the device auth challenge.");
            builder.setThumbprint(headerItems.get(PKeyAuthChallengeHandler.RequestField.CertThumbprint.name()));
        } else if (headerItems.containsKey(PKeyAuthChallengeHandler.RequestField.CertAuthorities.name())) {
            Logger.info(TAG, "CertAuthorities exists in the device auth challenge.");
            String authorities = headerItems.get(PKeyAuthChallengeHandler.RequestField.CertAuthorities.name());
            builder.setCertAuthorities(StringExtensions.getStringTokens(authorities,
                    CHALLENGE_REQUEST_CERT_AUTH_DELIMITER));
        } else {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "Both certThumbprint and cert authorities are not present");
        }

        return builder.build();
    }

    private boolean isWorkplaceJoined() {
        @SuppressWarnings("unchecked")
        Class<IDeviceCertificate> certClass = (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE.getDeviceCertificateProxy();
        return certClass != null;
    }

    private void validateHeaderForPkeyAuthChallenge(@NonNull final String header) throws ClientException {
        if (StringUtil.isEmpty(header)) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "header value is empty.");
        }

        // Header value should start with correct challenge type
        if (!StringExtensions.hasPrefixInHeader(header, CHALLENGE_RESPONSE_TYPE)) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "challenge response type is wrong.");
        }
    }

    private void validatePKeyAuthChallenge(Map<String, String> headerItems) throws
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

    private Map<String, String> getPKeyAuthHeader(final String headerStr) throws ClientException, UnsupportedEncodingException {
        final String authenticateHeader = headerStr.substring(CHALLENGE_RESPONSE_TYPE.length());
        final ArrayList<String> queryPairs = StringExtensions.splitWithQuotes(authenticateHeader, ',');
        Map<String, String> headerItems = new HashMap<>();

        for (String queryPair : queryPairs) {
            ArrayList<String> pair = StringExtensions.splitWithQuotes(queryPair, '=');
            if (pair.size() == 2 && !StringExtensions.isNullOrBlank(pair.get(0))
                    && !StringExtensions.isNullOrBlank(pair.get(1))) {
                String key = pair.get(0);
                String value = pair.get(1);
                key = StringExtensions.urlFormDecode(key);
                value = StringExtensions.urlFormDecode(value);
                key = key.trim();
                value = StringExtensions.removeQuoteInHeaderValue(value.trim());
                headerItems.put(key, value);
            } else if (pair.size() == 1 && !StringExtensions.isNullOrBlank(pair.get(0))) {
                // The value list could be null when either no certificate or no permission
                // for ADFS service account for the Device container in AD.
                headerItems.put(StringExtensions.urlFormDecode(pair.get(0)).trim(), StringExtensions.urlFormDecode(""));
            } else {
                // invalid format
                throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, authenticateHeader);
            }
        }

        return headerItems;
    }
}
