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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.logging.DiagnosticContext
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
import com.microsoft.identity.common.nativeauth.ApiConstants
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import com.microsoft.identity.common.nativeauth.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signInChallengeRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signInInitiateRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signInTokenRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signUpChallengeRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signUpContinueRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.signUpStartRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.ssprChallengeRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.ssprContinueRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.ssprStartRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.ssprSubmitRequestUrl
import com.microsoft.identity.common.nativeauth.ApiConstants.Companion.ssprPollCompletionRequestUrl

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class ResetPasswordOAuth2StrategyTest {
    private val USERNAME = "user@email.com"
    private val PASSWORD = "verySafePassword".toCharArray()
    private val TENANT = "samtoso.onmicrosoft.com"
    private val CLIENT_ID = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val CHALLENGE_TYPE = "oob redirect"
    private val OOB_CODE = "123456"
    private val CONTINUATION_TOKEN = "1234"
    private val INVALID_GRANT_ERROR = "invalid_grant"
    private val INVALID_CLIENT_ERROR = "invalid_client"
    private val UNSUPPORTED_CHALLENGE_TYPE_ERROR = "unsupported_challenge_type"
    private val EXPIRED_TOKEN_ERROR = "expired_token"
    private val PASSWORD_TOO_LONG_ERROR = "password_too_long"
    private val PASSWORD_TOO_SHORT_ERROR = "password_too_short"
    private val PASSWORD_TOO_WEAK_ERROR = "password_too_weak"
    private val PASSWORD_RECENTLY_USED_ERROR = "password_recently_used"
    private val PASSWORD_BANNED_ERROR = "password_banned"

    private val mockConfig = mock<NativeAuthOAuth2Configuration>()
    private val mockStrategyParams = mock<OAuth2StrategyParameters>()

    private lateinit var nativeAuthOAuth2Strategy: NativeAuthOAuth2Strategy

    @Before
    fun setup() {
        whenever(mockConfig.clientId).thenReturn(CLIENT_ID)
        whenever(mockConfig.tokenEndpoint).thenReturn(ApiConstants.tokenEndpoint)
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
        whenever(mockConfig.challengeType).thenReturn(CHALLENGE_TYPE)

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

    /**
     * Reset password Start Tests
     */
    @Test
    fun testPerformResetPasswordStartSuccessWithUsername() {
        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns USERNAME

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
    }

    @Test
    fun testPerformResetPasswordStartVerificationInvalidClientError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_CLIENT
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns USERNAME

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UnknownError)
        assertEquals((ssprStartResult as ResetPasswordStartApiResult.UnknownError).error, INVALID_CLIENT_ERROR)
    }

    @Test
    fun testPerformResetPasswordStartUnsupportedChallengeTypeRequestError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns USERNAME

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UnsupportedChallengeType)
        assertEquals((ssprStartResult as ResetPasswordStartApiResult.UnsupportedChallengeType).error, UNSUPPORTED_CHALLENGE_TYPE_ERROR)
    }

    @Test
    fun testPerformResetPasswordStartChallengeTypeRedirectError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns USERNAME

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.Redirect)
    }

    /**
     * expected:<[invalid_grant]> but was:<[user_not_found]>
     * Expected :[invalid_grant]
     * Actual   :[user_not_found]
     */
    @Test
    fun testPerformResetPasswordStartUserNotFoundError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns USERNAME

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UserNotFound)
    }

    /**
     * ResetPassword Challenge Tests
     */
    @Test
    fun testPerformResetPasswordChallengeSuccess() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            continuationToken = CONTINUATION_TOKEN
        )
        assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
    }

    @Test
    fun testPerformResetPasswordChallengeExpiredTokenError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPIRED_TOKEN
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            continuationToken = CONTINUATION_TOKEN
        )

        assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.ExpiredToken)
        assertEquals((ssprChallengeResult as ResetPasswordChallengeApiResult.ExpiredToken).error, EXPIRED_TOKEN_ERROR)
    }

    @Test
    fun testPerformResetPasswordContinueSuccess() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )

        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns OOB_CODE

        val ssprContinueApiResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )

        assertTrue(ssprContinueApiResult is ResetPasswordContinueApiResult.PasswordRequired)
    }

    @Test
    fun testPerformResetPasswordSubmitSuccess() {
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooWeakError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error, INVALID_GRANT_ERROR)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError, PASSWORD_TOO_WEAK_ERROR)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooShortError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_SHORT
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error, INVALID_GRANT_ERROR)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError, PASSWORD_TOO_SHORT_ERROR)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooLongError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_LONG
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error, INVALID_GRANT_ERROR)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError,PASSWORD_TOO_LONG_ERROR)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordRecentlyUsedError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_RECENTLY_USED
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error, INVALID_GRANT_ERROR)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError, PASSWORD_RECENTLY_USED_ERROR)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordBannedError() {
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_BANNED
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getContinuationToken() } returns CONTINUATION_TOKEN
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns PASSWORD

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).error, INVALID_GRANT_ERROR)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).subError, PASSWORD_BANNED_ERROR)
    }

    @Test
    fun testPerformResetPasswordPollCompletionSuccess() {
        val ssprPollCompletionResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            continuationToken = CONTINUATION_TOKEN
        )
        assertTrue(ssprPollCompletionResult is ResetPasswordPollCompletionApiResult.PollingSucceeded)
    }
}
