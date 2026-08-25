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
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import io.opentelemetry.api.trace.Span;

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
     * AB#3706623). The {@code SubmitUrl} must be an absolute HTTPS URL that is same-origin (equal
     * scheme, host, and port) with {@code challengingUrl}. The sibling token-endpoint path
     * ({@link #getPKeyAuthChallengeFromTokenEndpointResponse}) is not affected: it derives the submit
     * URL from the caller's trusted authority rather than from the wire.
     *
     * @param redirectUri   Location: urn:http-auth:CertAuth?
     *                      Nonce=[nonce value]
     *                      {@literal &}CertAuthorities=[distinguished names of CAs]
     *                      {@literal &}Version=1.0
     *                      {@literal &}SubmitUrl=[URL to submit response]
     *                      {@literal &}Context=[server state that client must convey back]
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
     *     <li>{@code challengingUrl} must itself be an absolute HTTPS URL with a host. A cleartext
     *         {@code http} challenging origin is rejected (fail closed) so it can never authorize an
     *         HTTPS {@code SubmitUrl}.</li>
     *     <li>The two must be same-origin: equal scheme (both HTTPS), equal host (case-insensitive),
     *         and equal port (with the implicit HTTPS port normalized on both sides), i.e. the
     *         submission must go back to the same origin that issued the challenge.</li>
     * </ul>
     * Same-origin is the protocol-correct constraint for PKeyAuth device authentication and works for
     * both AAD and ADFS on-prem, where the challenging endpoint and the submission endpoint share an
     * origin. If {@code challengingUrl} cannot be resolved to an HTTPS URL with a host the challenge is
     * rejected (fail closed) rather than accepted, since same-origin cannot otherwise be proven.
     *
     * <p>Rollout is staged via two flights. The master switch
     * {@link CommonFlight#ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION} (default on) gates the whole
     * feature: with it off this method is a true no-op — no evaluation, no telemetry — and the
     * pre-fix behavior (no scheme/origin enforcement) is preserved exactly. While the master switch
     * is on the verdict is always computed and emitted to telemetry, but the challenge is only
     * <em>blocked</em> when {@link CommonFlight#ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION}
     * (default off) is also on. With enforcement off the method runs in <em>shadow mode</em>: it
     * measures and logs the verdict but does not throw, so real-world origin pairs can be observed
     * before enforcement is ramped. This staging exists because a false reject fails the entire
     * authorization request (the {@code handleUrl} catch turns the {@link ClientException} into
     * {@code returnError} + {@code stopLoading}).
     *
     * <p>Same-origin here means scheme, host, and port must all match. Both endpoints are pinned to
     * HTTPS (a cleartext {@code SubmitUrl} or challenging origin is rejected), so scheme
     * equality is implied. Host is compared case-insensitively. Port is compared after normalizing
     * the implicit HTTPS port: {@code https://host} ({@link URL#getPort()} {@code == -1}) and
     * {@code https://host:443} both resolve to 443 via {@link URL#getDefaultPort()}, so a naive
     * {@code getPort()} comparison must not be substituted here — it would falsely reject that
     * legitimate same-origin pair and, via {@code handleUrl}, fail the entire authorization request.
     * On any rejection a value-free warning is logged to the primary channel (carrying only the
     * {@code enforced=} state and non-PII shape hints); a companion PII-channel warning
     * ({@link Logger#warnPII}) carries the disagreeing <em>hosts (and ports) only</em> — never the
     * header, signed JWT, nonce, {@code Context}, or full {@code SubmitUrl}. When enforcement is on a
     * {@link ClientException} is thrown before the challenge object exists, so the device key is
     * never exercised on a rejected challenge.
     *
     * <p>Note on {@code CertAuthorities}: that field is also attacker-suppliable from the same
     * redirect, but same-origin enforcement means any resulting signed assertion can only be
     * delivered back to the legitimate challenging origin, so it is intentionally left unvalidated
     * here.
     *
     * @param submitUrl      the {@code SubmitUrl} value taken verbatim from the redirect.
     * @param challengingUrl the trusted origin that issued the challenge; may be {@code null}/blank.
     * @throws ClientException if enforcement is on and {@code submitUrl} is not an absolute HTTPS URL,
     *                         if the challenging origin cannot be resolved or is not HTTPS, or if the
     *                         two are not same-origin (host or port differ).
     */
    private void validateSubmitUrlOrigin(@Nullable final String submitUrl,
                                         @Nullable final String challengingUrl) throws ClientException {
        final String methodTag = TAG + ":validateSubmitUrlOrigin";

        if (!CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION)) {
            // Master switch OFF: skip evaluation, telemetry and enforcement entirely so the whole
            // feature is a true no-op and the historical behavior is preserved exactly.
            return;
        }

        final boolean enforced = CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION);

        // Compute the verdict (and do the rejection logging) without throwing, so telemetry can be
        // emitted on every path — including ALLOWED and, in shadow mode, every rejection.
        final OriginValidation validation = computeOriginValidation(submitUrl, challengingUrl, methodTag, enforced);

        emitOriginValidationTelemetry(validation, enforced);

        if (enforced && validation.result != OriginValidationResult.ALLOWED) {
            throw new ClientException(DEVICE_CERTIFICATE_REQUEST_INVALID, validation.result.rejectionMessage);
        }
    }

    /**
     * The outcome of evaluating a webview-redirect {@code SubmitUrl} against its challenging origin.
     * Each rejection value carries the {@link ClientException} message historically thrown for that
     * reason so enforcement can reuse it verbatim.
     */
    private enum OriginValidationResult {
        ALLOWED(null),
        REJECTED_BACKSLASH_AUTHORITY(
                "PKeyAuth SubmitUrl authority must not contain a backslash."),
        REJECTED_SUBMIT_NOT_HTTPS(
                "PKeyAuth SubmitUrl must be an absolute HTTPS URL."),
        REJECTED_ORIGIN_UNRESOLVABLE(
                "PKeyAuth challenging origin is unavailable or not HTTPS; cannot validate SubmitUrl."),
        REJECTED_ORIGIN_MISMATCH(
                "PKeyAuth SubmitUrl is not same-origin (scheme/host/port) with the challenging endpoint.");

        @Nullable
        private final String rejectionMessage;

        OriginValidationResult(@Nullable final String rejectionMessage) {
            this.rejectionMessage = rejectionMessage;
        }
    }

    /**
     * Holds a verdict together with the parsed {@link URL}s that produced it, so the telemetry site
     * can derive non-PII cloud-membership booleans without re-parsing. A URL field is {@code null}
     * when it could not be safely parsed (e.g. the backslash guard fired before the {@code SubmitUrl}
     * was parsed).
     */
    private static final class OriginValidation {
        private final OriginValidationResult result;
        @Nullable
        private final URL submitUri;
        @Nullable
        private final URL originUri;

        OriginValidation(final OriginValidationResult result,
                         @Nullable final URL submitUri,
                         @Nullable final URL originUri) {
            this.result = result;
            this.submitUri = submitUri;
            this.originUri = originUri;
        }
    }

    /**
     * Evaluates {@code submitUrl} against {@code challengingUrl} and returns the verdict, logging on
     * each rejection path exactly as before (a value-free {@link Logger#warn} plus, where a
     * trustworthy host exists, a hosts/ports-only {@link Logger#warnPII}). Never throws; the caller
     * decides whether a non-{@code ALLOWED} verdict blocks the challenge. The {@code enforced} flag is
     * appended to each warn so on-call can tell a real block from a shadow observation.
     */
    private OriginValidation computeOriginValidation(@Nullable final String submitUrl,
                                                     @Nullable final String challengingUrl,
                                                     @NonNull final String methodTag,
                                                     final boolean enforced) {
        // Guard against a parser differential before any other check: java.net.URL (used below to
        // validate) and the WHATWG parser used by WebView#loadUrl (where the response is actually
        // sent) disagree on a backslash in the authority. Reject it here, in the authority component
        // only, so a legitimate backslash in a path or query value cannot false-reject the challenge.
        final String submitAuthority = extractAuthority(submitUrl);
        if (submitAuthority != null && submitAuthority.indexOf('\\') >= 0) {
            // Do not log the authority itself: on this path it is attacker-shaped (it reached here
            // precisely because it contains a backslash) and the two parsers disagree on what its host
            // even is, so there is no trustworthy host to record. An integer length is the only safe
            // triage signal. See the hosts/scheme-only warnPII discipline at the sibling sites below.
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: SubmitUrl authority contains a backslash "
                            + "(parser-differential guard). authorityLength=" + submitAuthority.length()
                            + " enforced=" + enforced);
            return new OriginValidation(OriginValidationResult.REJECTED_BACKSLASH_AUTHORITY, null, null);
        }

        final URL submitUri = parseAbsoluteUri(submitUrl);
        if (submitUri == null
                || !HTTPS_SCHEME.equalsIgnoreCase(submitUri.getProtocol())
                || StringUtil.isNullOrEmpty(submitUri.getHost())) {
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: SubmitUrl is not an absolute HTTPS URL. enforced=" + enforced);
            Logger.warnPII(methodTag,
                    "PKeyAuth SubmitUrl rejected (not absolute HTTPS). scheme="
                            + (submitUri == null ? "<unparseable>" : submitUri.getProtocol())
                            + " host="
                            + (submitUri == null || submitUri.getHost() == null ? "<none>" : submitUri.getHost()));
            return new OriginValidation(OriginValidationResult.REJECTED_SUBMIT_NOT_HTTPS, submitUri, null);
        }

        final URL originUri = parseAbsoluteUri(challengingUrl);
        if (originUri == null
                || !HTTPS_SCHEME.equalsIgnoreCase(originUri.getProtocol())
                || StringUtil.isNullOrEmpty(originUri.getHost())) {
            // Fail closed: without a resolvable HTTPS challenging origin we cannot prove same-origin.
            // A cleartext http origin is rejected here so it can never authorize an https SubmitUrl.
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: the challenging origin could not be determined or is not HTTPS. "
                            + "enforced=" + enforced);
            Logger.warnPII(methodTag,
                    "PKeyAuth challenge rejected: challenging origin unresolvable or not HTTPS. originScheme="
                            + (originUri == null ? "<unparseable>" : originUri.getProtocol())
                            + " submitHost=" + submitUri.getHost());
            return new OriginValidation(OriginValidationResult.REJECTED_ORIGIN_UNRESOLVABLE, submitUri, originUri);
        }

        final int submitPort = submitUri.getPort() == -1 ? submitUri.getDefaultPort() : submitUri.getPort();
        final int originPort = originUri.getPort() == -1 ? originUri.getDefaultPort() : originUri.getPort();
        if (!submitUri.getHost().equalsIgnoreCase(originUri.getHost()) || submitPort != originPort) {
            Logger.warn(methodTag,
                    "PKeyAuth challenge rejected: SubmitUrl is not same-origin with the challenging endpoint. "
                            + "enforced=" + enforced);
            Logger.warnPII(methodTag,
                    "PKeyAuth SubmitUrl origin mismatch. challengingHost=" + originUri.getHost()
                            + " challengingPort=" + originPort
                            + " submitHost=" + submitUri.getHost()
                            + " submitPort=" + submitPort);
            return new OriginValidation(OriginValidationResult.REJECTED_ORIGIN_MISMATCH, submitUri, originUri);
        }

        return new OriginValidation(OriginValidationResult.ALLOWED, submitUri, originUri);
    }

    /**
     * Emits the PKeyAuth {@code SubmitUrl} origin-validation verdict to the current span. All
     * attributes are non-PII (an enum verdict, booleans): never a hostname, URL, nonce, or
     * {@code Context}. Emitted on every evaluated challenge (including {@code ALLOWED}) so shadow-mode
     * rejection rates can be measured. Writes to {@link SpanExtension#current()}: on the
     * webview-redirect path the WebView client establishes a recording
     * {@code ProcessPKeyAuthChallenge} span as current before this runs (the call is synchronous on
     * the same thread), so the verdict lands on an exported span; outside an active trace
     * {@code current()} is a no-op span, so setting attributes here is always safe. The
     * cloud-membership
     * booleans use {@link AzureActiveDirectory#isValidCloudHost(URL)} and are {@code false} when the
     * corresponding URL could not be safely parsed.
     *
     * <p>The {@code aad_cloud_list_initialized} signal requested for triage is intentionally omitted:
     * {@link AzureActiveDirectory} exposes no public API to report whether its cloud list has been
     * populated, and this fix does not invent one.
     */
    private void emitOriginValidationTelemetry(@NonNull final OriginValidation validation,
                                               final boolean enforced) {
        final Span span = SpanExtension.current();
        span.setAttribute(AttributeName.pkeyauth_submit_url_origin_validation_result.name(),
                validation.result.name());
        span.setAttribute(AttributeName.pkeyauth_submit_url_origin_validation_enforced.name(), enforced);
        span.setAttribute(AttributeName.pkeyauth_submit_host_is_aad_cloud.name(),
                isValidatedAadCloudHost(validation.submitUri));
        span.setAttribute(AttributeName.pkeyauth_challenging_host_is_aad_cloud.name(),
                isValidatedAadCloudHost(validation.originUri));
    }

    /**
     * @return {@code true} when {@code url} is non-null and its host is a validated AAD cloud host.
     * Never throws and never logs the host.
     */
    private boolean isValidatedAadCloudHost(@Nullable final URL url) {
        return url != null && AzureActiveDirectory.isValidCloudHost(url);
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

    /**
     * Extracts the authority component of {@code url} textually — the substring after
     * {@code scheme://} up to the first {@code '/'}, {@code '?'} or {@code '#'}.
     *
     * <p>This is deliberately <em>not</em> derived from {@link URL#getAuthority()}: the whole point
     * of the caller's backslash guard is that {@link URL} mis-parses an authority that contains a
     * backslash (it treats {@code '\'} as an ordinary character and everything up to the last
     * {@code '@'} as userinfo), so we recover the raw authority region ourselves. The WHATWG URL
     * parser used by the WebView that ultimately sends the response additionally treats {@code '\'}
     * as an authority terminator for special schemes, which is exactly the differential we detect:
     * if this raw region contains a backslash, the two parsers would resolve different hosts.
     *
     * @param url the raw URL string; may be {@code null}.
     * @return the raw authority substring, or {@code null} when {@code url} is blank or has no
     *         {@code "://"} scheme separator.
     */
    @Nullable
    private String extractAuthority(@Nullable final String url) {
        if (StringUtil.isNullOrEmpty(url)) {
            return null;
        }
        final int schemeIdx = url.indexOf("://");
        if (schemeIdx < 0) {
            return null;
        }
        final int authorityStart = schemeIdx + "://".length();
        int authorityEnd = url.length();
        for (int i = authorityStart; i < url.length(); i++) {
            final char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                authorityEnd = i;
                break;
            }
        }
        return url.substring(authorityStart, authorityEnd);
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
