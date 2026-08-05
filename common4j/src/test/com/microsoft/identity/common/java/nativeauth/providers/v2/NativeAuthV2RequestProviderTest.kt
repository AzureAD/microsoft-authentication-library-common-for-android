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

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class NativeAuthV2RequestProviderTest {
    private val hrefResolver = mockk<NativeAuthV2HrefResolver>()

    private val provider = NativeAuthV2RequestProvider(
        config = mockk<NativeAuthOAuth2Configuration> {
            every { clientId } returns CLIENT_ID
            every { getAuthorizeChallengeEndpoint() } returns URL("https://contoso.com/authorize-challenge")
            every { getSignInTokenEndpoint() } returns URL("https://contoso.com/token")
        },
        hrefResolver = hrefResolver
    )

    @Test
    fun createUpdatePasswordRequest_whenUpdateRelationPresent_prefersUpdateRelation() {
        val state = continuationState(
            NativeAuthV2LinkRelation.UPDATE to "/password/update",
            NativeAuthV2LinkRelation.SELF to "/password/self"
        )
        val expectedUrl = URL("https://contoso.com/password/update")
        every { hrefResolver.resolve("/password/update", CORRELATION_ID) } returns expectedUrl

        val request = provider.createUpdatePasswordRequest(state, NEW_PASSWORD)

        assertEquals(expectedUrl, request.requestUrl)
        verify(exactly = 1) { hrefResolver.resolve("/password/update", CORRELATION_ID) }
        verify(exactly = 0) { hrefResolver.resolve("/password/self", CORRELATION_ID) }
    }

    @Test
    fun createUpdatePasswordRequest_whenUpdateRelationMissing_fallsBackToSelfRelation() {
        val state = continuationState(
            NativeAuthV2LinkRelation.SELF to "/password/self"
        )
        val expectedUrl = URL("https://contoso.com/password/self")
        every { hrefResolver.resolve("/password/self", CORRELATION_ID) } returns expectedUrl

        val request = provider.createUpdatePasswordRequest(state, NEW_PASSWORD)

        assertEquals(expectedUrl, request.requestUrl)
        verify(exactly = 1) { hrefResolver.resolve("/password/self", CORRELATION_ID) }
        verify(exactly = 0) { hrefResolver.resolve("/password/update", CORRELATION_ID) }
    }

    private fun continuationState(vararg links: Pair<NativeAuthV2LinkRelation, String>): NativeAuthV2ContinuationState {
        val linksJson = links.joinToString(",") { (relation, href) ->
            "\"${relation.value}\":{\"href\":\"$href\"}"
        }
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from("""{"continuationToken":"token","_links":{$linksJson}}"""),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = response,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD
        )!!
    }

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CORRELATION_ID = "correlation-id"
        private val NEW_PASSWORD = "newPassword!".toCharArray()
    }
}
