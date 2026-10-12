// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.fido

import android.net.Uri
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import io.opentelemetry.api.trace.Span
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PasskeyEndpointPolicyTest {
    private val policy = PasskeyEndpointPolicy(
        setOf("request.example"),
        setOf("https://response.example/common/credential")
    )

    private fun challenge(submitUrl: String?): FidoChallenge {
        val builder = Uri.Builder().scheme("urn").opaquePart("http-auth:PassKey")
        if (submitUrl != null) builder.appendQueryParameter("submitUrl", submitUrl)
        return FidoChallenge.createFromRedirectUri(builder.build().toString())
    }

    @Test
    fun independentHostsAndDefaultHttpsPortAreAllowed() {
        val span = mock(Span::class.java)
        policy.evaluateAndRecord(
            "HTTPS://REQUEST.EXAMPLE:443/tenant/authorize",
            challenge("HTTPS://RESPONSE.EXAMPLE:443/common/credential?passKeyAuth=1.0%2Fpasskey"),
            span, true
        )
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "ALLOW")
    }

    @Test
    fun emptyPolicyObservesWithoutBlocking() {
        val span = mock(Span::class.java)
        PasskeyEndpointPolicy.DEFAULT.evaluateAndRecord(
            "https://request.example/authorize",
            challenge("https://response.example/common/credential"),
            span, false
        )
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_configured.name, false)
        verify(span).setAttribute(AttributeName.passkey_incoming_endpoint_result.name, "HOST_NOT_ALLOWED")
        verify(span).setAttribute(AttributeName.passkey_submit_endpoint_result.name, "HOST_NOT_ALLOWED")
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "WOULD_REJECT")
    }

    @Test
    fun emptyPolicyEnforcesWithoutPermissiveFallback() {
        assertThrows(ClientException::class.java) {
            PasskeyEndpointPolicy.DEFAULT.evaluateAndRecord(
                "https://request.example/authorize",
                challenge("https://response.example/common/credential"), mock(Span::class.java), true
            )
        }
    }

    @Test
    fun responseVariantsAreRejectedWithBoundedReasons() {
        val variants = mapOf(
            "http://response.example/common/credential" to "NOT_HTTPS",
            "https://name@response.example/common/credential" to "USER_INFO",
            "https://response.example:8443/common/credential" to "PORT_NOT_ALLOWED",
            "https://response.example/common/credential#fragment" to "FRAGMENT",
            "https://response.example.other/common/credential" to "HOST_NOT_ALLOWED",
            "https://response.example./common/credential" to "HOST_NOT_ALLOWED",
            "https://response.example/other" to "ROUTE_NOT_ALLOWED",
            "https://response.example/common/%63redential" to "ROUTE_NOT_ALLOWED",
            "https://response.example/common/../common/credential" to "ROUTE_NOT_ALLOWED",
            "https://response.example\\other/common/credential" to "MALFORMED",
            "https://response.example:invalid/common/credential" to "MALFORMED",
            "https://response.example/common/credential\n" to "MALFORMED"
        )
        variants.forEach { (url, result) ->
            val span = mock(Span::class.java)
            assertThrows(url, ClientException::class.java) {
                policy.evaluateAndRecord("https://request.example/authorize", challenge(url), span, true)
            }
            verify(span).setAttribute(AttributeName.passkey_submit_endpoint_result.name, result)
            verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "BLOCK")
        }
    }

    @Test
    fun invalidIncomingHostRejectsEvenWithApprovedResponse() {
        val span = mock(Span::class.java)
        assertThrows(ClientException::class.java) {
            policy.evaluateAndRecord(
                "https://unlisted.example/authorize",
                challenge("https://response.example/common/credential"), span, true
            )
        }
        verify(span).setAttribute(AttributeName.passkey_incoming_endpoint_result.name, "HOST_NOT_ALLOWED")
        verify(span).setAttribute(AttributeName.passkey_submit_endpoint_result.name, "ALLOWED")
    }

    @Test
    fun missingAndMalformedFieldsAreObservedWithoutThrowing() {
        listOf(null, "", "not a url", "custom://response.example/common/credential").forEach {
            val span = mock(Span::class.java)
            policy.evaluateAndRecord(null, challenge(it), span, false)
            verify(span).setAttribute(AttributeName.passkey_incoming_endpoint_result.name, "MISSING")
            verify(span).setAttribute(AttributeName.passkey_submit_endpoint_result.name, "MALFORMED")
            verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "WOULD_REJECT")
        }
    }

    @Test
    fun malformedOriginalUrlsAreRejected() {
        listOf(
            "", "relative", "http://request.example/authorize",
            "custom://request.example/authorize", "https://user@request.example/authorize",
            "https://request.example:8443/authorize", "https://request.example/authorize#fragment"
        ).forEach {
            assertThrows(ClientException::class.java) {
                policy.evaluateAndRecord(
                    it, challenge("https://response.example/common/credential"), mock(Span::class.java), true
                )
            }
        }
    }

    @Test
    fun invalidPolicyEntriesAreExplicitConfigurationErrors() {
        listOf("REQUEST.EXAMPLE", "request.example:443", "request.example/path", "user@request.example")
            .forEach {
                assertThrows(IllegalArgumentException::class.java) { PasskeyEndpointPolicy(setOf(it)) }
            }
        listOf(
            "http://response.example/common/credential", "https://response.example:443/common/credential",
            "https://response.example/common/credential?query=value", "https://response.example"
        ).forEach {
            assertThrows(IllegalArgumentException::class.java) {
                PasskeyEndpointPolicy(responseEndpoints = setOf(it))
            }
        }
    }
}
