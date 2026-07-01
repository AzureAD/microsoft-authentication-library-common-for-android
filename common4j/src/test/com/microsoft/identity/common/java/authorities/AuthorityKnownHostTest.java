//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.authorities;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.http.MockConnection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;

/**
 * Regression tests for the security gate in {@link Authority#isKnownAuthority(Authority)} and for the
 * agreement between that gate and authority resolution
 * ({@link Authority#getAuthorityFromAuthorityUrl(String)}).
 *
 * <p>The gate must only accept a candidate authority whose parsed host exactly (case-insensitively)
 * matches the parsed host of a developer-configured known authority. A previous implementation used
 * {@code String.contains()} against the full configured URL string, which allowed an untrusted host
 * (e.g. {@code login.com}) to pass merely because it was a substring of a configured URL
 * (e.g. {@code https://contoso.b2clogin.com/...}). These tests guard against that bypass.</p>
 *
 * <p>The gate and authority resolution must also agree about which URLs are developer-configured:
 * both compare by parsed host only, so a same-host/different-port candidate is treated as configured
 * by both paths (Group 4).</p>
 */
public class AuthorityKnownHostTest {

    private static final String CONFIGURED_B2C_URL =
            "https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signin";

    private Authority configuredAuthority(final String url) {
        return new AzureActiveDirectoryB2CAuthority(url);
    }

    private void configureKnownAuthority(final String url) {
        Authority.addKnownAuthorities(Collections.singletonList(configuredAuthority(url)));
    }

    private Authority candidate(final String url) {
        // Using a concrete Authority whose getAuthorityURL() simply parses the supplied URL string,
        // so the test exercises only the host comparison performed by isKnownAuthority().
        return new AzureActiveDirectoryB2CAuthority(url);
    }

    @Before
    public void setUp() {
        Authority.clearKnownAuthorities();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @After
    public void tearDown() {
        Authority.clearKnownAuthorities();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    // =============================================================================================
    // Group 1 - Security regression guards: these FAIL on the legacy .contains() logic and pass only
    // WITH the fix. They are the tests that actually catch the substring-bypass vulnerability.
    // =============================================================================================

    /** A host that is a substring of a configured host must be rejected. */
    @Test
    public void substringHostIsRejected() {
        configureKnownAuthority(CONFIGURED_B2C_URL);

        // "login.com" is a substring of "b2clogin.com".
        Assert.assertFalse(
                Authority.isKnownAuthority(candidate("https://login.com/contoso.onmicrosoft.com")));
    }

    /**
     * Port is ignored: an explicit default port on the candidate must still match a configured
     * authority that omits the port. The trust boundary is the host, not the port. (Legacy
     * substring matching would have compared "host:443" and failed, so this only passes with the fix.)
     */
    @Test
    public void explicitDefaultPortMatchesImplicitPort() {
        configureKnownAuthority("https://login.microsoftonline.com/common");

        Assert.assertTrue(
                Authority.isKnownAuthority(candidate("https://login.microsoftonline.com:443/common")));
    }

    // =============================================================================================
    // Group 2 - Non-regression guards: these pass WITH AND WITHOUT the fix. They do not catch the
    // bug; they ensure the stricter matching does not break legitimate authority validation.
    // =============================================================================================

    /** The exact configured host must be accepted. */
    @Test
    public void exactConfiguredHostIsAccepted() {
        configureKnownAuthority(CONFIGURED_B2C_URL);

        Assert.assertTrue(
                Authority.isKnownAuthority(candidate("https://contoso.b2clogin.com/contoso.onmicrosoft.com/anotherpolicy")));
    }

    /** Case-insensitive exact host match must be accepted. */
    @Test
    public void caseInsensitiveExactHostIsAccepted() {
        configureKnownAuthority("https://Contoso.B2CLogin.com/contoso.onmicrosoft.com/B2C_1_signin");

        Assert.assertTrue(
                Authority.isKnownAuthority(candidate("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signin")));
    }

    /**
     * A look-alike host that merely contains the configured host as a substring must be rejected.
     * (Legacy already rejects this because the candidate host is longer than the configured host, so
     * it is a non-regression guard rather than a bug-catcher.)
     */
    @Test
    public void pathExtensionLookAlikeHostIsRejected() {
        configureKnownAuthority("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signin");

        Assert.assertFalse(
                Authority.isKnownAuthority(candidate("https://contoso.b2clogin.com.attacker.example/contoso.onmicrosoft.com/B2C_1_signin")));
    }

    /** A null candidate authority must be rejected (early return, before any matching). */
    @Test
    public void nullAuthorityIsRejected() {
        configureKnownAuthority(CONFIGURED_B2C_URL);

        Assert.assertFalse(Authority.isKnownAuthority(null));
    }

    // =============================================================================================
    // Group 3 - Full-gate guards: exercise the shared Authority.getKnownAuthorityResult() chokepoint
    // (with AAD instance-discovery mocked offline) that both the ROPC flow
    // (BaseController.acquireTokenWithPassword) and the silent flow
    // (BaseController.performSilentTokenRequest) consult - via
    // `if (!authorityResult.getKnown()) throw authorityResult.getClientException();` - BEFORE
    // constructing the OAuth2 strategy / setting the refresh token and POSTing any credential. Since
    // both paths reduce to this single gate, one reject test and one accept test cover both flows.
    // The reject test only passes WITH the fix; the accept test is a non-regression guard (both).
    // =============================================================================================

    /** A substring-host authority is denied by the shared gate before any credential is sent. */
    @Test
    public void gateRejectsSubstringHostBeforeCredentialSubmission() throws IOException {
        configureKnownAuthority(CONFIGURED_B2C_URL);
        enqueueInstanceDiscoveryResponse();

        // "login.com" is a substring of the configured "contoso.b2clogin.com" host.
        final Authority attackerAuthority = candidate("https://login.com/contoso.onmicrosoft.com");
        final Authority.KnownAuthorityResult result = Authority.getKnownAuthorityResult(attackerAuthority);

        // Gate denies -> BaseController would throw this exception before any token POST.
        Assert.assertFalse("Substring host must not be treated as known", result.getKnown());
        Assert.assertNotNull("A blocking exception must be produced for the controller to throw",
                result.getClientException());
        Assert.assertEquals(ClientException.UNKNOWN_AUTHORITY,
                result.getClientException().getErrorCode());
    }

    /** The legitimate, exactly-configured host must still pass the shared gate (non-regression). */
    @Test
    public void gateAcceptsExactlyConfiguredHost() throws IOException {
        configureKnownAuthority(CONFIGURED_B2C_URL);
        enqueueInstanceDiscoveryResponse();

        final Authority legitimateAuthority =
                candidate("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signin");
        final Authority.KnownAuthorityResult result = Authority.getKnownAuthorityResult(legitimateAuthority);

        Assert.assertTrue("Exactly-configured host must remain known", result.getKnown());
        Assert.assertNull(result.getClientException());
    }

    // =============================================================================================
    // Group 4 - Path-consistency guards: the known-authority gate (isKnownAuthority) and authority
    // resolution (getAuthorityFromAuthorityUrl -> getEquivalentConfiguredAuthority) must agree about
    // whether a URL is developer-configured. Both now compare by parsed host only (via the shared
    // matchesConfiguredHost helper), so a same-host/different-port candidate is treated as
    // configured by BOTH paths. Before the fix these diverged (gate = host-only, resolution =
    // host:port), so a port-divergent URL was "known" by the gate yet resolved as an unconfigured
    // (fallback AAD) authority. Both tests below only pass WITH the fix: one covers the port-divergence
    // agreement, the other covers a malformed configured entry being skipped (not aborting) by both paths.
    // =============================================================================================

    private static final String PORT_DIVERGENT_B2C_URL =
            "https://contoso.b2clogin.com:8443/contoso.onmicrosoft.com/B2C_1_signin";

    /**
     * The gate and authority resolution must agree for a same-host/different-port candidate: the gate
     * reports it as known AND resolution treats it as the configured (B2C) type.
     */
    @Test
    public void gateAndResolutionAgreeForPortDivergentHost() {
        configureKnownAuthority(CONFIGURED_B2C_URL);

        Assert.assertTrue("Gate must treat the port-divergent host as known",
                Authority.isKnownAuthority(candidate(PORT_DIVERGENT_B2C_URL)));
        Assert.assertTrue("Resolution must treat the port-divergent host as configured (B2C)",
                Authority.getAuthorityFromAuthorityUrl(PORT_DIVERGENT_B2C_URL)
                        instanceof AzureActiveDirectoryB2CAuthority);
    }

    /**
     * A malformed configured known-authority entry must be skipped by BOTH paths without aborting the
     * scan, so a valid entry later in the list is still matched. This keeps the gate and resolution in
     * agreement even when the configured list contains an unparseable URL.
     */
    @Test
    public void malformedConfiguredAuthorityIsSkippedByBothPaths() {
        // A malformed configured URL (no protocol) precedes a valid configured B2C authority.
        Authority.addKnownAuthorities(Arrays.asList(
                configuredAuthority("malformed-configured-url"),
                configuredAuthority(CONFIGURED_B2C_URL)));

        final String validCandidateUrl =
                "https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signin";

        Assert.assertTrue("Gate must still match the valid entry after skipping the malformed one",
                Authority.isKnownAuthority(candidate(validCandidateUrl)));
        Assert.assertTrue("Resolution must still match the valid entry after skipping the malformed one",
                Authority.getAuthorityFromAuthorityUrl(validCandidateUrl)
                        instanceof AzureActiveDirectoryB2CAuthority);
    }

    /**
     * Enqueues a mocked AAD instance-discovery response so the cloud-discovery step inside
     * {@link Authority#getKnownAuthorityResult(Authority)} completes offline. A 4xx response makes
     * discovery return without throwing and without registering any host as a Microsoft cloud, so the
     * gate's decision is driven solely by the developer-configured known-authority comparison under
     * test (and the resulting exception for an unknown host is {@code UNKNOWN_AUTHORITY}).
     */
    private void enqueueInstanceDiscoveryResponse() throws IOException {
        HttpUrlConnectionFactory.addMockedConnection(
                MockConnection.getMockedConnectionWithFailureResponse(HttpURLConnection.HTTP_BAD_REQUEST));
    }
}
