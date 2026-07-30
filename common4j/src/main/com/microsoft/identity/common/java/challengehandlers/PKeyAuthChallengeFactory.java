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
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;

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
     * Scheme required for a PKeyAuth {@code SubmitUrl} received over an untrusted WebView redirect.
     */
    private static final String HTTPS_SCHEME = "https";

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     *
     * This is retrieved from response from auth endpoint
     * (read: it would be triggered in interactive flow only).
     *
     * <p>Because the redirect (and therefore the {@code SubmitUrl} it carries) is fully
     * attacker-controllable — any page rendered in the auth WebView can emit a
     * {@code urn:http-auth:PKeyAuth?...} navigation — the {@code SubmitUrl} is validated against
     * {@code challengingUrl} before a challenge object is constructed. Validation happens here, at
     * construction, so a rejected challenge never becomes a {@link PKeyAuthChallenge}: the device key
     * is never used to sign and the response is never submitted (CWE-918 / SSRF hardening,
     * AB#3706623). The {@code SubmitUrl} must be an absolute HTTPS URL whose host equals the host of
     * {@code challengingUrl} (same-origin). The sibling token-endpoint path
     * ({@link #getPKeyAuthChallengeFromTokenEndpointResponse}) is not affected: it derives the submit
     * URL from the caller's trusted authority rather than from the wire.
     *
     * @param redirectUri   Location: urn:http-auth:CertAuth?
     *                      Nonce=[nonce value]
     *                      {@literal &}CertAuthorities=[distinguished names of CAs]
     *                      {@literal &}Version=1.0
     *                      {@literal &}SubmitUrl=[URL to submit response]
     *                      {@literal &}Context=[server state thatclient must convey back]
     * @param challengingUrl The trusted URL of the endpoint that issued this challenge (the WebView's
     *                       current navigation origin). Used as the same-origin reference for
     *                       {@code SubmitUrl}. When {@code null}/blank or unparseable the challenge is
     *                       rejected (fail closed), because same-origin cannot then be proven.
     * @return Return PKeyAuth challenge object
     * @throws ClientException if a required field is missing, or {@code SubmitUrl} is not an absolute
     *                         HTTPS URL that is same-origin with {@code challengingUrl}.
     */
    public PKeyAuthChallenge getPKeyAuthChallengeFromWebViewRedirect(@NonNull final String redirectUri,
                                                                     @Nullable final String challengingUrl) throws ClientException {
        //get the PKeyAuthChallenge from redirect Uri sent from authorization endpoint
        final Map<String, String> parameters = UrlUtil.getParameters(redirectUri);
        validatePKeyAuthChallengeFromWebViewRedirect(parameters);
        // SubmitUrl arrives verbatim from an untrusted redirect; require it to be an absolute HTTPS
        // URL that is same-origin with the challenging endpoint before we ever sign or submit.
        validateSubmitUrlOrigin(parameters.get(SubmitUrl.name()), challengingUrl);

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

    /**
     * Enforces that a {@code SubmitUrl} received over an untrusted WebView redirect is safe to sign
     * for and submit to. The rules are:
     * <ul>
     *     <li>{@code SubmitUrl} must be an absolute HTTPS URL with a host (no cleartext, no
     *         scheme-relative or opaque values).</li>
     *     <li>Its host must equal (case-insensitively) the host of {@code challengingUrl}, i.e. the
     *         submission must go back to the same origin that issued the challenge.</li>
     * </ul>
     * Same-origin is the protocol-correct constraint for PKeyAuth device authentication and works for
     * both AAD and ADFS on-prem, where the challenging endpoint and the submission endpoint share a
     * host. If {@code challengingUrl} cannot be resolved to a host the challenge is rejected (fail
     * closed) rather than accepted, since same-origin cannot otherwise be proven.
     *
     * <p>Enforcement is gated by the {@link CommonFlight#ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION}
     * flight (default on). When the flight is disabled via ECS this method is a no-op and the
     * pre-fix behavior (no scheme/origin enforcement) is preserved exactly — a kill-switch for the
     * unlikely case that a federation topology defeats origin derivation and legitimate device auth
     * is rejected.
     *
     * <p>Only the host is compared: both the challenging and submission endpoints use the default
     * HTTPS port, so a port comparison would add no security while risking false rejections. On any
     * rejection a value-free warning is logged to the primary channel; a companion PII-channel
     * warning ({@link Logger#warnPII}) carries the disagreeing <em>hosts only</em> — never the header,
     * signed JWT, nonce, {@code Context}, or full {@code SubmitUrl} — so an on-call engineer can tell a
     * genuine block from an origin-derivation bug. A {@link ClientException} is thrown before the
     * challenge object exists, so the device key is never exercised on a rejected challenge.
     *
     * <p>Note on {@code CertAuthorities}: that field is also attacker-suppliable from the same
     * redirect, but same-origin enforcement means any resulting signed assertion can only be
     * delivered back to the legitimate challenging origin, so it is intentionally left unvalidated
     * here.
     *
     * @param submitUrl      the {@code SubmitUrl} value taken verbatim from the redirect.
     * @param challengingUrl the trusted origin that issued the challenge; may be {@code null}/blank.
     * @throws ClientException if {@code submitUrl} is not an absolute HTTPS URL, if the origin cannot
     *                         be resolved, or if the hosts differ.
     */
    private void validateSubmitUrlOrigin(@Nullable final String submitUrl,
                                         @Nullable final String challengingUrl) throws ClientException {
        final String methodTag = TAG + ":validateSubmitUrlOrigin";

        if (!CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION)) {
            // Kill-switch OFF: skip enforcement entirely and preserve the historical behavior.
            Logger.info(methodTag,
                    "PKeyAuth SubmitUrl origin validation is disabled via flight; skipping enforcement.");
            return;
        }

        final URL submitUri = parseAbsoluteUri(submitUrl);
        if (submitUri == null
                || !HTTPS_SCHEME.equalsIgnoreCase(submitUri.getProtocol())
                || StringUtil.isNullOrEmpty(submitUri.getHost())) {
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: SubmitUrl is not an absolute HTTPS URL.");
            Logger.warnPII(methodTag,
                    "PKeyAuth SubmitUrl rejected (not absolute HTTPS). scheme="
                            + (submitUri == null ? "<unparseable>" : submitUri.getProtocol())
                            + " host="
                            + (submitUri == null || submitUri.getHost() == null ? "<none>" : submitUri.getHost()));
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "PKeyAuth SubmitUrl must be an absolute HTTPS URL.");
        }

        final URL originUri = parseAbsoluteUri(challengingUrl);
        if (originUri == null || StringUtil.isNullOrEmpty(originUri.getHost())) {
            // Fail closed: without a resolvable challenging origin we cannot prove same-origin.
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: the challenging origin could not be determined.");
            Logger.warnPII(methodTag,
                    "PKeyAuth challenge rejected: challenging origin unresolvable. submitHost="
                            + submitUri.getHost());
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "PKeyAuth challenging origin is unavailable; cannot validate SubmitUrl.");
        }

        if (!submitUri.getHost().equalsIgnoreCase(originUri.getHost())) {
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: SubmitUrl host is not same-origin with the challenging endpoint.");
            Logger.warnPII(methodTag,
                    "PKeyAuth SubmitUrl host mismatch. challengingHost=" + originUri.getHost()
                            + " submitHost=" + submitUri.getHost());
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "PKeyAuth SubmitUrl host does not match the challenging endpoint host.");
        }
    }

    /**
     * Parses {@code url} into an absolute {@link URL}, returning {@code null} when it is blank or
     * malformed. Never throws, so callers can treat every failure mode uniformly as "reject".
     *
     * <p>{@link URL} is used rather than {@link java.net.URI} deliberately: {@code URI.getHost()}
     * returns {@code null} for an otherwise-valid host that contains an underscore (e.g. an on-prem
     * ADFS deployment at {@code adfs_server.contoso.com}), because such a host fails the RFC&nbsp;1123
     * server-based-authority production and {@code URI} falls back to registry-based parsing with a
     * null host. That would cause a legitimate same-origin challenge to be rejected here. {@code URL}
     * preserves the host verbatim. A {@code URL} is inherently absolute and its construction throws
     * {@link MalformedURLException} for an unknown protocol (covering the {@code urn:} and relative
     * cases), so no separate absolute/relative check is needed; the HTTPS scheme is still enforced
     * explicitly by the caller via {@link URL#getProtocol()}.
     *
     * @param url the string to parse; may be {@code null}.
     * @return the parsed {@link URL}, or {@code null} if it is blank or unparseable.
     */
    @Nullable
    private URL parseAbsoluteUri(@Nullable final String url) {
        if (StringUtil.isNullOrEmpty(url)) {
            return null;
        }
        try {
            return new URL(url);
        } catch (final MalformedURLException e) {
            return null;
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
