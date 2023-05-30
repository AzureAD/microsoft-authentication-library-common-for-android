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

package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.scenario

import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL
import java.util.UUID

class ResetPasswordScenarioTest {
    private val username = "user@email.com"
    private val password = "verySafePassword"
    private val tenant = "samtoso.onmicrosoft.com"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val signUpContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/continue")
    private val signInInitiateRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/initiate")
    private val signInChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/challenge")
    private val signInTokenRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/token")
    private val resetPasswordStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val resetPasswordChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val resetPasswordContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val resetPasswordSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val resetPasswordPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
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
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(signInTokenRequestUrl)
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(signUpContinueRequestUrl)
        whenever(mockConfig.getResetPasswordStartEndpoint()).thenReturn(resetPasswordStartRequestUrl)
        whenever(mockConfig.getResetPasswordChallengeEndpoint()).thenReturn(resetPasswordChallengeRequestUrl)
        whenever(mockConfig.getResetPasswordContinueEndpoint()).thenReturn(resetPasswordContinueRequestUrl)
        whenever(mockConfig.getResetPasswordSubmitEndpoint()).thenReturn(resetPasswordSubmitRequestUrl)
        whenever(mockConfig.getResetPasswordPollCompletionEndpoint()).thenReturn(resetPasswordPollCompletionRequestUrl)
        whenever(mockConfig.challengeType).thenReturn(challengeType)

        nativeAuthOAuth2Strategy = NativeAuthOAuth2Strategy(
            config = mockConfig,
            strategyParameters = mockStrategyParams,
            signInInteractor = SignInInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            signUpInteractor = SignUpInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            resetPasswordInteractor = ResetPasswordInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.1: Verify email with email OTP first and then reset password
    @Test
    fun testResetPasswordScenarioEmailVerificationThenResetPassword() {
        var passwordResetToken: String
        val passwordSubmitToken: String
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        passwordResetToken = (ssprStartResult as ResetPasswordStartApiResult.Success).passwordResetToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        passwordResetToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).passwordResetToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getPasswordResetToken() } returns passwordResetToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.PasswordRequired)
        passwordSubmitToken = (ssprContinueResult as ResetPasswordContinueApiResult.PasswordRequired).passwordSubmitToken

        // Call /submit
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns passwordSubmitToken
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        Assert.assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        passwordResetToken = (ssprSubmitResult as ResetPasswordSubmitApiResult.SubmitSuccess).passwordResetToken

        // Call /poll_completion
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        val ssprPollResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprPollResult is ResetPasswordPollCompletionApiResult.PollingSucceeded)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.4: Email is not found in records
    @Test
    fun testResetPasswordScenarioUserNotFound() {
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.EXPLICIT_USER_NOT_FOUND
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )

        Assert.assertFalse(ssprStartResult is ResetPasswordStartApiResult.Success)
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.UserNotFound)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.8: New password being set does not meet password complexity requirements set on portal
    @Test
    fun testResetPasswordScenarioPasswordComplexity() {
        var passwordResetToken: String
        val passwordSubmitToken: String
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        passwordResetToken = (ssprStartResult as ResetPasswordStartApiResult.Success).passwordResetToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        passwordResetToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).passwordResetToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getPasswordResetToken() } returns passwordResetToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.PasswordRequired)
        passwordSubmitToken = (ssprContinueResult as ResetPasswordContinueApiResult.PasswordRequired).passwordSubmitToken

        // Call /submit
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns passwordSubmitToken
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        Assert.assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        Assert.assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        Assert.assertEquals(
            (ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode,
            "password_too_weak"
        )

        // Call /poll_completion
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_FAILED
        )
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprPollResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprPollResult is ResetPasswordPollCompletionApiResult.PollingFailed)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.9: Continuous attempts to reset password for single email with wrong OTP
    @Test
    fun testResetPasswordScenarioSingleEmailWrongOTP() {
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        var passwordResetToken = (ssprStartResult as ResetPasswordStartApiResult.Success).passwordResetToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken.toString()
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        passwordResetToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).passwordResetToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.EXPLICIT_INVALID_OOB_VALUE
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getPasswordResetToken() } returns passwordResetToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.CodeIncorrect)
    }
}
