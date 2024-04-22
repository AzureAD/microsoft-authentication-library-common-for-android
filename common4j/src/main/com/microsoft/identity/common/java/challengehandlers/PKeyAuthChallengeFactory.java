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

import lombok.NonNull;

import com.microsoft.identity.common.java.AuthenticationSettings;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.CertAuthorities;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.CertThumbprint;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.Context;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.Nonce;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.SubmitUrl;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.TenantId;
import static com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge.RequestField.Version;
import static com.microsoft.identity.common.java.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

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
     * This is retrieved from response from auth endpoint
     * (read: it would be triggered in interactive flow only).
     *
     * @param redirectUri Location: urn:http-auth:CertAuth?
     *                    Nonce=[nonce value]
     *                    {@literal &}CertAuthorities=[distinguished names of CAs]
     *                    {@literal &}Version=1.0
     *                    {@literal &}SubmitUrl=[URL to submit response]
     *                    {@literal &}Context=[server state thatclient must convey back]
     * @return Return PKeyAuth challenge object
     */
    public PKeyAuthChallenge getPKeyAuthChallengeFromWebViewRedirect(@NonNull final String redirectUri) throws ClientException {
        //get the PKeyAuthChallenge from redirect Uri sent from authorization endpoint
        final Map<String, String> parameters = UrlUtil.getParameters(redirectUri);
        validatePKeyAuthChallengeFromWebViewRedirect(parameters);

        final PKeyAuthChallenge.PKeyAuthChallengeBuilder builder = new PKeyAuthChallenge.PKeyAuthChallengeBuilder();
        builder.nonce(parameters.get(Nonce.name().toLowerCase(Locale.US)))
                .context(parameters.get(Context.name()))
                .version(parameters.get(Version.name()))
                .submitUrl(parameters.get(SubmitUrl.name()))
                .tenantId(parameters.get(TenantId.name()));

        if (parameters.containsKey(CertAuthorities.name())) {
            final String authorities = parameters.get(CertAuthorities.name());
            builder.certAuthorities(StringUtil.getStringTokens(authorities,
                    CHALLENGE_REQUEST_CERT_AUTH_DELIMITER));
        }

        return builder.build();
    }

    /**
     * Create the pkeyauth challenge with headers.
     *
     * This is retrieved from response from token endpoint
     * (read: it would be triggered in silent flow only).
     */
    public PKeyAuthChallenge getPKeyAuthChallengeFromTokenEndpointResponse(@NonNull final String header, @NonNull final String authority)
            throws ClientException, UnsupportedEncodingException {
        //get the PKeyAuthChallenge from http response headers sent from token endpoint
        validateHeaderForPkeyAuthChallenge(header);
        final Map<String, String> headerItems = getPKeyAuthHeader(header);
        validatePKeyAuthChallengeFromTokenEndpointResponse(headerItems);

        final PKeyAuthChallenge.PKeyAuthChallengeBuilder builder = new PKeyAuthChallenge.PKeyAuthChallengeBuilder();
        builder.submitUrl(authority)
                .nonce(headerItems.get(Nonce.name().toLowerCase(Locale.US)))
                .context(headerItems.get(Context.name()))
                .version(headerItems.get(Version.name()))
                .tenantId(headerItems.get(TenantId.name()));

        // When pkeyauth header is present, ADFS is always trying to device auth. When hitting token endpoint(device
        // challenge will be returned via 401 challenge), ADFS is sending back an empty cert thumbprint when they found
        // the device is not managed. To account for the behavior of how ADFS performs device auth, below code is checking
        // if it's already workplace joined before checking the existence of cert thumbprint or authority from returned challenge.
        if (!StringUtil.isNullOrEmpty(headerItems.get(CertThumbprint.name()))) {
            builder.thumbprint(headerItems.get(CertThumbprint.name()));
        } else if (headerItems.containsKey(CertAuthorities.name())) {
            String authorities = headerItems.get(CertAuthorities.name());
            builder.certAuthorities(StringUtil.getStringTokens(authorities,
                    CHALLENGE_REQUEST_CERT_AUTH_DELIMITER));
        }

        return builder.build();
    }

    private void validateHeaderForPkeyAuthChallenge(@NonNull final String header) throws ClientException {
        if (StringUtil.isNullOrEmpty(header)) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "header value is empty.");
        }

        // Header value should start with correct challenge type
        if (!StringUtil.hasPrefixInHeader(header, CHALLENGE_RESPONSE_TYPE)) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "challenge response type is wrong.");
        }
    }

    // Validated the required fields.
    private void validatePKeyAuthChallengeFromTokenEndpointResponse(Map<String, String> headerItems) throws
            ClientException {
        if (!(headerItems.containsKey(Nonce.name()) || headerItems
                .containsKey(Nonce.name().toLowerCase(Locale.US)))) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce is empty.");
        }
        if (!headerItems.containsKey(Context.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Context is empty");
        }
        if (!headerItems.containsKey(Version.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Version name is empty");
        }
    }

    private void validatePKeyAuthChallengeFromWebViewRedirect(Map<String, String> headerItems) throws
            ClientException {
        if (!(headerItems.containsKey(Nonce.name()) || headerItems
                .containsKey(Nonce.name().toLowerCase(Locale.US)))) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce is empty.");
        }
        if (!headerItems.containsKey(Context.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Context is empty");
        }
        if (!headerItems.containsKey(Version.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "Version name is empty");
        }
        if (!headerItems.containsKey(SubmitUrl.name())) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, "SubmitUrl is empty");
        }
    }

    private Map<String, String> getPKeyAuthHeader(final String headerStr) throws ClientException, UnsupportedEncodingException {
        final String authenticateHeader = headerStr.substring(CHALLENGE_RESPONSE_TYPE.length());
        final ArrayList<String> queryPairs = StringUtil.splitWithQuotes(authenticateHeader, ',');
        Map<String, String> headerItems = new HashMap<>();

        for (final String queryPair : queryPairs) {
            final ArrayList<String> pair = StringUtil.splitWithQuotes(queryPair, '=');
            if (pair.size() == 2 && !StringUtil.isNullOrEmpty(pair.get(0))
                    && !StringUtil.isNullOrEmpty(pair.get(1))) {
                String key = pair.get(0);
                String value = pair.get(1);
                key = StringUtil.urlFormDecode(key);
                value = StringUtil.urlFormDecode(value);
                key = key.trim();
                value = StringUtil.removeQuoteInHeaderValue(value.trim());
                headerItems.put(key, value);
            } else if (pair.size() == 1 && !StringUtil.isNullOrEmpty(pair.get(0))) {
                // The value list could be null when either no certificate or no permission
                // for ADFS service account for the Device container in AD.
                headerItems.put(StringUtil.urlFormDecode(pair.get(0)).trim(), StringUtil.urlFormDecode(""));
            } else {
                // invalid format
                throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, authenticateHeader);
            }
        }

        return headerItems;
    }
}
