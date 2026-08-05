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
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2ResponseParserTest {
    private val parser = NativeAuthV2ResponseParser()

    @Test
    fun parseInteraction_whenActionMissing_returnsUnknownErrorForMissingActionField() {
        val response = responseFrom("""{"continuationToken":"token"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        assertFalse(result is NativeAuthV2InteractionApiResult.UnsupportedAction)

        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("action"))
    }

    @Test
    fun parseInteraction_whenActionUnknown_returnsUnsupportedActionWithExactRawValue() {
        val response = responseFrom(
            """{"continuationToken":"token","action":"mystery-action"}"""
        )

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnsupportedAction)

        val unsupported = result as NativeAuthV2InteractionApiResult.UnsupportedAction
        assertEquals("mystery-action", unsupported.rawAction)
        assertEquals(ApiErrorResult.INVALID_STATE, unsupported.error)
        assertTrue(unsupported.errorDescription.contains("mystery-action"))
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun previousState(): NativeAuthV2ContinuationState {
        val seedResponse = responseFrom("""{"continuationToken":"seed"}""")
        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = seedResponse,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD
        )!!
    }

    private companion object {
        private const val CORRELATION_ID = "corr-123"
    }
}
