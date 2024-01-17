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
package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.integration

import android.os.Build
import com.microsoft.identity.common.nativeauth.ApiConstants
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import com.microsoft.identity.common.nativeauth.MockApiUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.UUID

/**
 * These are integration tests using real API responses instead of mocked API responses. This class
 * covers all sign up endpoints.
 * These tests run on the mock API, see: https://native-auth-mock-api.azurewebsites.net/
 */
@RunWith(
    RobolectricTestRunner::class
)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest(DiagnosticContext::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SignInOAuthStrategyTest {
    private val USERNAME = "user@email.com"
    private val PASSWORD = "verySafePassword".toCharArray()
    private val CLIENT_ID = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val CHALLENGE_TYPE = "oob password redirect"
    private val CONTINUATION_TOKEN = "12345"

    private val OOB = "1234"

    private val mockConfig = mock<NativeAuthOAuth2Configuration>()
    private val mockStrategyParams = mock<OAuth2StrategyParameters>()

    private lateinit var nativeAuthOAuth2Strategy: NativeAuthOAuth2Strategy

    @Before
    fun setup() {
        whenever(mockConfig.clientId).thenReturn(CLIENT_ID)
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
        whenever(mockConfig.challengeType).thenReturn(CHALLENGE_TYPE)

        nativeAuthOAuth2Strategy = NativeAuthOAuth2Strategy(
            config = mockConfig,
            strategyParameters = mockStrategyParams,
            signUpInteractor = SignUpInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            signInInteractor = SignInInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            resetPasswordInteractor = ResetPasswordInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    @Test
    fun testPerformSignInInitiateSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInInitiate,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INITIATE_SUCCESS
        )

        val parameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .build()

        val signInInitiateResult = nativeAuthOAuth2Strategy.performSignInInitiate(
            parameters
        )

        Assert.assertTrue(signInInitiateResult is SignInInitiateApiResult.Success)
    }

    @Test
    fun testPerformSignInChallengeSuccess() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )

        Assert.assertTrue(signInChallengeResult is SignInChallengeApiResult.Redirect)
    }

    @Test
    fun testPerformSignInTokenWithPasswordSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInInitiate,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INITIATE_SUCCESS
        )

        val parameters = SignInStartUsingPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .password(PASSWORD)
            .build()

        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )

        Assert.assertTrue(signInChallengeResult is SignInInitiateApiResult.Success)
    }

    @Test
    fun testPerformSignInTokenWithOobSuccess() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = mockk<SignInSubmitCodeCommandParameters>()
        every { parameters.getCode() } returns OOB
        every { parameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { parameters.correlationId } returns correlationId

        val signInChallengeResult = nativeAuthOAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )

        Assert.assertTrue(signInChallengeResult is SignInTokenApiResult.Success)
        Assert.assertTrue((signInChallengeResult as SignInTokenApiResult.Success).tokenResponse.accessToken.isNotEmpty())
    }

    @Test
    fun testPerformOobTokenWithInvalidOob() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = mockk<SignInSubmitCodeCommandParameters>()
        every { parameters.getCode() } returns OOB
        every { parameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { parameters.correlationId } returns correlationId

        val signInChallengeResult = nativeAuthOAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )

        Assert.assertTrue(signInChallengeResult is SignInTokenApiResult.CodeIncorrect)
    }

    @Test
    fun testPerformSignInInitiateWithChallengeTypeRedirectSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInInitiate,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .build()

        val signInInitiateResult = nativeAuthOAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
        Assert.assertTrue(signInInitiateResult is SignInInitiateApiResult.Redirect)
    }

    @Test
    fun testPerformSignInChallengeWithChallengeTypeOobSuccess() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )

        Assert.assertTrue(signInChallengeResult is SignInChallengeApiResult.OOBRequired)
    }

    @Test
    fun testPerformSignInChallengeWithChallengeTypePasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )
        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )

        Assert.assertTrue(signInChallengeResult is SignInChallengeApiResult.PasswordRequired)
    }

    @Test
    fun testPerformSignInChallengeWithRedirectSuccess() {
        val correlationId = UUID.randomUUID().toString()

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )
        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )
        Assert.assertTrue(signInChallengeResult is SignInChallengeApiResult.Redirect)
    }

    @Test
    fun testPerformTokenWithInvalidGrantError() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_GRANT
        )

        val parameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .build()

        val result = nativeAuthOAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.UnknownError)
    }

    @Test
    fun testPerformPasswordTokenRequestSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .build()

        val result = nativeAuthOAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.Success)
    }

    @Test
    fun testPerformPasswordTokenRequestIncorrectPassword() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SIGNIN_INVALID_PASSWORD
        )

        val parameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .build()

        val result = nativeAuthOAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.InvalidCredentials)
    }

    @Test
    fun testPerformPasswordTokenRequestUserNotFound() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .build()

        val result = nativeAuthOAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.UserNotFound)
    }

    @Test
    fun testPerformContinuationTokenTokenRequest() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(CONTINUATION_TOKEN)
            .username(USERNAME)
            .build()

        val result = nativeAuthOAuth2Strategy.performContinuationTokenTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.Success)
    }

    @Test
    fun testSignInStartWithPasswordMFARequired() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.MFA_REQUIRED
        )

        val parameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(CONTINUATION_TOKEN)
            .username(USERNAME)
            .build()

        val result = nativeAuthOAuth2Strategy.performContinuationTokenTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.MFARequired)
    }

    @Test
    fun testPerformContinuationTokenTokenRequestUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(CONTINUATION_TOKEN)
            .username(USERNAME)
            .build()

        val result = nativeAuthOAuth2Strategy.performContinuationTokenTokenRequest(
            parameters = parameters
        )
        Assert.assertTrue(result is SignInTokenApiResult.UserNotFound)
    }
}
