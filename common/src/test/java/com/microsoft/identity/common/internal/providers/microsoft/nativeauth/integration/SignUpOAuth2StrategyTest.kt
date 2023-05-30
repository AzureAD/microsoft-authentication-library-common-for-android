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
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.logging.DiagnosticContext
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
import junit.framework.TestCase.assertTrue
import org.junit.Before
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
 * These tests run on the mock API, see: https://native-ux-mock-api.azurewebsites.net/
 */

@RunWith(
    RobolectricTestRunner::class
)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest(DiagnosticContext::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SignUpOAuth2StrategyTest {
    private val username = "user@email.com"
    private val password = "verySafePassword"
    private val tenant = "samtoso.onmicrosoft.com"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val signUpStartRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val signUpContinueRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/continue")
    private val signInInitiateRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/initiate")
    private val signInChallengeRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/challenge")
    private val signInTokenRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/token")
    private val ssprStartRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val ssprChallengeRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val ssprContinueRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val ssprSubmitRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val ssprPollCompletionRequestUrl =
        URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeTypes = "oob password redirect"
    private val userAttributes = mapOf("city" to "Dublin")
    private val oobCode = "123456"
    private val signUpToken = "iFQ"

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
        whenever(mockConfig.challengeType).thenReturn(challengeTypes)

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
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    @Test
    fun testPerformSignUpStartSuccessWithVerificationRequired() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.VerificationRequired)
    }

    @Test
    fun testPerformSignUpStartSuccessWithRedirect() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.Redirect)
    }

    @Test
    fun testPerformSignUpStartWithInvalidPassword() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_LONG
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testPerformSignUpStartSuccessWithUnsupportedChallengeType() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.UnknownError)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .signupToken(signUpToken)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitCodeSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitCodeCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .signupToken(signUpToken)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitCodeCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitUserAttributesSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitUserAttributesCommandParameters =
            SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(mock<PlatformComponents>())
                .userAttributes(userAttributes)
                .signupToken(signUpToken)
                .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordAttributesRequired() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .signupToken(signUpToken)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.AttributesRequired)
    }

    @Test
    fun testPerformSignUpChallengeSuccessOOBRequired() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken,
        )
        assertTrue(signupResult is SignUpChallengeApiResult.OOBRequired)
    }

    @Test
    fun testPerformSignUpChallengeSuccessPasswordRequired() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val signupResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken,
        )
        assertTrue(signupResult is SignUpChallengeApiResult.PasswordRequired)
    }

    @Test
    fun testPerformSignUpChallengeWithInvalidOOB() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPLICIT_INVALID_OOB_VALUE
        )

        val signUpSubmitCodeCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .signupToken(signUpToken)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitCodeCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidOOBValue)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordInvalidPassword() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .signupToken(signUpToken)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testPerformSignUpWithSubmitAttributesWithInvalidAttributes() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val signUpSubmitUserAttributesCommandParameters =
            SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(mock<PlatformComponents>())
                .userAttributes(userAttributes)
                .signupToken(signUpToken)
                .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidAttributes)
    }

    @Test
    fun testPerformSignUpStartWithAttributesWithInvalidAttributes() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val signUpSubmitUserAttributesCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .userAttributes(userAttributes)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.InvalidAttributes)
    }
}
