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

package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.integration.scenario

import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.ApiConstants
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpoint
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class SignUpScenarioTest {
    private val username = "user@email.com"
    private val email = "user@email.com"
    private val password = "verySafePassword"
    private val tenant = "samtoso.onmicrosoft.com"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val challengeType = "oob redirect"
    private val oobCode = "123456"

    private val mockConfig = mock<NativeAuthOAuth2Configuration>()
    private val mockStrategyParams = mock<OAuth2StrategyParameters>()

    private lateinit var nativeAuthOAuth2Strategy: NativeAuthOAuth2Strategy

    @Before
    fun setup() {
        whenever(mockConfig.clientId).thenReturn(clientId)
        whenever(mockConfig.tokenEndpoint).thenReturn(ApiConstants.tokenEndpoint)
        whenever(mockConfig.getSignUpStartEndpoint()).thenReturn(ApiConstants.signUpStartRequestUrl)
        whenever(mockConfig.getSignUpChallengeEndpoint()).thenReturn(ApiConstants.signUpChallengeRequestUrl)
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(ApiConstants.signUpContinueRequestUrl)
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(ApiConstants.signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(ApiConstants.signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(ApiConstants.signInTokenRequestUrl)
        whenever(mockConfig.getResetPasswordStartEndpoint()).thenReturn(ApiConstants.ssprStartRequestUrl)
        whenever(mockConfig.getResetPasswordChallengeEndpoint()).thenReturn(ApiConstants.ssprChallengeRequestUrl)
        whenever(mockConfig.getResetPasswordContinueEndpoint()).thenReturn(ApiConstants.ssprContinueRequestUrl)
        whenever(mockConfig.getResetPasswordSubmitEndpoint()).thenReturn(ApiConstants.ssprSubmitRequestUrl)
        whenever(mockConfig.getResetPasswordPollCompletionEndpoint()).thenReturn(ApiConstants.ssprPollCompletionRequestUrl)
        whenever(mockConfig.challengeType).thenReturn(challengeType)

        nativeAuthOAuth2Strategy = NativeAuthOAuth2Strategy(
            config = mockConfig,
            strategyParameters = mockStrategyParams,
            signInInteractor = SignInInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            signUpInteractor = SignUpInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            resetPasswordInteractor = ResetPasswordInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 1.1.1: Set email and password and then verify email OOB as last step
    @Test
    fun testSignUpScenarioEmailPasswordWithOOBVerification() {
        var signUpToken: String
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        val mockSignUpStartCommandParameters = SignUpStartUsingPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .password(password)
            .build()

        val signupStartResult = nativeAuthOAuth2Strategy.performSignUpStartUsingPassword(
            mockSignUpStartCommandParameters
        )
        assertTrue(signupStartResult is SignUpStartApiResult.VerificationRequired)
        signUpToken = (signupStartResult as SignUpStartApiResult.VerificationRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        assertTrue(signupChallengeResult is SignUpChallengeApiResult.OOBRequired)
        signUpToken = (signupChallengeResult as SignUpChallengeApiResult.OOBRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .signupToken(signUpToken)
            .code(oobCode)
            .build()

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpSubmitCode(
            mockSignUpContinueCommandParameters
        )
        assertTrue(signupContinueResult is SignUpContinueApiResult.Success)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 1.1.7: Verify email address using email OTP and then set password
    @Test
    fun testSignUpScenarioEmailVerificationFirstThenPassword() {
        var signUpToken: String
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        val mockSignUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .build()

        val signupStartResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )

        assertTrue(signupStartResult is SignUpStartApiResult.VerificationRequired)
        signUpToken = (signupStartResult as SignUpStartApiResult.VerificationRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        assertTrue(signupChallengeResult is SignUpChallengeApiResult.OOBRequired)
        signUpToken = (signupChallengeResult as SignUpChallengeApiResult.OOBRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .signupToken(signUpToken)
            .code(oobCode)
            .build()

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpSubmitCode(
            mockSignUpContinueCommandParameters
        )
        assertTrue(signupContinueResult is SignUpContinueApiResult.Success)
    }
}
