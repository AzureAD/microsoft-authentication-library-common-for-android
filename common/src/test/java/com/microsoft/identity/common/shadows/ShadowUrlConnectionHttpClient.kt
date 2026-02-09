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
package com.microsoft.identity.common.shadows

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.net.HttpRequest
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ported.Consumer
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.concurrent.atomic.AtomicInteger

/**
 * Result type for network behavior simulation.
 */
sealed class NetworkResult {
    data class Success(
        val statusCode: Int = 200,
        val body: String = "{}",
        val headers: Map<String, List<String>> = mapOf("Content-Type" to listOf("application/json"))
    ) : NetworkResult()

    data class Failure(val exception: ClientException) : NetworkResult()
}

/**
 * Basic Robolectric shadow for [UrlConnectionHttpClient].
 * Allows tests to define custom network behavior per request attempt.
 *
 * Usage examples:
 * ```
 * // Fail on first, succeed on second
 * ShadowUrlConnectionHttpClient.setBehavior { attempt ->
 *     if (attempt == 1) NetworkResult.Failure(ClientException("Network error"))
 *     else NetworkResult.Success(200, "{\"ok\":true}")
 * }
 *
 * // Always fail
 * ShadowUrlConnectionHttpClient.setBehavior { _ ->
 *     NetworkResult.Failure(ClientException("Always fails"))
 * }
 *
 * // Always succeed
 * ShadowUrlConnectionHttpClient.setBehavior { _ ->
 *     NetworkResult.Success(200, "{\"data\":\"value\"}")
 * }
 *
 * // Fail first two attempts, then succeed
 * ShadowUrlConnectionHttpClient.setBehavior { attempt ->
 *     if (attempt <= 2) NetworkResult.Failure(ClientException("Retry me"))
 *     else NetworkResult.Success(200, "{\"finally\":\"success\"}")
 * }
 * ```
 */
@Implements(UrlConnectionHttpClient::class)
@Suppress("unused")
class ShadowUrlConnectionHttpClient {
    /**
     * Shadow executeHttpSend to return the configured response based on behavior.
     */
    @Implementation
    @Throws(ClientException::class)
    fun executeHttpSend(
        request: HttpRequest,
        completionCallback: Consumer<HttpResponse?>
    ): HttpResponse {
        val currentAttempt = requestCount.incrementAndGet()

        return when (val result = networkBehavior(currentAttempt)) {
            is NetworkResult.Failure -> throw result.exception
            is NetworkResult.Success -> {
                val response = HttpResponse(
                    result.statusCode,
                    result.body,
                    result.headers.mapKeys { it.key }.mapValues { it.value.toMutableList() }.toMutableMap()
                )
                completionCallback.accept(response)
                response
            }
        }
    }

    companion object {
        private val requestCount = AtomicInteger(0)

        // Default behavior: always succeed with 200
        private var networkBehavior: (Int) -> NetworkResult = { _ ->
            NetworkResult.Success()
        }

        /**
         * Resets shadow state to defaults (always succeed with 200).
         */
        fun reset() {
            requestCount.set(0)
            networkBehavior = { _ -> NetworkResult.Success() }
        }

        /**
         * Sets the network behavior for the shadow.
         * The lambda receives the attempt number (1-based) and returns the result.
         *
         * @param behavior Lambda that takes attempt number and returns NetworkResult
         */
        fun setBehavior(behavior: (attempt: Int) -> NetworkResult) {
            networkBehavior = behavior
        }

        /**
         * Returns how many times executeHttpSend was called.
         *
         * @return request count.
         */
        fun getRequestCount(): Int {
            return requestCount.get()
        }
    }
}
