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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import com.microsoft.identity.common.nativeauth.MockApiUtils.Companion.configureMockApi
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
import java.util.UUID

/**
 * These are integration tests using real API responses instead of mocked API responses. This class
 * covers all sign up endpoints.
 * These tests run on the mock API, see: $(MOCK_API_URL) in the variable of the pipeline.
 */

@RunWith(
    RobolectricTestRunner::class
)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest(DiagnosticContext::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SignUpOAuth2StrategyTest {
    private val USERNAME = "user@email.com"
    private val INVALID_USERNAME = "invalidUsername"
    private val INVALID_CLIENT_ID = "d7ce036a-8cc5-4734-b475-5ae4a0d5ab" // missing digits
    private val PASSWORD = "verySafePassword".toCharArray()
    private val CLIENT_ID = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val CHALLENGE_TYPE = "oob password redirect"
    private val USER_ATTRIBUTES = mapOf("city" to "Dublin")
    private val OOB_CODE = "123456"
    private val CONTINUATION_TOKEN = "iFQ"

    private val mockConfig = mock<NativeAuthOAuth2Configuration>()
    private val mockStrategyParams = mock<OAuth2StrategyParameters>()

    private lateinit var nativeAuthOAuth2Strategy: NativeAuthOAuth2Strategy

    @Before
    fun setup() {
        whenever(mockConfig.clientId).thenReturn(CLIENT_ID)
        whenever(mockConfig.tokenEndpoint).thenReturn(ApiConstants.MockApi.tokenEndpoint)
        whenever(mockConfig.getSignUpStartEndpoint()).thenReturn(ApiConstants.MockApi.signUpStartRequestUrl)
        whenever(mockConfig.getSignUpChallengeEndpoint()).thenReturn(ApiConstants.MockApi.signUpChallengeRequestUrl)
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(ApiConstants.MockApi.signUpContinueRequestUrl)
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(ApiConstants.MockApi.signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(ApiConstants.MockApi.signInChallengeRequestUrl)
        whenever(mockConfig.getSignInIntrospectEndpoint()).thenReturn(ApiConstants.MockApi.signInIntrospectRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(ApiConstants.MockApi.signInTokenRequestUrl)
        whenever(mockConfig.getResetPasswordStartEndpoint()).thenReturn(ApiConstants.MockApi.ssprStartRequestUrl)
        whenever(mockConfig.getResetPasswordChallengeEndpoint()).thenReturn(ApiConstants.MockApi.ssprChallengeRequestUrl)
        whenever(mockConfig.getResetPasswordContinueEndpoint()).thenReturn(ApiConstants.MockApi.ssprContinueRequestUrl)
        whenever(mockConfig.getResetPasswordSubmitEndpoint()).thenReturn(ApiConstants.MockApi.ssprSubmitRequestUrl)
        whenever(mockConfig.getResetPasswordPollCompletionEndpoint()).thenReturn(ApiConstants.MockApi.ssprPollCompletionRequestUrl)
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
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    @Test
    fun testPerformSignUpStartSuccessWithSuccess() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.Success)
    }

    @Test
    fun testPerformSignUpStartSuccessWithRedirect() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.Redirect)
    }

    @Test
    fun testPerformSignUpStartWithInvalidPassword() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_LONG
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testPerformSignUpStartWithInvalidPEmail() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_USERNAME
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(INVALID_USERNAME)
            .clientId(CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.InvalidUsername)
    }

    @Test
    fun testPerformSignUpStartWithInvalidClientId() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.UNAUTHORIZED_CLIENT
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(INVALID_CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.UnknownError)
    }

    @Test
    fun testPerformSignUpStartWithUnsupportedChallengeType() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )

        val signUpStartCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(CLIENT_ID)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpStartCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.UnsupportedChallengeType)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitPassword(
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitCodeSuccess() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitCodeCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(OOB_CODE)
            .continuationToken(CONTINUATION_TOKEN)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitCode(
            signUpSubmitCodeCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitUserAttributesSuccess() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val signUpSubmitUserAttributesCommandParameters =
            SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(mock<PlatformComponents>())
                .userAttributes(USER_ATTRIBUTES)
                .continuationToken(CONTINUATION_TOKEN)
                .correlationId(correlationId)
                .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitUserAttributes(
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.Success)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordAttributesRequired() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitPassword(
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.AttributesRequired)
    }

    @Test
    fun testPerformSignUpChallengeSuccessOOBRequired() {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )
        assertTrue(signupResult is SignUpChallengeApiResult.OOBRequired)
    }

    @Test
    fun testPerformSignUpChallengeSuccessPasswordRequired() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val signupResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            continuationToken = CONTINUATION_TOKEN,
            correlationId = correlationId
        )
        assertTrue(signupResult is SignUpChallengeApiResult.PasswordRequired)
    }

    @Test
    fun testPerformSignUpChallengeWithInvalidOOB() {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val signUpSubmitCodeCommandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(OOB_CODE)
            .continuationToken(CONTINUATION_TOKEN)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitCode(
            signUpSubmitCodeCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidOOBValue)
    }

    @Test
    fun testPerformSignUpWithSubmitPasswordInvalidPassword() {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val signUpSubmitPasswordCommandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(PASSWORD)
            .continuationToken(CONTINUATION_TOKEN)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitPassword(
            signUpSubmitPasswordCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testPerformSignUpWithSubmitAttributesWithInvalidAttributes() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val signUpSubmitUserAttributesCommandParameters =
            SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(mock<PlatformComponents>())
                .userAttributes(USER_ATTRIBUTES)
                .continuationToken(CONTINUATION_TOKEN)
                .correlationId(correlationId)
                .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpSubmitUserAttributes(
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpContinueApiResult.InvalidAttributes)
    }

    @Test
    fun testPerformSignUpStartWithAttributesWithInvalidAttributes() {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val signUpSubmitUserAttributesCommandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .clientId(CLIENT_ID)
            .userAttributes(USER_ATTRIBUTES)
            .correlationId(correlationId)
            .build()

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            signUpSubmitUserAttributesCommandParameters
        )
        assertTrue(signupResult is SignUpStartApiResult.InvalidAttributes)
    }
}
