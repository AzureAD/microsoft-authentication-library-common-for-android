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
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.util.CommonURIBuilder
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/**
 * Resolves Native Auth V2 server-provided hrefs against the configured authority.
 *
 * Relative hrefs supply the entire path and query beneath the configured origin; the configured
 * tenant path and developer-supplied `dc` never override service routing. Absolute HTTPS hrefs are
 * followed as returned, including a different origin, with warnings for authority or slice mismatches.
 * Invalid paths and insecure non-mock URLs are rejected before receiving sensitive flow state.
 */
class NativeAuthV2HrefResolver(private val config: NativeAuthOAuth2Configuration) {

    /**
     * Resolves a server-provided `_links` href into an absolute request URL.
     *
     * @param href Server-provided absolute or origin-relative href, including its query.
     * @param correlationId Correlation ID to attach to any validation error.
     * @return The service URL, using the configured origin only for relative hrefs.
     * @throws ClientException If the href is malformed or uses a disallowed scheme or path.
     */
    fun resolve(href: String, correlationId: String): URL {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.resolve"
        )

        val trimmedHref = href.trim()
        if (trimmedHref.isBlank()) {
            throw clientException(
                errorCode = ClientException.MALFORMED_URL,
                message = "Native Auth V2 href must not be blank.",
                correlationId = correlationId
            )
        }

        return try {
            if (trimmedHref.startsWith(NETWORK_PATH_PREFIX)) {
                throw clientException(
                    errorCode = ClientException.UNSUPPORTED_URL,
                    message = "Native Auth V2 network-path hrefs are not supported.",
                    correlationId = correlationId
                )
            }

            val uri = URI(trimmedHref)
            validateUserInfoAndFragment(uri, correlationId)
            val resolved = if (uri.isAbsolute) {
                resolveAbsolute(uri, correlationId)
            } else {
                resolveRelative(uri, correlationId)
            }
            warnOnRoutingMismatch(resolved.toURI(), correlationId)
            resolved
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

        val authority = config.getNativeAuthV2AuthorityUrl(correlationId)
        val isHttps = scheme.equals(HTTPS_SCHEME, ignoreCase = true)
        val isAllowedMockHttp = scheme.equals(HTTP_SCHEME, ignoreCase = true) &&
            authority.protocol.equals(HTTP_SCHEME, ignoreCase = true) &&
            hasSameOrigin(uri, authority)

        if (!isHttps && !isAllowedMockHttp) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href uses a disallowed scheme.",
                correlationId = correlationId
            )
        }

        validateSupportedApiPath(uri.path.orEmpty(), correlationId)
        return uri.toURL()
    }

    private fun resolveRelative(uri: URI, correlationId: String): URL {
        if (uri.rawAuthority != null) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 network-path hrefs are not supported.",
                correlationId = correlationId
            )
        }

        val authorityUrl = config.getNativeAuthV2AuthorityUrl(correlationId)
        validateSupportedApiPath(uri.path.orEmpty(), correlationId)
        // Do not decode/re-encode service paths or queries: routing hints and duplicate query
        // parameters must survive byte-for-byte, and no configured query may leak into this URL.
        val pathAndQuery = uri.toString().let { if (it.startsWith(PATH_SEPARATOR)) it else "/$it" }
        return URI("${authorityUrl.protocol}://${authorityUrl.authority}$pathAndQuery").toURL()
    }

    /**
     * Rejects a supported API path whose segments contain an internal empty segment (i.e. a
     * double slash occurring anywhere except as a single trailing slash). Silently dropping such
     * segments would change the semantics of a malformed path instead of surfacing it as an error.
     * A single trailing empty segment (a plain trailing slash) is permitted, and the single
     * required leading slash never produces a leading empty entry because [CommonURIBuilder]
     * already skips exactly one leading separator when parsing path segments.
     */
    private fun rejectInvalidPathSegments(segments: List<String>, correlationId: String) {
        val lastIndex = segments.size - 1
        segments.forEachIndexed { index, segment ->
            if (segment.isEmpty() && index != lastIndex) {
                throw clientException(
                    errorCode = ClientException.MALFORMED_URL,
                    message = "Native Auth V2 href contains an empty path segment.",
                    correlationId = correlationId
                )
            }
            if (segment == CURRENT_DIRECTORY_SEGMENT || segment == PARENT_DIRECTORY_SEGMENT) {
                throw clientException(
                    errorCode = ClientException.MALFORMED_URL,
                    message = "Native Auth V2 href contains a dot path segment.",
                    correlationId = correlationId
                )
            }
        }
    }

    private fun warnOnRoutingMismatch(uri: URI, correlationId: String) {
        val authority = config.getNativeAuthV2AuthorityUrl(correlationId)
        val configuredTenant = authority.path.trimStart('/').substringBefore('/')
        val returnedTenant = uri.path.orEmpty().trimStart('/').substringBefore('/')
        if (!hasSameOrigin(uri, authority) || !returnedTenant.equals(configuredTenant, ignoreCase = true)) {
            Logger.warn(
                TAG, correlationId,
                "Native Auth V2 service URL authority differs from the configured authority; following the service URL."
            )
        }
        val configuredSlices = BuildValues.getDC().takeIf { it.isNotEmpty() }?.let { listOf(it) }
            ?: slices(authority.toURI())
        if (configuredSlices != slices(uri)) {
            Logger.warn(
                TAG, correlationId,
                "Native Auth V2 service test slice differs from the configured test slice; following service routing and ignoring developer dc."
            )
        }
    }

    private fun slices(uri: URI): List<String?> = CommonURIBuilder(uri).queryParams
        .filter { it.name.equals(DC_QUERY_PARAMETER, ignoreCase = true) }
        .map { it.value }

    private fun hasSameOrigin(uri: URI, authority: URL): Boolean =
        uri.scheme.equals(authority.protocol, ignoreCase = true) &&
            uri.host.equals(authority.host, ignoreCase = true) &&
            effectivePort(uri) == effectivePort(authority)

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

    private fun validateSupportedApiPath(
        path: String,
        correlationId: String
    ) {
        val normalizedPath = if (path.startsWith(PATH_SEPARATOR)) path else "$PATH_SEPARATOR$path"
        val pathAfterTenant = normalizedPath.removePrefix(PATH_SEPARATOR)
            .substringAfter(PATH_SEPARATOR, "")
            .let { "$PATH_SEPARATOR$it" }
        if (SUPPORTED_API_PATH_PREFIXES.none {
                normalizedPath.startsWith(it) || pathAfterTenant.startsWith(it)
            }) {
            throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href does not contain a supported API path.",
                correlationId = correlationId
            )
        }
        val pathBuilder = CommonURIBuilder()
        pathBuilder.setPath(normalizedPath)
        rejectInvalidPathSegments(pathBuilder.pathSegments, correlationId)
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

    private companion object {
        private val TAG: String = NativeAuthV2HrefResolver::class.java.simpleName
        private const val HTTPS_SCHEME = "https"
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_DEFAULT_PORT = 443
        private const val HTTP_DEFAULT_PORT = 80
        private const val NETWORK_PATH_PREFIX = "//"
        private const val PATH_SEPARATOR = "/"
        private const val API_PATH_PREFIX = "/api/"
        private const val OAUTH2_PATH_PREFIX = "/oauth2/"
        private const val DC_QUERY_PARAMETER = "dc"
        private const val CURRENT_DIRECTORY_SEGMENT = "."
        private const val PARENT_DIRECTORY_SEGMENT = ".."
        private val SUPPORTED_API_PATH_PREFIXES = listOf(API_PATH_PREFIX, OAUTH2_PATH_PREFIX)
    }
}
