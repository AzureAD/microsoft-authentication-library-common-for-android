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

import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartUsingPasswordCommandParameters
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
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL
import java.util.UUID

class SignUpScenarioTest {
    private val username = "user@email.com"
    private val email = "user@email.com"
    private val password = "verySafePassword"
    private val tenant = "samtoso.onmicrosoft.com"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val signUpContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/continue")
    private val signInInitiateRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/initiate")
    private val signInChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/challenge")
    private val signInTokenRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/token")
    private val ssprStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val ssprChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val ssprContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val ssprSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val ssprPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeType = "oob redirect"
    private val oobCode = "123456"

    private val mockConfig = mock<NativeAuthOAuth2Configuration>()
    private val mockStrategyParams = mock<OAuth2StrategyParameters>()

    private lateinit var nativeAuthOAuth2Strategy: NativeAuthOAuth2Strategy

    @Before
    fun setup() {
        whenever(mockConfig.clientId).thenReturn(clientId)
        whenever(mockConfig.tokenEndpoint).thenReturn(tokenEndpoint)
        whenever(mockConfig.getSignUpStartEndpoint()).thenReturn(signUpStartRequestUrl)
        whenever(mockConfig.getSignUpChallengeEndpoint()).thenReturn(signUpChallengeRequestUrl)
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(signUpContinueRequestUrl)
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(signInTokenRequestUrl)
        whenever(mockConfig.getResetPasswordStartEndpoint()).thenReturn(ssprStartRequestUrl)
        whenever(mockConfig.getResetPasswordChallengeEndpoint()).thenReturn(ssprChallengeRequestUrl)
        whenever(mockConfig.getResetPasswordContinueEndpoint()).thenReturn(ssprContinueRequestUrl)
        whenever(mockConfig.getResetPasswordSubmitEndpoint()).thenReturn(ssprSubmitRequestUrl)
        whenever(mockConfig.getResetPasswordPollCompletionEndpoint()).thenReturn(ssprPollCompletionRequestUrl)
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
            endpointType = MockApiEndpointType.SignUpStart,
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
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        assertTrue(signupChallengeResult is SignUpChallengeApiResult.OOBRequired)
        signUpToken = (signupChallengeResult as SignUpChallengeApiResult.OOBRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
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
            endpointType = MockApiEndpointType.SignUpStart,
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
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        assertTrue(signupChallengeResult is SignUpChallengeApiResult.OOBRequired)
        signUpToken = (signupChallengeResult as SignUpChallengeApiResult.OOBRequired).signupToken

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            mockSignUpContinueCommandParameters
        )
        assertTrue(signupContinueResult is SignUpContinueApiResult.Success)
    }
}
