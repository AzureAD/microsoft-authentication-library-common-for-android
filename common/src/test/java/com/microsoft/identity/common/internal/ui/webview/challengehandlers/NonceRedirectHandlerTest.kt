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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.webkit.WebView
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.java.broker.CommonRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import com.microsoft.identity.common.java.interfaces.IRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud
import io.opentelemetry.api.trace.Span
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.net.URL

/**
 * Tests for [NonceRedirectHandler], focused on the CWE-918 fix that prevents the PRT credential
 * header ([AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]) from being forwarded to an
 * untrusted or cleartext redirect target.
 *
 * The trust gate is exercised for real: a genuinely-validated cloud host is seeded via
 * [AzureActiveDirectory.putCloud] (which yields an [AzureActiveDirectoryCloud] with
 * `isValidated == true`), while untrusted hosts are simply never seeded, so
 * [AzureActiveDirectory.isValidCloudHost] genuinely returns `false` for them.
 *
 * A synthetic, test-only hostname ([TRUSTED_HOST]) is used rather than a real production host such
 * as `login.microsoftonline.com`. [AzureActiveDirectory.putCloud] writes into the JVM-global
 * `sAadClouds` cache, which this class never clears (the only clear path is guarded inside
 * [AzureActiveDirectory.setEnvironment] and cannot be triggered cleanly). Seeding a real host would
 * therefore permanently mark it validated for every subsequent test in the module, creating
 * order-dependent behavior for anything that relies on that host being unvalidated until instance
 * discovery runs. The behavior under test (host in the validated cloud set -> forward the PRT
 * header) is host-agnostic, so a synthetic host loses nothing.
 */
@RunWith(RobolectricTestRunner::class)
class NonceRedirectHandlerTest {

    private lateinit var webView: WebView
    private lateinit var headers: HashMap<String, String>
    private lateinit var span: Span
    private lateinit var handler: NonceRedirectHandler

    private val prtHeaderValue = "original-prt-credential"
    private val nonCredentialHeaderValue = "passkey-protocol-v1"

    @Before
    fun setUp() {
        webView = mock(WebView::class.java)
        headers = HashMap()
        headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER] = prtHeaderValue
        // A representative non-credential header (e.g. passkey/FIDO protocol or telemetry). It must
        // survive the credential strip so an untrusted-but-legitimate hop keeps that functionality.
        headers[NON_CREDENTIAL_HEADER] = nonCredentialHeaderValue
        span = mock(Span::class.java)
        handler = NonceRedirectHandler(webView, headers, span)

        // Seed a real, validated cloud host so isValidCloudHost() executes for real rather than
        // being mocked. The public (network, cache, aliases) constructor sets isValidated == true.
        // Untrusted-host tests use hosts that are never seeded.
        AzureActiveDirectory.putCloud(
            TRUSTED_HOST,
            AzureActiveDirectoryCloud(TRUSTED_HOST, TRUSTED_HOST, listOf(TRUSTED_HOST))
        )
    }

    @After
    fun tearDown() {
        // Reset the flights manager so a flight override installed by an individual test (e.g. the
        // kill-switch-off case) cannot leak into the other tests, which rely on the default (on).
        CommonFlightsManager.resetFlightsManager()

        // CommonRefreshTokenCredentialProvider is a JVM-global object whose backing provider is a
        // private var, null by default. The swap test below injects a fake; without this reset that
        // fake would persist for the rest of the module's tests and silently enable the credential
        // swap in tests that assume it never fires. The injector's param is non-null so we cannot
        // pass null; instead we re-inject a fake that returns null, restoring the default no-swap
        // semantics (getRefreshTokenCredentialUsingNewNonce == null -> original PRT left in place).
        CommonRefreshTokenCredentialProvider.initializeCommonRefreshTokenCredentialProvider(
            NoSwapRefreshTokenCredentialProvider
        )
    }

    @Test
    fun `https trusted host forwards the PRT credential header`() {
        val url = "https://$TRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertEquals(
            "PRT credential header must be forwarded to a trusted HTTPS AAD host.",
            prtHeaderValue,
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
    }

    @Test
    fun `cleartext http trusted host strips the PRT credential header`() {
        val url = "http://$TRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertFalse(
            "PRT credential header must not be forwarded over cleartext http.",
            forwarded.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
        )
        assertSharedHeadersUnmutated()
    }

    @Test
    fun `https untrusted host strips the PRT credential header`() {
        val url = "https://$UNTRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertFalse(
            "PRT credential header must not be forwarded to an untrusted host.",
            forwarded.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
        )
        assertSharedHeadersUnmutated()
    }

    /**
     * The core exploit shape: omitting `login_hint` means the PRT header is never rewritten, so the
     * ORIGINAL, valid PRT would previously be forwarded verbatim. With the fix, an untrusted target
     * receives no credential header at all.
     */
    @Test
    fun `login_hint omitted on untrusted host does not leak the original PRT`() {
        val url = "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertNull(
            "The original PRT must never reach an untrusted host, even without login_hint.",
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
        assertSharedHeadersUnmutated()
    }

    @Test
    fun `untrusted host strips only credential headers and preserves non-credential headers`() {
        val url = "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertFalse(
            "PRT credential header must be stripped for an untrusted host.",
            forwarded.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
        )
        assertEquals(
            "Non-credential headers must survive the credential strip.",
            nonCredentialHeaderValue,
            forwarded[NON_CREDENTIAL_HEADER]
        )
    }

    /**
     * CWE-918 (Finding B): HTTP header names are case-insensitive (RFC 9110) and the request-header
     * map originates outside this module (broker / MSAL via the REQUEST_HEADERS intent extra), so a
     * producer that supplies the PRT header under a different casing than the canonical constant must
     * still have it stripped on an untrusted host. A case-sensitive strip would forward the
     * credential verbatim — the exact leak this fix exists to close.
     */
    @Test
    fun `untrusted host strips a differently-cased PRT credential header`() {
        val differentlyCasedPrtKey = "X-MS-REFRESHTOKENCREDENTIAL"
        val customHeaders = HashMap<String, String>()
        customHeaders[differentlyCasedPrtKey] = prtHeaderValue
        customHeaders[NON_CREDENTIAL_HEADER] = nonCredentialHeaderValue
        val customWebView = mock(WebView::class.java)
        val customHandler = NonceRedirectHandler(customWebView, customHeaders, span)
        val url = "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        customHandler.processChallenge(URL(url))

        @Suppress("UNCHECKED_CAST")
        val headersCaptor = ArgumentCaptor.forClass(Map::class.java)
            as ArgumentCaptor<Map<String, String>>
        verify(customWebView).loadUrl(eq(url), headersCaptor.capture())
        val forwarded = headersCaptor.value
        assertFalse(
            "A differently-cased PRT credential header must be stripped for an untrusted host.",
            forwarded.containsKey(differentlyCasedPrtKey)
        )
        assertEquals(
            "Non-credential headers must survive the case-insensitive credential strip.",
            nonCredentialHeaderValue,
            forwarded[NON_CREDENTIAL_HEADER]
        )
    }

    @Test
    fun `isRedirectTrustedForHeaderForwarding contract`() {
        assertTrue(
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "https://$TRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "cleartext http must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "http://$TRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "unseeded/uninitialized host must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "malformed url must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding("not a url")
        )
    }

    /**
     * Kill-switch revert: with [CommonFlight.ENABLE_NONCE_REDIRECT_CREDENTIAL_HEADER_VALIDATION]
     * turned OFF, the trust check is skipped entirely and the redirect loads with the full header
     * map — i.e. exactly the pre-fix behavior — even for an untrusted, cleartext target. This proves
     * enforcement can be reverted via ECS without a code rollback.
     */
    @Test
    fun `flight off forwards the PRT credential header to an untrusted host`() {
        CommonFlightsManager.initializeCommonFlightsManager(NonceValidationOffFlightsManager)
        val url = "http://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertEquals(
            "With the kill-switch flight OFF, the PRT credential header must be forwarded unchanged " +
                "(pre-fix behavior).",
            prtHeaderValue,
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
    }

    /**
     * The credential swap is the whole point of the handler: on a trusted host that carries a
     * `login_hint`, [NonceRedirectHandler] must replace the ORIGINAL PRT with a fresh, nonce-bound
     * one (via [CommonRefreshTokenCredentialProvider.getRefreshTokenCredentialUsingNewNonce]) before
     * it is forwarded. Every other test here omits `login_hint`, so the swap short-circuits and the
     * original PRT passes through unchanged — which means a silently no-op'd swap (forwarding the
     * stale PRT) would still satisfy them. This test pins the swap end-to-end: trusted host +
     * `login_hint` => the NEW nonce-bound credential is forwarded, explicitly NOT the original, and
     * the provider is invoked with the nonce and login_hint parsed from the redirect URL (so a swap
     * that fires with the wrong nonce also fails).
     */
    @Test
    fun `trusted host with login_hint forwards the nonce-bound PRT, not the original`() {
        val nonce = "abc"
        val loginHint = "testuser"
        val nonceBoundPrt = "NONCE_BOUND_PRT_SENTINEL"
        var capturedNonce: String? = null
        var capturedUsername: String? = null
        CommonRefreshTokenCredentialProvider.initializeCommonRefreshTokenCredentialProvider(
            object : IRefreshTokenCredentialProvider {
                override fun getRefreshTokenCredentialUsingNewNonce(
                    inputUrl: String,
                    username: String,
                    nonce: String
                ): String {
                    capturedNonce = nonce
                    capturedUsername = username
                    return nonceBoundPrt
                }

                override fun getRefreshTokenCredential(inputUrl: String, username: String): String? =
                    null
            }
        )
        val url = "https://$TRUSTED_HOST/authorize?sso_nonce=$nonce&login_hint=$loginHint"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertEquals(
            "The nonce-bound PRT credential must be the one forwarded to a trusted host with a login_hint.",
            nonceBoundPrt,
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
        assertNotEquals(
            "The ORIGINAL PRT must not be forwarded once a nonce-bound credential is available; a " +
                "silently no-op'd swap would otherwise leak the stale PRT.",
            prtHeaderValue,
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
        assertEquals(
            "The provider must be invoked with the nonce parsed from the redirect URL.",
            nonce,
            capturedNonce
        )
        assertEquals(
            "The provider must be invoked with the login_hint parsed from the redirect URL.",
            loginHint,
            capturedUsername
        )
    }

    /**
     * Telemetry (Round 8): a trusted HTTPS AAD host records the credential-forwarding decision on the
     * span as booleans only — flight enabled, host trusted, nothing stripped. No host, URL, header
     * value, PRT, sso_nonce or login_hint is ever emitted.
     */
    @Test
    fun `trusted host records credential-forwarding telemetry`() {
        val url = "https://$TRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        verify(span).setAttribute(AttributeName.nonce_redirect_validation_flight_enabled.name, true)
        verify(span).setAttribute(AttributeName.nonce_redirect_host_trusted.name, true)
        verify(span).setAttribute(AttributeName.nonce_redirect_credential_header_stripped.name, false)
    }

    /**
     * Telemetry (Round 8): an untrusted host with the validation flight on records host-not-trusted
     * and credential-header-stripped, distinguishing a genuine strip from a trusted forward.
     */
    @Test
    fun `untrusted host records credential-forwarding telemetry`() {
        val url = "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        verify(span).setAttribute(AttributeName.nonce_redirect_validation_flight_enabled.name, true)
        verify(span).setAttribute(AttributeName.nonce_redirect_host_trusted.name, false)
        verify(span).setAttribute(AttributeName.nonce_redirect_credential_header_stripped.name, true)
    }

    /**
     * Telemetry (Round 8): with the kill-switch flight OFF the fix is a no-op — the flight attribute
     * reports disabled and nothing is stripped, even for an untrusted cleartext target.
     */
    @Test
    fun `flight off records credential-forwarding telemetry as a no-op`() {
        CommonFlightsManager.initializeCommonFlightsManager(NonceValidationOffFlightsManager)
        val url = "http://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        verify(span).setAttribute(AttributeName.nonce_redirect_validation_flight_enabled.name, false)
        verify(span).setAttribute(AttributeName.nonce_redirect_credential_header_stripped.name, false)
    }

    private fun captureLoadedHeaders(expectedUrl: String): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        val headersCaptor = ArgumentCaptor.forClass(Map::class.java)
            as ArgumentCaptor<Map<String, String>>
        verify(webView).loadUrl(
            eq(expectedUrl),
            headersCaptor.capture()
        )
        return headersCaptor.value
    }

    private fun assertSharedHeadersUnmutated() {
        assertEquals(
            "The shared request-header map must not be mutated by the untrusted path.",
            prtHeaderValue,
            headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
    }

    companion object {
        private const val TRUSTED_HOST = "trusted.contoso.example"
        private const val UNTRUSTED_HOST = "malicious.contoso.example"
        private const val NON_CREDENTIAL_HEADER = "x-ms-PasskeyProtocol"

        /**
         * A [IRefreshTokenCredentialProvider] that never produces a credential. Used by `@After` to
         * restore [CommonRefreshTokenCredentialProvider]'s default no-swap semantics after the swap
         * test injects a real fake, since the injector requires a non-null provider and cannot be
         * reset to `null` directly.
         */
        private object NoSwapRefreshTokenCredentialProvider : IRefreshTokenCredentialProvider {
            override fun getRefreshTokenCredentialUsingNewNonce(
                inputUrl: String,
                username: String,
                nonce: String
            ): String? = null

            override fun getRefreshTokenCredential(inputUrl: String, username: String): String? = null
        }

        /**
         * Inline test [IFlightsManager] whose provider returns `false` only for
         * [CommonFlight.ENABLE_NONCE_REDIRECT_CREDENTIAL_HEADER_VALIDATION] and each flight's own
         * default for everything else, so the kill-switch-off path can be exercised without
         * disturbing unrelated flights.
         *
         * Implemented inline (rather than via the shared `MockCommonFlightsManager` helper) because
         * that helper's setter is generated by Lombok at Java-compile time — after kotlinc — so it is
         * not visible from Kotlin test sources without enabling kapt. This mirrors the approach in
         * `SwitchBrowserActivityTest`.
         */
        private object NonceValidationOffFlightsManager : IFlightsManager {
            private val provider = object : IFlightsProvider {
                override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean =
                    if (flightConfig.key ==
                        CommonFlight.ENABLE_NONCE_REDIRECT_CREDENTIAL_HEADER_VALIDATION.key
                    ) {
                        false
                    } else {
                        flightConfig.defaultValue as Boolean
                    }

                override fun getBooleanValue(flightConfig: IFlightConfig): Boolean =
                    isFlightEnabled(flightConfig)

                override fun getIntValue(flightConfig: IFlightConfig): Int =
                    flightConfig.defaultValue as Int

                override fun getDoubleValue(flightConfig: IFlightConfig): Double =
                    flightConfig.defaultValue as Double

                override fun getStringValue(flightConfig: IFlightConfig): String =
                    flightConfig.defaultValue as String

                override fun getJsonValue(flightConfig: IFlightConfig): JSONObject =
                    flightConfig.defaultValue as JSONObject
            }

            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider =
                provider

            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ): IFlightsProvider = provider
        }
    }
}
