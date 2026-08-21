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
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.util.CommonURIBuilder
import com.microsoft.identity.common.java.util.StringUtil
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

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
            val normalizedHref = stripLeadingTenantTemplate(trimmedHref)
            if (normalizedHref.isBlank()) {
                throw clientException(
                    errorCode = ClientException.MALFORMED_URL,
                    message = "Native Auth V2 href resolves to an empty path after template stripping.",
                    correlationId = correlationId
                )
            }
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

        val authority = config.getNativeAuthV2AuthorityUrl(correlationId)
        val isHttps = scheme.equals(HTTPS_SCHEME, ignoreCase = true)
        val isAllowedMockHttp = scheme.equals(HTTP_SCHEME, ignoreCase = true) &&
            authority.protocol.equals(HTTP_SCHEME, ignoreCase = true)

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

        if (!isConfiguredAuthority) {
            throw clientException(
                errorCode = ClientException.UNKNOWN_AUTHORITY,
                message = "Native Auth V2 href points to an untrusted authority.",
                correlationId = correlationId
            )
        }

        validateSupportedApiPath(
            path = uri.path.orEmpty(),
            configuredAuthorityPath = normalizeConfiguredAuthorityPath(authority.path),
            correlationId = correlationId
        )

        val dc = BuildValues.getDC()
        if (dc.isEmpty()) {
            return uri.toURL()
        }

        // Reuse resolveQuery to preserve the raw query verbatim — same semantic guarantee as
        // resolveRelative — rather than round-tripping through CommonURIBuilder's query parser,
        // which silently drops pairs with an empty name (e.g. ?=novalue&y=2).
        val finalQuery = resolveQuery(uri)
        if (finalQuery.isNullOrEmpty()) {
            return uri.toURL()
        }

        // Reconstruct the URI preserving the original scheme/authority/path bytes exactly;
        // only the query portion is replaced. toASCIIString() is fully percent-encoded, and
        // the fragment check in validateUserInfoAndFragment guarantees rawFragment is null here.
        val rawUriString = uri.toASCIIString()
        val queryStart = rawUriString.indexOf('?')
        val base = if (queryStart >= 0) rawUriString.substring(0, queryStart) else rawUriString
        return URI("$base?$finalQuery").toURL()
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
        val apiPath = validateSupportedApiPath(
            path = uri.path.orEmpty(),
            configuredAuthorityPath = normalizeConfiguredAuthorityPath(authorityUrl.path),
            allowTenantIdPrefix = true,
            correlationId = correlationId
        )

        // Resolve path segments only here. The href's query is preserved separately, verbatim,
        // by resolveQuery below so that duplicate keys, ordering, and any not-yet-decoded
        // characters survive without a decode/re-encode round trip through the query parser.
        val pathBuilder = CommonURIBuilder()
        pathBuilder.setPath(apiPath)
        val apiSegments = pathBuilder.pathSegments
        rejectInvalidPathSegments(apiSegments, correlationId)

        val builder = CommonURIBuilder(authorityUrl.toString())
        val baseSegments = ArrayList(builder.pathSegments)
        if (baseSegments.isNotEmpty() && baseSegments.last() == "") {
            baseSegments.removeAt(baseSegments.size - 1)
        }
        baseSegments.addAll(apiSegments)
        builder.setPathSegments(baseSegments)

        val resolvedUri = builder.build()
        val finalQuery = resolveQuery(uri)

        return if (finalQuery.isNullOrEmpty()) {
            resolvedUri.toURL()
        } else {
            URI("$resolvedUri?$finalQuery").toURL()
        }
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

    /**
     * Preserves the href's raw query verbatim — duplicate keys, ordering, and un-decoded
     * characters intact — and appends `dc` exactly once when [BuildValues.getDC] is non-empty
     * and not already present.
     *
     * This deliberately avoids [CommonURIBuilder.setParameters] with a parsed/decoded
     * [NameValuePair] list to rebuild the query: that reconstruction depends on the query
     * parser's own (silent) handling of edge cases, such as dropping a pair with an empty name.
     * [CommonURIBuilder.getQueryParams] is used below *only* to answer the yes/no question of
     * whether a `dc` parameter is already present; that parsed view is never used to
     * reconstruct the emitted query, so the parser's handling of malformed pairs cannot alter
     * the bytes sent to the server. This has been verified against the httpcore5 versions
     * present for this repository (the pinned 5.3 and the 5.4.2 also resolved into the local
     * Gradle cache): both preserve duplicate keys and ordering identically for this query, so
     * the resolver does not depend on version-specific reconstruction behavior.
     */
    private fun resolveQuery(uri: URI): String? {
        val rawQuery = uri.rawQuery
        val dc = BuildValues.getDC()
        if (dc.isEmpty()) {
            return rawQuery
        }

        val dcAlreadyPresent = rawQuery != null && CommonURIBuilder(uri).queryParams.any {
            pair: NameValuePair -> pair.name.equals(DC_QUERY_PARAMETER, ignoreCase = true)
        }
        if (dcAlreadyPresent) {
            return rawQuery
        }

        // Build the dc-only query fragment in isolation so its value is percent-encoded through
        // the same codec path used elsewhere in this class, without touching the href's own
        // query bytes.
        val dcQuery = CommonURIBuilder()
            .setParameters(BasicNameValuePair(DC_QUERY_PARAMETER, dc))
            .build()
            .rawQuery

        return if (rawQuery.isNullOrEmpty()) dcQuery else "$rawQuery&$dcQuery"
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

    private fun validateSupportedApiPath(
        path: String,
        configuredAuthorityPath: String,
        allowTenantIdPrefix: Boolean = false,
        correlationId: String
    ): String {
        val normalizedPath = if (path.startsWith(PATH_SEPARATOR)) path else "$PATH_SEPARATOR$path"
        val apiPath = extractSupportedApiPath(
            normalizedPath = normalizedPath,
            configuredAuthorityPath = configuredAuthorityPath,
            allowTenantIdPrefix = allowTenantIdPrefix
        )
            ?: throw clientException(
                errorCode = ClientException.UNSUPPORTED_URL,
                message = "Native Auth V2 href does not contain a supported API path.",
                correlationId = correlationId
            )

        val pathBuilder = CommonURIBuilder()
        pathBuilder.setPath(apiPath)
        rejectInvalidPathSegments(pathBuilder.pathSegments, correlationId)
        return apiPath
    }

    private fun extractSupportedApiPath(
        normalizedPath: String,
        configuredAuthorityPath: String,
        allowTenantIdPrefix: Boolean
    ): String? {
        SUPPORTED_API_PATH_PREFIXES.firstOrNull { normalizedPath.startsWith(it) }?.let {
            return normalizedPath
        }

        if (configuredAuthorityPath.isNotEmpty()) {
            SUPPORTED_API_PATH_PREFIXES.firstOrNull { prefix ->
                normalizedPath.startsWith("$configuredAuthorityPath$prefix")
            }?.let {
                return normalizedPath.removePrefix(configuredAuthorityPath)
            }
        }

        if (allowTenantIdPrefix) {
            val tenantId = normalizedPath.removePrefix(PATH_SEPARATOR).substringBefore(PATH_SEPARATOR)
            if (tenantId.length == UUID_STRING_LENGTH && StringUtil.isUuid(tenantId)) {
                val tenantPath = "$PATH_SEPARATOR$tenantId"
                SUPPORTED_API_PATH_PREFIXES.firstOrNull { prefix ->
                    normalizedPath.startsWith("$tenantPath$prefix")
                }?.let {
                    return normalizedPath.removePrefix(tenantPath)
                }
            }
        }

        return null
    }

    private fun normalizeConfiguredAuthorityPath(path: String?): String {
        val normalizedPath = path.orEmpty()
        if (normalizedPath.isBlank() || normalizedPath == PATH_SEPARATOR) {
            return ""
        }

        return normalizedPath.trimEnd(PATH_SEPARATOR.first())
    }

    private fun stripLeadingTenantTemplate(href: String): String {
        // TODO: Remove this workaround when service hrefs can be appended directly to the
        // Authority without stripping the leading tenant template segment.
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
        private val TAG: String = NativeAuthV2HrefResolver::class.java.simpleName
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
        private const val CURRENT_DIRECTORY_SEGMENT = "."
        private const val PARENT_DIRECTORY_SEGMENT = ".."
        private const val UUID_STRING_LENGTH = 36
        private val SUPPORTED_API_PATH_PREFIXES = listOf(API_PATH_PREFIX, OAUTH2_PATH_PREFIX)
    }
}
