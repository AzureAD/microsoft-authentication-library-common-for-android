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
package com.microsoft.identity.common.internal.net

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.net.NoRetryPolicy
import com.microsoft.identity.common.java.net.StatusCodeAndExceptionRetry
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.shadows.NetworkResult
import com.microsoft.identity.common.shadows.ShadowUrlConnectionHttpClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.MalformedURLException
import java.net.URL

/**
 * Unit tests for [UrlConnectionHttpClient].
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowUrlConnectionHttpClient::class])
class UrlConnectionHttpClientTest {

    companion object {

        private val TEST_URL: URL
        private val TEST_HEADERS = mutableMapOf("Header1" to "Value1", "Header2" to "Value2")

        init {
            try {
                TEST_URL = URL("https://www.bing.com")
            } catch (e: MalformedURLException) {
                throw RuntimeException(e)
            }
        }
    }

    @Before
    fun setUp() {
        ShadowUrlConnectionHttpClient.reset()
    }

    /**
     * Verifies that a request succeeds on the first attempt when using NoRetryPolicy.
     * Expects: Single request, 200 status code.
     */
    @Test
    fun testGet_withNoRetryPolicy_succeedsOnFirstAttempt() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Success(200, "{\"data\":\"value\"}")
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(NoRetryPolicy())
            .build()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertNotNull(response)
        Assert.assertEquals(200, response.statusCode.toLong())
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that a request succeeds on the first attempt when using default retry policy.
     * Expects: Single request (no retries needed), 200 status code.
     */
    @Test
    fun testGet_withRetryPolicy_succeedsOnFirstAttempt() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Success(200, "{\"data\":\"value\"}")
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 1, null))
            .build()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertNotNull(response)
        Assert.assertEquals(200, response.statusCode.toLong())
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that a request retries after the first attempt fails and succeeds on the second.
     * Expects: Two requests total, final 200 status code.
     */
    @Test
    @Throws(Exception::class)
    fun testGet_withRetryPolicy_succeedsOnSecondAttemptAfterFailure() {
        ShadowUrlConnectionHttpClient.setBehavior { attempt ->
            if (attempt == 1) NetworkResult.Failure(
                ClientException(
                    ClientException.IO_ERROR,
                    "Simulated network error"
                )
            )
            else NetworkResult.Success(200, "{\"ok\":true}")
        }
        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 1, null))
            .build()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertNotNull(response)
        Assert.assertEquals(200, response.statusCode.toLong())
        Assert.assertEquals(2, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that when all retry attempts fail, an exception is thrown.
     * Expects: Two requests (initial + 1 retry), ClientException thrown.
     */
    @Test
    @Throws(Exception::class)
    fun testGet_withRetryPolicy_throwsExceptionWhenAllAttemptsFail() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Failure(
                ClientException(
                    ClientException.IO_ERROR,
                    "Simulated network error"
                )
            )
        }
        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 1, null))
            .build()

        Assert.assertThrows(ClientException::class.java) {
            urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        }
        Assert.assertEquals(2, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that HTTP 500 responses trigger a retry and succeed on the second attempt.
     * Expects: Two requests, final 200 status code after 500 error.
     */
    @Test
    fun testGet_withRetryPolicy_retriesOnHttp500AndSucceeds() {
        ShadowUrlConnectionHttpClient.setBehavior { attempt ->
            if (attempt == 1) NetworkResult.Success(500, "{\"error\":\"internal\"}")
            else NetworkResult.Success(200, "{\"ok\":true}")
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient.getDefaultInstance()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertEquals(200, response.statusCode)
        Assert.assertEquals(2, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that SocketTimeoutException is not retried even with retry policy enabled.
     * Expects: Single request (no retry), ClientException thrown immediately.
     */
    @Test
    @Throws(Exception::class)
    fun testGet_withRetryPolicy_doesNotRetryOnSocketTimeoutException() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Failure(
                ClientException(
                    ClientException.IO_ERROR,
                    "Socket timeout",
                    java.net.SocketTimeoutException("Connection timed out")
                )
            )
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 1, null))
            .build()

        Assert.assertThrows(ClientException::class.java) {
            urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        }
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that retry policy with 0 retries behaves like no retry (only initial attempt).
     * Expects: Single request, ClientException thrown on failure.
     */
    @Test
    fun testGet_withZeroRetries_failsImmediately() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Failure(
                ClientException(
                    ClientException.IO_ERROR,
                    "Network error"
                )
            )
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 0, null))
            .build()

        Assert.assertThrows(ClientException::class.java) {
            urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        }
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that retry policy with 2 retries allows up to 3 total attempts.
     * Expects: Three requests (initial + 2 retries), final success on third attempt.
     */
    @Test
    fun testGet_withTwoRetries_succeedsOnThirdAttempt() {
        ShadowUrlConnectionHttpClient.setBehavior { attempt ->
            when (attempt) {
                1, 2 -> NetworkResult.Failure(
                    ClientException(
                        ClientException.IO_ERROR,
                        "Temporary error"
                    )
                )
                else -> NetworkResult.Success(200, "{\"success\":true}")
            }
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 2, null))
            .build()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertEquals(200, response.statusCode)
        Assert.assertEquals(3, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that all 3 attempts fail when retries are exhausted with 2 retries configured.
     * Expects: Three requests (initial + 2 retries), ClientException thrown.
     */
    @Test
    fun testGet_withTwoRetries_failsAfterAllAttemptsExhausted() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Failure(
                ClientException(
                    ClientException.IO_ERROR,
                    "Persistent error"
                )
            )
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 2, null))
            .build()

        Assert.assertThrows(ClientException::class.java) {
            urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        }
        Assert.assertEquals(3, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that getIOExceptionRetryPolicy rejects negative retry counts.
     */
    @Test
    fun testGetIOExceptionRetryPolicy_withNegativeRetries_throwsException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", -1, null)
        }
    }

    /**
     * Verifies that non-IO exceptions are not retried.
     */
    @Test
    fun testGet_withNonIOException_doesNotRetry() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Failure(
                ClientException(
                    "DIFFERENT_ERROR",
                    "Not an IO error"
                )
            )
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 2, null))
            .build()

        Assert.assertThrows(ClientException::class.java) {
            urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        }
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }

    /**
     * Verifies that with attributeNameForRetry set, policy executes successfully.
     */
    @Test
    fun testGet_withAttributeName_executesSuccessfully() {
        ShadowUrlConnectionHttpClient.setBehavior { _ ->
            NetworkResult.Success(200, "{\"data\":\"value\"}")
        }

        val urlConnectionHttpClient = UrlConnectionHttpClient
            .builder()
            .connectTimeoutMs(1000)
            .readTimeoutMs(1000)
            .retryPolicy(
                StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("TEST", 1, "test_attribute")
            )
            .build()

        val response = urlConnectionHttpClient.get(TEST_URL, TEST_HEADERS)
        Assert.assertEquals(200, response.statusCode)
        Assert.assertEquals(1, ShadowUrlConnectionHttpClient.getRequestCount())
    }
}



