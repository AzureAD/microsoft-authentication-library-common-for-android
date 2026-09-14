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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.net.HttpConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URL

/**
 * Guards against Native Auth V1 and V2 drifting apart on the SDK identity headers they send.
 *
 * Both versions build their headers from [NativeAuthSdkHeaders], so a change to the correlation id
 * handling or the SDK product/version/platform fields must apply to both. Without this test that
 * drift is invisible in review, because the two providers live in different packages.
 *
 * This test is deliberately temporary: it exists only while V1 and V2 ship side by side, and should
 * be deleted along with [NativeAuthRequestProvider] when V1 is retired.
 */
class NativeAuthSdkHeaderParityTest {

    @Test
    fun v1AndV2SendIdenticalSdkHeadersForTheSameFormUrlEncodedRequest() {
        val v1Headers = v1FormUrlEncodedHeaders(CORRELATION_ID)
        val v2Headers = v2FormUrlEncodedHeaders(CORRELATION_ID)

        assertEquals(v1Headers, v2Headers)
        assertEquals(CORRELATION_ID, v2Headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
        assertEquals(
            NativeAuthContentType.FORM_URL_ENCODED.value,
            v2Headers[HttpConstants.HeaderField.CONTENT_TYPE]
        )
        assertFalse(v2Headers[AuthenticationConstants.SdkPlatformFields.PRODUCT].isNullOrBlank())
        assertFalse(v2Headers[AuthenticationConstants.SdkPlatformFields.VERSION].isNullOrBlank())
    }

    @Test
    fun v1AndV2BothOmitTheClientRequestIdWhenTheCorrelationIdIsUnset() {
        val v1Headers = v1FormUrlEncodedHeaders(NativeAuthSdkHeaders.UNSET_CORRELATION_ID)
        val v2Headers = v2FormUrlEncodedHeaders(NativeAuthSdkHeaders.UNSET_CORRELATION_ID)

        assertNull(v1Headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
        assertNull(v2Headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
        assertEquals(v1Headers, v2Headers)
    }

    /**
     * V2 sends JSON to the HAL endpoints, so only the `Content-Type` may differ from V1.
     */
    @Test
    fun v2JsonHeadersDifferFromV1OnlyByContentType() {
        val v1Headers = v1FormUrlEncodedHeaders(CORRELATION_ID)
        val v2JsonHeaders = NativeAuthSdkHeaders.base(CORRELATION_ID).also {
            it[HttpConstants.HeaderField.CONTENT_TYPE] = NativeAuthContentType.JSON.value
        }

        assertEquals(
            v1Headers - HttpConstants.HeaderField.CONTENT_TYPE,
            v2JsonHeaders - HttpConstants.HeaderField.CONTENT_TYPE
        )
        assertEquals(
            NativeAuthContentType.JSON.value,
            v2JsonHeaders[HttpConstants.HeaderField.CONTENT_TYPE]
        )
    }

    private fun v1FormUrlEncodedHeaders(correlationId: String): Map<String, String?> =
        NativeAuthRequestProvider(config()).createResetPasswordChallengeRequest(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        ).headers

    private fun v2FormUrlEncodedHeaders(correlationId: String): Map<String, String?> =
        NativeAuthV2RequestProvider(config())
            .createAuthorizeChallengeStartRequest(correlationId)
            .headers

    private fun config() = NativeAuthOAuth2Configuration(
        authorityUrl = URL(AUTHORITY_URL),
        clientId = CLIENT_ID,
        challengeType = CHALLENGE_TYPE,
        capabilities = null,
        requestInterceptor = null,
        useMockApiForNativeAuth = false,
        MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = MOCK_API_URL
    )

    private companion object {
        private const val AUTHORITY_URL = "https://login.contoso.com/tenant"
        private const val MOCK_API_URL = "https://localhost/mock-tenant"
        private const val CLIENT_ID = "client-id"
        private const val CHALLENGE_TYPE = "oob"
        private const val CORRELATION_ID = "correlation-id"
        private const val CONTINUATION_TOKEN = "continuation-token"
    }
}
