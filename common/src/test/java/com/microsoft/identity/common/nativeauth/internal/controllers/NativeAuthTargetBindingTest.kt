// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.nativeauth.internal.controllers

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class NativeAuthTargetBindingTest {
    private class Controller : BaseNativeAuthController() {
        fun complete(
            strategy: NativeAuthOAuth2Strategy,
            parameters: BaseSignInTokenCommandParameters,
            result: SignInTokenApiResult.Success
        ) = saveAndReturnTokens(strategy, parameters, result)

        fun complete(
            strategy: NativeAuthV2OAuth2Strategy,
            parameters: BaseSignInTokenCommandParameters,
            result: SignInTokenApiResult.Success
        ) = saveAndReturnTokens(strategy, parameters, result)
    }

    @Test
    fun nativeV1AndV2RejectMismatchBeforeCache() {
        val cache = mockk<OAuth2TokenCache<*, *, *>>(relaxed = true)
        val parameters = SignInStartCommandParameters.builder()
            .platformComponents(mockk<IPlatformComponents>(relaxed = true))
            .username("test@example.com")
            .clientId("test-client")
            .correlationId("test-correlation")
            .claimsRequestJson("""{"id_token":{"xms_agvs":{"additionalProperties":["target-mc"]}}}""")
            .oAuth2TokenCache(cache)
            .build()
        val strategy = mockk<NativeAuthOAuth2Strategy>()
        val strategyV2 = mockk<NativeAuthV2OAuth2Strategy>()
        every { strategy.getAuthority() } returns "https://example.com/tenant"
        every { strategyV2.getAuthority() } returns "https://example.com/tenant"
        val controller = Controller()

        for (receipt in listOf(null, "+sm")) {
            val response = MicrosoftStsTokenResponse().apply {
                idToken = PlainJWT(JWTClaimsSet.Builder()
                    .claim("xms_agvs", 0).claim("xms_agvt", receipt).build()).serialize()
            }
            val result = SignInTokenApiResult.Success("test-correlation", response)
            for (complete in listOf(
                { controller.complete(strategy, parameters, result) },
                { controller.complete(strategyV2, parameters, result) }
            )) {
                try {
                    complete()
                    fail("Mismatched native tokens must not be cached or returned")
                } catch (exception: ClientException) {
                    assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.errorCode)
                }
            }
        }
        verify { cache wasNot Called }
    }
}
