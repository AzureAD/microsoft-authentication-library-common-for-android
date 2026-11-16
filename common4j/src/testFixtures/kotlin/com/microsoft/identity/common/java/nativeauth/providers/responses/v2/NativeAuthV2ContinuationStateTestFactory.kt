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

import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario

/**
 * Creates representative continuation states for tests in downstream repositories without
 * exposing or depending on the state's private constructor.
 */
object NativeAuthV2ContinuationStateTestFactory {

    /** Models an enrollment offer containing SMS and email methods in server order. */
    @JvmStatic
    fun registration(state: NativeAuthV2ContinuationState): NativeAuthV2ContinuationState {
        val response = NativeAuthV2HalApiResponse.from(HalResource.from("""
            {"action":"enroll","continuationToken":"registration","_embedded":{"methods":[
              {"type":"phone","channels":["sms"],"_links":{"enroll":{"href":"/tenant/api/v2.0/auth/phone/enroll"}}},
              {"type":"email","_links":{"enroll":{"href":"/tenant/api/v2.0/auth/email/enroll"}}}
            ]}}
        """.trimIndent()), 200, state.correlationId)
        return (NativeAuthV2ResponseParser().parseInteraction(response, state)
            as NativeAuthV2InteractionApiResult.RegistrationRequired).continuationState
    }

    /** Models the completion-stage marker set by the real update-password interactor. */
    @JvmStatic
    fun afterPasswordUpdate(state: NativeAuthV2ContinuationState): NativeAuthV2ContinuationState =
        state.withResetPasswordUpdateSubmitted()

    @JvmStatic
    fun create(
        correlationId: String,
        scopes: List<String> = listOf("scope"),
        entryRelation: NativeAuthV2LinkRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
        scenario: NativeAuthV2FlowScenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
        claimsRequestJson: String? = null
    ): NativeAuthV2ContinuationState {
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(
                """
                {
                  "continuation_token": "opaque-token",
                  "_links": {
                    "${entryRelation.value}": {
                      "href": "/tenant/${entryRelation.value}"
                    }
                  }
                }
                """.trimIndent()
            ),
            statusCode = 200,
            correlationId = correlationId
        )
        val result = NativeAuthV2ResponseParser().parseAuthorizeChallenge(
            response = response,
            entryRelation = entryRelation,
            scenario = scenario,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson
        )

        return (result as AuthorizeChallengeApiResult.ContinuationRequired).continuationState
    }
}
