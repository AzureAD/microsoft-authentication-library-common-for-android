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
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.http.MockConnection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

/**
 * Regression tests for the security gate in {@link Authority#isKnownAuthority(Authority)}.
 *
 * <p>The gate must only accept a candidate authority whose parsed host exactly (case-insensitively)
 * matches the parsed host of a developer-configured known authority. A previous implementation used
 * {@code String.contains()} against the full configured URL string, which allowed an untrusted host
 * (e.g. {@code login.com}) to pass merely because it was a substring of a configured URL
 * (e.g. {@code https://contoso.b2clogin.com/...}). These tests guard against that bypass.</p>
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
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    /**
     * Overrides the {@link CommonFlight#ENABLE_KNOWN_AUTHORITY_HOST_EXACT_MATCH} kill-switch flight.
     * Default (no override) is on/secure; pass {@code false} to fall back to legacy substring matching.
     */
    private void setExactHostMatchFlight(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(
                CommonFlight.ENABLE_KNOWN_AUTHORITY_HOST_EXACT_MATCH.getKey(),
                Boolean.toString(enabled));
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
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

    /** A short generic substring host must be rejected. */
    @Test
    public void shortGenericSubstringHostIsRejected() {
        configureKnownAuthority("https://login.microsoftonline.com/common");

        // "e.com" is a substring of "microsoftonline.com".
        Assert.assertFalse(
                Authority.isKnownAuthority(candidate("https://e.com/common")));
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

    /**
     * Port is ignored: a different, non-default port on the same configured host is still accepted,
     * since the gate trusts the host regardless of port.
     */
    @Test
    public void differentPortSameHostIsAccepted() {
        configureKnownAuthority("https://login.microsoftonline.com/common");

        Assert.assertTrue(
                Authority.isKnownAuthority(candidate("https://login.microsoftonline.com:8443/common")));
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
    // Group 4 - Kill-switch flight (CommonFlight.ENABLE_KNOWN_AUTHORITY_HOST_EXACT_MATCH).
    //
    // The fix is gated behind a default-on flight so the previous (insecure) substring behavior can
    // be restored via ECS in an emergency, should exact-host matching ever reject a legitimate,
    // developer-configured authority. The same CommonFlight is honored by Broker automatically
    // through its ECS flights provider (which forwards any IFlightConfig by key), matching the
    // ENABLE_SOVEREIGN_CLOUD_INSTANCE_DISCOVERY precedent in AzureActiveDirectory.
    //
    // Contrast with substringHostIsRejected (flight default-on -> rejected): the same substring host
    // is accepted here when the flight is OFF, demonstrating the switch actually reverts behavior.
    // =============================================================================================

    /** Flight OFF (kill switch): legacy substring behavior is restored, so the substring host matches. */
    @Test
    public void flightOff_substringHostIsAcceptedByLegacyBehavior() {
        configureKnownAuthority(CONFIGURED_B2C_URL);
        setExactHostMatchFlight(false);

        // "login.com" is a substring of the configured "contoso.b2clogin.com" host. Under the legacy
        // comparison this (insecurely) passes — documenting exactly what the kill switch reverts to.
        Assert.assertTrue(
                Authority.isKnownAuthority(candidate("https://login.com/contoso.onmicrosoft.com")));
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
