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

import com.microsoft.identity.common.nativeauth.ApiConstants
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import com.microsoft.identity.common.nativeauth.MockApiUtils.Companion.configureMockApi
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class ResetPasswordScenarioTest {
    private val username = "user@email.com"
    private val password = "verySafePassword".toCharArray()
    private val tenant = "samtoso.onmicrosoft.com"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val challengeType = "oob redirect"
    private val oobCode = "123456"
    private val passwordTooWeak = "password_too_weak"
    private val invalidGrant = "invalid_grant"

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
    // Scenario 3.1.1: Verify email with email OTP first and then reset password
    @Test
    fun testResetPasswordScenarioEmailVerificationThenResetPassword() {
        var continuationToken: String
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        continuationToken = (ssprStartResult as ResetPasswordStartApiResult.Success).continuationToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            continuationToken = continuationToken
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        continuationToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).continuationToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getContinuationToken() } returns continuationToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.PasswordRequired)
        continuationToken = (ssprContinueResult as ResetPasswordContinueApiResult.PasswordRequired).continuationToken

        // Call /submit
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns continuationToken
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        Assert.assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        continuationToken = (ssprSubmitResult as ResetPasswordSubmitApiResult.SubmitSuccess).continuationToken

        // Call /poll_completion
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        val ssprPollResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            continuationToken = continuationToken
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
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
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
        var continuationToken: String
        val correlationId = UUID.randomUUID().toString()

        // Call /start
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        continuationToken = (ssprStartResult as ResetPasswordStartApiResult.Success).continuationToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            continuationToken = continuationToken
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        continuationToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).continuationToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getContinuationToken() } returns continuationToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.PasswordRequired)
        continuationToken = (ssprContinueResult as ResetPasswordContinueApiResult.PasswordRequired).continuationToken

        // Call /submit
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns continuationToken
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        Assert.assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        Assert.assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        Assert.assertEquals(
            (ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error,
            invalidGrant
        )
        Assert.assertEquals(
            (ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError,
            passwordTooWeak
        )

        // Call /poll_completion
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_FAILED
        )
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password
        val ssprPollResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            continuationToken = continuationToken
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
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
        var continuationToken = (ssprStartResult as ResetPasswordStartApiResult.Success).continuationToken

        // Call /challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            continuationToken = continuationToken.toString()
        )
        Assert.assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        continuationToken = (ssprChallengeResult as ResetPasswordChallengeApiResult.CodeRequired).continuationToken

        // Call /continue
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )
        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getContinuationToken() } returns continuationToken
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )
        Assert.assertTrue(ssprContinueResult is ResetPasswordContinueApiResult.CodeIncorrect)
    }
}
