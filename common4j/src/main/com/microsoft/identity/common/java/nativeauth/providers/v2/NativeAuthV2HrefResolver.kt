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

package com.microsoft.identity.common.java.nativeauth.providers.v2

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.util.CommonURIBuilder
import com.microsoft.identity.common.java.util.UrlUtil
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.LinkedHashMap

/**
 * Resolves Native Auth V2 server-provided hrefs against the configured authority.
 *
 * Absolute hrefs are restricted to the configured authority, while relative hrefs retain the
 * configured tenant path. Invalid hrefs are rejected before they can receive sensitive flow state.
 */
class NativeAuthV2HrefResolver(private val config: NativeAuthOAuth2Configuration) {

    /**
     * Resolves a server-provided `_links` href into an absolute request URL.
     *
     * @param href Server-provided absolute, relative, or tenant-templated href.
     * @param correlationId Correlation ID to attach to any validation error.
     * @return An absolute URL restricted to the configured authority.
     * @throws ClientException If the href is malformed, uses a disallowed scheme, or points
     * off-authority.
     */
    fun resolve(href: String, correlationId: String): URL {
        val trimmedHref = href.trim()
        if (trimmedHref.isBlank()) {
            throw clientException(
                errorCode = ClientException.MALFORMED_URL,
                message = "Native Auth V2 href must not be blank.",
                correlationId = correlationId
            )
        }

        return try {
            val normalizedHref = stripLeadingTenantTemplate(trimmedHref)
            if (normalizedHref.startsWith(NETWORK_PATH_PREFIX)) {
                throw clientException(
                    errorCode = ClientException.UNSUPPORTED_URL,
                    message = "Native Auth V2 network-path hrefs are not supported.",
                    correlationId = correlationId
                )
            }

            val uri = URI(normalizedHref)
            validateUserInfoAndFragment(uri, correlationId)

            if (uri.isAbsolute) {
                resolveAbsolute(uri, correlationId)
            } else {
                resolveRelative(uri, correlationId)
            }
        } catch (exception: ClientException) {
            throw exception
        } catch (_: URISyntaxException) {
            throw clientException(
                errorCode = ClientException.MALFORMED_URL,
                message = "Native Auth V2 href is malformed.",
                correlationId = correlationId
            )
        } catch (_: MalformedURLException) {
            throw clientException(
                errorCode = ClientException.MALFORMED_URL,
                message = "Native Auth V2 href could not be converted to a URL.",
                correlationId = correlationId
            )
        }
    }

    private fun resolveAbsolute(uri: URI, correlationId: String): URL {
        val scheme = uri.scheme
        val host = uri.host
        if (scheme == null || host == null) {
            throw clientException(
                errorCode = ClientException.MALFORMED_URL,
                message = "Native Auth V2 absolute href must include a scheme and host.",
                correlationId = correlationId
            )
        }

        val authority = config.getAuthorityUrl()
        val isHttps = scheme.equals(HTTPS_SCHEME, ignoreCase = true)
        val isAllowedMockHttp = scheme.equals(HTTP_SCHEME, ignoreCase = true) &&
            config.useMockApiForNativeAuth &&
            host.equals(authority.host, ignoreCase = true)

        if (!isHttps && !isAllowedMockHttp) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href uses a disallowed scheme.",
                correlationId = correlationId
            )
        }

        val endpoint = AuthorityEndpoint(host, effectivePort(uri))
        val configuredEndpoint = AuthorityEndpoint(authority.host, effectivePort(authority))
        val isConfiguredAuthority = endpoint.host.equals(
            configuredEndpoint.host,
            ignoreCase = true
        ) && endpoint.port == configuredEndpoint.port
        val isAllowlistedAuthority = ALLOWED_AUTHORITIES.any {
            endpoint.host.equals(it.host, ignoreCase = true) && endpoint.port == it.port
        }

        if (!isConfiguredAuthority && !isAllowlistedAuthority) {
            throw clientException(
                errorCode = ClientException.UNKNOWN_AUTHORITY,
                message = "Native Auth V2 href points to an untrusted authority.",
                correlationId = correlationId
            )
        }

        val dc = BuildValues.getDC()
        if (dc.isEmpty()) {
            return uri.toURL()
        }

        return CommonURIBuilder(uri)
            .addParametersIfAbsent(mapOf(DC_QUERY_PARAMETER to dc))
            .build()
            .toURL()
    }

    private fun resolveRelative(uri: URI, correlationId: String): URL {
        if (uri.rawAuthority != null) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 network-path hrefs are not supported.",
                correlationId = correlationId
            )
        }

        val apiPath = extractApiPath(uri.path.orEmpty(), correlationId)
        val queryParameters = getQueryParameters(uri)
        val dc = BuildValues.getDC()
        if (
            dc.isNotEmpty() &&
            queryParameters.keys.none {
                it.equals(DC_QUERY_PARAMETER, ignoreCase = true)
            }
        ) {
            queryParameters[DC_QUERY_PARAMETER] = dc
        }

        return UrlUtil.appendPathAndQueryToURL(
            config.getAuthorityUrl(),
            apiPath,
            queryParameters.takeIf { it.isNotEmpty() }
        )
    }

    private fun validateUserInfoAndFragment(uri: URI, correlationId: String) {
        if (uri.rawUserInfo != null) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href must not contain user-info.",
                correlationId = correlationId
            )
        }

        if (uri.rawFragment != null) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href must not contain a fragment.",
                correlationId = correlationId
            )
        }
    }

    private fun extractApiPath(path: String, correlationId: String): String {
        val normalizedPath = if (path.startsWith(PATH_SEPARATOR)) path else "$PATH_SEPARATOR$path"
        val apiIndex = normalizedPath.indexOf(API_PATH_PREFIX)
        val oauth2Index = normalizedPath.indexOf(OAUTH2_PATH_PREFIX)
        val endpointIndex = listOf(apiIndex, oauth2Index)
            .filter { it >= 0 }
            .minOrNull()

        if (endpointIndex == null) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 relative href does not contain a supported API path.",
                correlationId = correlationId
            )
        }

        return normalizedPath.substring(endpointIndex)
    }

    private fun getQueryParameters(uri: URI): LinkedHashMap<String, String> {
        val queryParameters = LinkedHashMap<String, String>()
        CommonURIBuilder(uri).queryParams.forEach { parameter ->
            queryParameters[parameter.name] = parameter.value.orEmpty()
        }
        return queryParameters
    }

    private fun stripLeadingTenantTemplate(href: String): String {
        val hrefWithoutLeadingSlash = href.removePrefix(PATH_SEPARATOR)
        return when {
            hrefWithoutLeadingSlash == TENANT_TEMPLATE -> ""
            hrefWithoutLeadingSlash.startsWith("$TENANT_TEMPLATE$PATH_SEPARATOR") ||
                hrefWithoutLeadingSlash.startsWith("$TENANT_TEMPLATE?") ||
                hrefWithoutLeadingSlash.startsWith("$TENANT_TEMPLATE#") ->
                hrefWithoutLeadingSlash.removePrefix(TENANT_TEMPLATE)
            else -> href
        }
    }

    private fun effectivePort(uri: URI): Int =
        if (uri.port >= 0) {
            uri.port
        } else if (uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true)) {
            HTTPS_DEFAULT_PORT
        } else {
            HTTP_DEFAULT_PORT
        }

    private fun effectivePort(url: URL): Int =
        if (url.port >= 0) url.port else url.defaultPort

    private fun clientException(
        errorCode: String,
        message: String,
        correlationId: String
    ): ClientException {
        val exception = ClientException(errorCode, message)
        exception.setCorrelationId(correlationId)
        return exception
    }

    private data class AuthorityEndpoint(
        val host: String,
        val port: Int
    )

    private companion object {
        private const val HTTPS_SCHEME = "https"
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_DEFAULT_PORT = 443
        private const val HTTP_DEFAULT_PORT = 80
        private const val NETWORK_PATH_PREFIX = "//"
        private const val PATH_SEPARATOR = "/"
        private const val TENANT_TEMPLATE = "{tenant}"
        private const val API_PATH_PREFIX = "/api/"
        private const val OAUTH2_PATH_PREFIX = "/oauth2/"
        private const val DC_QUERY_PARAMETER = "dc"

        private val ALLOWED_AUTHORITIES: Set<AuthorityEndpoint> = emptySet()
    }
}
