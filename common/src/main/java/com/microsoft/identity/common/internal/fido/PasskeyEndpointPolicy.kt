// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.fido

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import io.opentelemetry.api.trace.Span
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * Evaluates independent original-request hosts and HTTPS response endpoints.
 *
 * @param incomingHosts exact lowercase hosts permitted to start the native flow.
 * @param responseEndpoints canonical HTTPS URLs containing only a host and exact credential path.
 */
class PasskeyEndpointPolicy(
    incomingHosts: Set<String> = emptySet(),
    responseEndpoints: Set<String> = emptySet()
) {
    private val incomingHosts = incomingHosts.toSet()
    private val responseEndpoints = responseEndpoints.toSet()

    init {
        require(this.incomingHosts.all { host ->
            parse("https://$host")?.let {
                it.host == host && host == host.lowercase(Locale.ROOT) &&
                    structuralResult("https://$host", it) == Result.ALLOWED &&
                    it.rawPath.isEmpty() && it.rawQuery == null
            } == true
        }) { "Incoming hosts must be canonical HTTPS hostnames." }
        require(this.responseEndpoints.all { endpoint ->
            parse(endpoint)?.let {
                structuralResult(endpoint, it) == Result.ALLOWED &&
                    it.rawPath.isNotEmpty() && it.rawQuery == null && endpointKey(it) == endpoint
            } == true
        }) { "Response endpoints must be canonical HTTPS host/path URLs." }
    }

    /** Bounded endpoint-policy outcomes; no challenge data is included. */
    enum class Result {
        ALLOWED, MISSING, MALFORMED, NOT_HTTPS, USER_INFO, FRAGMENT, PORT_NOT_ALLOWED,
        HOST_NOT_ALLOWED, ROUTE_NOT_ALLOWED
    }

    companion object {
        /** Empty until the supported endpoint contract has been approved. */
        @JvmField
        val DEFAULT = PasskeyEndpointPolicy()

        private const val POLICY_VERSION = "1"

        private fun parse(value: String?): URI? {
            if (value.isNullOrBlank()) return null
            return try {
                val uri = URI(value)
                if (!uri.isAbsolute || uri.host.isNullOrEmpty() || value.contains('\\')) null
                else uri
            } catch (exception: URISyntaxException) {
                null
            }
        }

        private fun endpointKey(uri: URI): String =
            "https://${uri.host.lowercase(Locale.ROOT)}${uri.rawPath}"

        private fun structuralResult(value: String?, uri: URI?): Result = when {
            value.isNullOrBlank() -> Result.MISSING
            uri == null -> Result.MALFORMED
            !uri.scheme.equals("https", ignoreCase = true) -> Result.NOT_HTTPS
            uri.rawUserInfo != null -> Result.USER_INFO
            uri.rawFragment != null -> Result.FRAGMENT
            uri.port != -1 && uri.port != 443 -> Result.PORT_NOT_ALLOWED
            else -> Result.ALLOWED
        }
    }

    /**
     * Records the policy verdict and optionally rejects locally before challenge dispatch.
     *
     * Shadow mode never changes challenge fields or invokes their response/error handlers.
     */
    @Throws(ClientException::class)
    fun evaluateAndRecord(
        requestUrl: String?,
        challenge: FidoChallenge,
        span: Span,
        enforced: Boolean
    ) {
        val incomingUri = parse(requestUrl)
        val incomingStructure = structuralResult(requestUrl, incomingUri)
        val incomingResult = if (incomingStructure == Result.ALLOWED &&
            !incomingHosts.contains(incomingUri!!.host.lowercase(Locale.ROOT))
        ) Result.HOST_NOT_ALLOWED else incomingStructure

        // Field validation is lazy. Observe a parsing failure without changing legacy shadow behavior.
        var submitFieldInvalid = false
        val submitUrl = try {
            challenge.submitUrl.getOrThrow()
        } catch (exception: ClientException) {
            submitFieldInvalid = true
            null
        }
        val submitUri = parse(submitUrl)
        val submitStructure = structuralResult(submitUrl, submitUri)
        val submitResult = when {
            submitFieldInvalid -> Result.MALFORMED
            submitStructure != Result.ALLOWED -> submitStructure
            responseEndpoints.contains(endpointKey(submitUri!!)) -> Result.ALLOWED
            responseEndpoints.none {
                URI(it).host.equals(submitUri.host, ignoreCase = true)
            } -> Result.HOST_NOT_ALLOWED
            else -> Result.ROUTE_NOT_ALLOWED
        }
        val allowed = incomingResult == Result.ALLOWED && submitResult == Result.ALLOWED
        span.setAttribute(AttributeName.passkey_endpoint_policy_version.name, POLICY_VERSION)
        span.setAttribute(
            AttributeName.passkey_endpoint_policy_configured.name,
            incomingHosts.isNotEmpty() && responseEndpoints.isNotEmpty()
        )
        span.setAttribute(AttributeName.passkey_incoming_endpoint_result.name, incomingResult.name)
        span.setAttribute(AttributeName.passkey_submit_endpoint_result.name, submitResult.name)
        span.setAttribute(AttributeName.passkey_incoming_host_class.name, hostClass(incomingUri))
        span.setAttribute(AttributeName.passkey_submit_host_class.name, hostClass(submitUri))
        span.setAttribute(
            AttributeName.passkey_submit_route_class.name,
            when {
                submitUri == null -> "UNAVAILABLE"
                submitUri.rawPath.endsWith("/credential") -> "CREDENTIAL"
                else -> "OTHER"
            }
        )
        span.setAttribute(AttributeName.passkey_endpoint_policy_enforced.name, enforced)
        span.setAttribute(
            AttributeName.passkey_endpoint_policy_action.name,
            if (allowed) "ALLOW" else if (enforced) "BLOCK" else "WOULD_REJECT"
        )
        if (enforced && !allowed) {
            throw ClientException(
                ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR,
                "Passkey endpoint policy rejected the challenge: " +
                    "incoming=${incomingResult.name}, submit=${submitResult.name}."
            )
        }
    }

    private fun hostClass(uri: URI?): String = when {
        uri == null -> "UNAVAILABLE"
        AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(uri.host)?.isValidated == true -> "AAD_CLOUD"
        else -> "OTHER"
    }
}
