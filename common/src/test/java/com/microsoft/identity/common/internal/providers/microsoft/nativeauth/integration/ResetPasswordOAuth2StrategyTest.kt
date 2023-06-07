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
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.logging.DiagnosticContext
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.UUID

/**
 * These are integration tests using real API responses instead of mocked API responses. This class
 * covers all sign up endpoints.
 * These tests run on the mock API, see: https://native-ux-mock-api.azurewebsites.net/
 */

@RunWith(
    PowerMockRunner::class
)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest(DiagnosticContext::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ResetPasswordOAuth2StrategyTest {
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
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(signUpContinueRequestUrl)
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(signInTokenRequestUrl)
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
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.Success)
    }

    @Test
    fun testPerformResetPasswordStartVerificationInvalidClientError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_CLIENT
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertFalse(ssprStartResult is ResetPasswordStartApiResult.Success)
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UnknownError)
        assertEquals((ssprStartResult as ResetPasswordStartApiResult.UnknownError).errorCode, "invalid_client")
    }

    @Test
    fun testPerformResetPasswordStartUnsupportedChallengeTypeRequestError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertFalse(ssprStartResult is ResetPasswordStartApiResult.Success)
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UnknownError)
        assertEquals((ssprStartResult as ResetPasswordStartApiResult.UnknownError).errorCode, "unsupported_challenge_type")
    }

    @Test
    fun testPerformResetPasswordStartChallengeTypeRedirectError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username

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
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPLICITLY_USER_NOT_FOUND
        )

        val mockResetPasswordStartCommandParameters = mockk<ResetPasswordStartCommandParameters>()
        every { mockResetPasswordStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performResetPasswordStart(
            mockResetPasswordStartCommandParameters
        )
        assertFalse(ssprStartResult is ResetPasswordStartApiResult.Success)
        assertTrue(ssprStartResult is ResetPasswordStartApiResult.UserNotFound)
    }

    /**
     * ResetPassword Challenge Tests
     */
    @Test
    fun testPerformResetPasswordChallengeSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = "1234"
        )
        assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
    }

    @Test
    fun testPerformResetPasswordChallengeExpiredTokenError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPIRED_TOKEN
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = "1234"
        )

        assertFalse(ssprChallengeResult is ResetPasswordChallengeApiResult.CodeRequired)
        assertTrue(ssprChallengeResult is ResetPasswordChallengeApiResult.UnknownError)
        assertEquals((ssprChallengeResult as ResetPasswordChallengeApiResult.UnknownError).errorCode, "expired_token")
    }

    @Test
    fun testPerformResetPasswordContinueSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )

        val mockResetPasswordSubmitCodeCommandParameters = mockk<ResetPasswordSubmitCodeCommandParameters>()
        every { mockResetPasswordSubmitCodeCommandParameters.getPasswordResetToken() } returns "1234"
        every { mockResetPasswordSubmitCodeCommandParameters.getCode() } returns oobCode

        val ssprContinueApiResult = nativeAuthOAuth2Strategy.performResetPasswordContinue(
            mockResetPasswordSubmitCodeCommandParameters
        )

        assertTrue(ssprContinueApiResult is ResetPasswordContinueApiResult.PasswordRequired)
    }

    @Test
    fun testPerformResetPasswordSubmitSuccess() {
        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooWeakError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode, "password_too_weak")
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooShortError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_SHORT
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode, "password_too_short")
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordTooLongError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_LONG
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode, "password_too_long")
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordRecentlyUsedError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_RECENTLY_USED
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode, "password_recently_used")
    }

    @Test
    fun testPerformResetPasswordSubmitPasswordBannedError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_BANNED
        )

        val mockResetPasswordSubmitCommandParameters = mockk<ResetPasswordSubmitNewPasswordCommandParameters>()
        every { mockResetPasswordSubmitCommandParameters.getPasswordSubmitToken() } returns "1234"
        every { mockResetPasswordSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performResetPasswordSubmit(
            mockResetPasswordSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertTrue(ssprSubmitResult is ResetPasswordSubmitApiResult.PasswordInvalid)
        assertEquals((ssprSubmitResult as ResetPasswordSubmitApiResult.PasswordInvalid).errorCode, "password_banned")
    }

    @Test
    fun testPerformResetPasswordPollCompletionSuccess() {
        val ssprPollCompletionResult = nativeAuthOAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = "1234"
        )
        assertTrue(ssprPollCompletionResult is ResetPasswordPollCompletionApiResult.PollingSucceeded)
    }
}
