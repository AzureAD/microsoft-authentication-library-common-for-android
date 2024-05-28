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
package com.microsoft.identity.common.nativeauth.internal.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.internal.util.capture
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Tests for [NativeAuthMsalController].
 */
@RunWith(RobolectricTestRunner::class)
class NativeAuthControllerTest {
    private val code = "12345"
    private val username = "user@email.com"
    private val password = "verySafePassword".toCharArray()
    private val defaultScopes: List<String> = AuthenticationConstants.DEFAULT_SCOPES.toList()
    private val scopes: List<String> = listOf("scope1", "scope2", "scope3")
    private val invalidGrantError = "invalid_grant"
    private val credentialRequiredError = "credential_required"
    private val userNotFoundError = "user_not_found"
    private val continuationToken = "1234"
    private val newPassword = "newPassword".toCharArray()
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val authorityUrl = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"
    private val userAttributes = mapOf("city" to "dublin")

    private lateinit var platformComponents: IPlatformComponents
    private lateinit var context: Context

    private val controller = spy(NativeAuthMsalController())
    private val localController = LocalMSALController()

    @Captor
    lateinit var initiateApiResultCaptor: ArgumentCaptor<SignInInitiateApiResult>
    @Captor
    lateinit var signInStartCommandParametersWithScopesCaptor: ArgumentCaptor<SignInStartCommandParameters>
    @Captor
    lateinit var signInSubmitPasswordCommandParametersWithScopesCaptor: ArgumentCaptor<SignInSubmitPasswordCommandParameters>
    @Captor
    lateinit var signInWithContinuationTokenCommandParametersWithScopesCaptor: ArgumentCaptor<SignInWithContinuationTokenCommandParameters>
    @Captor
    lateinit var signUpStartCommandParametersCaptor: ArgumentCaptor<SignUpStartCommandParameters>
    @Captor
    lateinit var signUpSubmitUserAttributesCommandParametersCaptor: ArgumentCaptor<SignUpSubmitUserAttributesCommandParameters>
    @Captor
    lateinit var oAuth2StrategyCaptor: ArgumentCaptor<NativeAuthOAuth2Strategy>
    @Captor
    lateinit var usePasswordCaptor: ArgumentCaptor<Boolean>

    @get:Rule
    var rule: MockitoRule = MockitoJUnit.rule()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            BuildValues.setUseMockApiForNativeAuth(true)
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            BuildValues.setUseMockApiForNativeAuth(false)
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        platformComponents = AndroidPlatformComponentsFactory.createFromContext(
            context
        )
    }

    // region Sign In
    @Test
    fun testSignInStartWithPasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.Complete)
    }

    @Test
    fun testSignInStartWithPasswordAssertScopes() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)

        verify(controller).processSignInInitiateApiResult(
            capture(initiateApiResultCaptor),
            capture(signInStartCommandParametersWithScopesCaptor),
            capture(oAuth2StrategyCaptor),
            capture(usePasswordCaptor)
        )

        val scopesToCheck = scopes + defaultScopes
        assertTrue(usePasswordCaptor.value)
        assertEquals(scopesToCheck, signInStartCommandParametersWithScopesCaptor.value?.scopes)
        assert(result is SignInCommandResult.Complete)
    }

    @Test
    fun testSignInStartWithPasswordPasswordIncorrect() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNIN_INVALID_PASSWORD
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.InvalidCredentials)
    }

    @Test
    fun testSignInStartWithPasswordUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.UserNotFound)
    }

    @Test
    fun testSignInStartWithPasswordCodeRequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.CodeRequired)
    }

    @Test
    fun testSignInStartWithPasswordMFARequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.MFA_REQUIRED
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignInStartWithEmailUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSignInStartCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.UserNotFound)
    }

    @Test
    fun testSignInStartWithEmailCodeRequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInStartCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.CodeRequired)
    }

    @Test
    fun testSignInSubmitCodeWithSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInSubmitCodeCommandParameters(correlationId)
        val result = controller.signInSubmitCode(parameters)
        assert(result is SignInCommandResult.Complete)
    }

    @Test
    fun testSignInSubmitCodeWithInvalidCode() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSignInSubmitCodeCommandParameters(correlationId)
        val result = controller.signInSubmitCode(parameters)
        assert(result is SignInCommandResult.IncorrectCode)
    }

    @Test
    fun testSignInResendCodeSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInResendCodeCommandParameters(correlationId)
        val result = controller.signInResendCode(parameters)
        assert(result is SignInCommandResult.CodeRequired)
    }

    fun testSignInSubmitPasswordWithPasswordInvalid() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNIN_INVALID_PASSWORD
        )

        val parameters = createSignInSubmitPasswordCommandParameters(correlationId)
        val result = controller.signInSubmitPassword(parameters)
        assert(result is SignInCommandResult.InvalidCredentials)

        // Verify whether scopes include default scopes
        verify(controller).processSignInInitiateApiResult(
            capture(initiateApiResultCaptor),
            capture(signInStartCommandParametersWithScopesCaptor),
            capture(oAuth2StrategyCaptor),
            capture(usePasswordCaptor)
        )

        val scopesToCheck = scopes + defaultScopes
        assertTrue(usePasswordCaptor.value)
        assertEquals(scopesToCheck, signInStartCommandParametersWithScopesCaptor.value?.scopes)
    }

    @Test
    fun testSignInSubmitPasswordWithSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInSubmitPasswordCommandParameters(correlationId)
        val result = controller.signInSubmitPassword(parameters)
        assert(result is SignInCommandResult.Complete)

        // Verify scopes
        verify(controller).performPasswordTokenCall(
            capture(oAuth2StrategyCaptor),
            capture(signInSubmitPasswordCommandParametersWithScopesCaptor)
        )

        val scopesToCheck = scopes + defaultScopes
        assertEquals(scopesToCheck, signInSubmitPasswordCommandParametersWithScopesCaptor.value?.scopes)
    }

    @Test
    fun testSignInStartWithEmailPasswordRequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INITIATE_SUCCESS
        )
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val parameters = createSignInStartCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is SignInCommandResult.PasswordRequired)
    }

    @Test
    fun testSignInWithContinuationTokenSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInWithContinuationTokenCommandParameters(
            withScopes = true,
            correlationId = correlationId
        )
        val result = controller.signInWithContinuationToken(parameters)
        assert(result is SignInCommandResult.Complete)

        // Verify scopes
        verify(controller).performContinuationTokenTokenRequest(
            capture(oAuth2StrategyCaptor),
            capture(signInWithContinuationTokenCommandParametersWithScopesCaptor),
        )

        val scopesToCheck = scopes + defaultScopes
        assertEquals(scopesToCheck, signInWithContinuationTokenCommandParametersWithScopesCaptor.value?.scopes)
    }

    @Test
    fun testSignInWithContinuationTokenInvalidGrant() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INVALID_GRANT
        )

        val parameters = createSignInWithContinuationTokenCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signInWithContinuationToken(parameters)
        assert(result is INativeAuthCommandResult.UnknownError)
        assert((result as INativeAuthCommandResult.UnknownError).error == invalidGrantError)
    }

    @Test
    fun testSignInWithContinuationTokenCodeIncorrect() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSignInWithContinuationTokenCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signInWithContinuationToken(parameters)
        assert(result is INativeAuthCommandResult.UnknownError)
        assert((result as INativeAuthCommandResult.UnknownError).error == invalidGrantError)
    }

    @Test
    fun testSignInWithContinuationTokenUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSignInWithContinuationTokenCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signInWithContinuationToken(parameters)
        assert(result is INativeAuthCommandResult.UnknownError)
        assert((result as INativeAuthCommandResult.UnknownError).error == userNotFoundError)
    }

    @Test
    fun testSignInWithContinuationTokenPasswordIncorrect() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNIN_INVALID_PASSWORD
        )

        val parameters = createSignInWithContinuationTokenCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signInWithContinuationToken(parameters)
        assert(result is INativeAuthCommandResult.UnknownError)
        assert((result as INativeAuthCommandResult.UnknownError).error == invalidGrantError)
    }

    @Test
    fun testSignInStartWithPasswordBrowserRequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSignInStartWithPasswordCommandParameters(correlationId)
        val result = controller.signInStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }
    //endregion

    // region Sign out
    @Test
    fun testSignOutSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.TOKEN_SUCCESS
        )

        val signInParameters = createSignInSubmitPasswordCommandParameters(correlationId)
        val signInResult = controller.signInSubmitPassword(signInParameters) as SignInCommandResult.Complete

        val account = signInResult.authenticationResult.accountRecord as AccountRecord
        val parameters = createRemoveAccountCommandParameters(
            account = account,
            correlationId = correlationId
        )
        val result = localController.removeCurrentAccount(parameters)
        assert(result)
    }

    @Test
    fun testSignOutFailedWithEmptyAccountRecord() {
        val correlationId = UUID.randomUUID().toString()
        val account = AccountRecord()
        val parameters = createRemoveAccountCommandParameters(
            account = account,
            correlationId = correlationId
        )
        val result = localController.removeCurrentAccount(parameters)
        assertFalse(result)
    }
    // endregion

    // region Sspr
    @Test
    fun testSsprStartSsprEmailVerificationRequired() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_START_SUCCESS
        )

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSsprStartCommandParameters(correlationId)
        val result = controller.resetPasswordStart(parameters)
        assert(result is ResetPasswordCommandResult.CodeRequired)
    }

    @Test
    fun testSsprStartSsprRedirect() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSsprStartCommandParameters(correlationId)
        val result = controller.resetPasswordStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSsprStartUserNotFound() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSsprStartCommandParameters(correlationId)
        val result = controller.resetPasswordStart(parameters)
        assert(result is ResetPasswordCommandResult.UserNotFound)
    }

    @Test
    fun testSsprSubmitCodeWithSuccess() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )

        val parameters = createSsprSubmitCodeCommandParameters(correlationId)
        val result = controller.resetPasswordSubmitCode(parameters)
        assert(result is ResetPasswordCommandResult.PasswordRequired)
    }

    @Test
    fun testSsprSubmitCodeWithInvalidCode() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSsprSubmitCodeCommandParameters(correlationId)
        val result = controller.resetPasswordSubmitCode(parameters)
        assert(result is ResetPasswordCommandResult.IncorrectCode)
    }

    @Test
    fun testSsprResendCode() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSsprResendCodeCommandParameters(correlationId)
        val result = controller.resetPasswordResendCode(parameters)
        assert(result is ResetPasswordCommandResult.CodeRequired)
    }

    @Test
    fun testSsprSubmitNewPasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_POLL_SUCCESS
        )

        val parameters = createSsprSubmitNewPasswordCommandParameters(correlationId)
        val result = controller.resetPasswordSubmitNewPassword(parameters)
        assert(result is ResetPasswordCommandResult.Complete)
    }

    @Test
    fun testSsprSubmitNewPasswordFailed() {
        val correlationId = UUID.randomUUID().toString()
        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SSPR_POLL_FAILED
        )

        val parameters = createSsprSubmitNewPasswordCommandParameters(correlationId)
        val result = controller.resetPasswordSubmitNewPassword(parameters)
        assert(result is ResetPasswordCommandResult.PasswordResetFailed)
    }
    // endregion

    // region signup
    @Test
    fun testSignUpStartWithPasswordRequired() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNUP_START_SUCCESS
        )

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.PasswordRequired)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithInvalidPassword() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_TOO_LONG
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)
    }

    @Test
    fun testSignUpStartWithUsernameAlreadyExists() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.USER_ALREADY_EXISTS
        )

        val parameters = createSignUpStartCommandParameters(correlationId)
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.UsernameAlreadyExists)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordTooWeak() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            passwordValue = "Test@123".toCharArray(),
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordTooShort() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_TOO_SHORT
        )

        val parameters =
            createSignUpStartWithPasswordCommandParameters(
                passwordValue = "123".toCharArray(),
                correlationId = correlationId
            )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordBanned() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_BANNED
        )

        val parameters =
            createSignUpStartWithPasswordCommandParameters(
                passwordValue = "Abc@123".toCharArray(),
                correlationId = correlationId
            )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordTooLong() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_TOO_LONG
        )

        val parameters =
            createSignUpStartWithPasswordCommandParameters(
                passwordValue = "079af063-4ea7-4dcd-91ff-2b24f54621ea-079af063-4ea7-4dcd-91ff-2b24f54621ea-079af063-4ea7-4dcd-91ff-2b24f54621ea".toCharArray(),
                correlationId = correlationId
            )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordRecentlyUsed() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_RECENTLY_USED
        )

        val parameters =
            createSignUpStartWithPasswordCommandParameters(
                passwordValue = password,
                correlationId = correlationId
            )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartWithPasswordAuthenticationNotSupported() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.AUTH_NOT_SUPPORTED
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.AuthNotSupported)

        // Verify attributes
        verify(controller).performSignUpStartUsingPasswordRequest(
            capture(oAuth2StrategyCaptor),
            capture(signUpStartCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpStartAuthenticationNotSupported() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.AUTH_NOT_SUPPORTED
        )

        val parameters = createSignUpStartCommandParameters(correlationId)
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.AuthNotSupported)
    }

    @Test
    fun testSignUpResendCodeSuccess() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignUpResendCodeCommandParameters(correlationId)
        val result = controller.signUpResendCode(parameters)
        assert(result is SignUpCommandResult.CodeRequired)
    }

    @Test
    fun testSignUpSubmitCodeSuccess() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val parameters = createSignUpSubmitCodeCommandParameters(correlationId)
        val result = controller.signUpSubmitCode(parameters)
        assert(result is SignUpCommandResult.Complete)
    }

    @Test
    fun testSignUpSubmitPasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val parameters = createSignUpSubmitPasswordCommandParameters(correlationId)
        val result = controller.signUpSubmitPassword(parameters)
        assert(result is SignUpCommandResult.Complete)
    }

    @Test
    fun testSignUpSubmitPasswordInvalidPassword() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val parameters = createSignUpSubmitPasswordCommandParameters(correlationId)
        val result = controller.signUpSubmitPassword(parameters)
        assert(result is SignUpCommandResult.InvalidPassword)
    }

    @Test
    fun testSignUpSubmitUserAttributeSuccess() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val parameters = createSignUpSubmitUserAttributesCommandParameters(correlationId)
        val result = controller.signUpSubmitUserAttributes(parameters)
        assert(result is SignUpCommandResult.Complete)

        // Verify attributes
        verify(controller).performSignUpSubmitUserAttributes(
            capture(oAuth2StrategyCaptor),
            capture(signUpSubmitUserAttributesCommandParametersCaptor),
        )

        assertEquals(userAttributes, userAttributes)
    }

    @Test
    fun testSignUpContinueAttributesRequired() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val parameters = createSignUpSubmitPasswordCommandParameters(correlationId)
        val result = controller.signUpSubmitPassword(parameters)
        assert(result is SignUpCommandResult.AttributesRequired)
    }

    @Test
    fun testSignUpStartRedirect() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSignUpStartCommandParameters(correlationId)
        val result = controller.signUpStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignUpStartWithPasswordRedirect() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignUpChallengeRedirect() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.SIGNUP_START_SUCCESS
        )

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSignUpStartWithPasswordCommandParameters(
            correlationId = correlationId
        )
        val result = controller.signUpStart(parameters)
        assert(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignUpAdditionalAttributesRequired() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val parameters = createSignUpSubmitUserAttributesCommandParameters(correlationId)
        val result = controller.signUpSubmitUserAttributes(parameters)
        assert(result is SignUpCommandResult.AttributesRequired)
    }

    @Test
    fun testSignUpInvalidOOB() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSignUpSubmitCodeCommandParameters(correlationId)
        val result = controller.signUpSubmitCode(parameters)
        assert(result is SignUpCommandResult.InvalidCode)
    }

    @Test
    fun testSignUpSubmitUserAttributesWithInvalidUserAttributes() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val parameters = createSignUpSubmitUserAttributesCommandParameters(correlationId)
        val result = controller.signUpSubmitUserAttributes(parameters)
        assert(result is SignUpCommandResult.InvalidAttributes)
    }

    @Test
    fun testSignUpStartWithInvalidUserAttributes() {
        val correlationId = UUID.randomUUID().toString()

        com.microsoft.identity.internal.testutils.nativeauth.MockApiUtils.configureMockApi(
            endpointType = com.microsoft.identity.internal.testutils.nativeauth.MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = com.microsoft.identity.internal.testutils.nativeauth.MockApiResponseType.ATTRIBUTE_VALIDATION_FAILED
        )

        val parameters = createSignUpStartCommandParameters(correlationId)
        val result = controller.signUpStart(parameters)
        assert(result is SignUpCommandResult.InvalidAttributes)
    }
    // endregion

    private fun createSignInStartWithPasswordCommandParameters(
        correlationId: String
    ): SignInStartCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponentsFactory.createFromContext(context),
            null
        )

        return SignInStartCommandParameters.builder()
            .username(username)
            .password(password)
            .scopes(scopes)
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInStartCommandParameters(
        correlationId: String
    ): SignInStartCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponentsFactory.createFromContext(context),
            null
        )

        return SignInStartCommandParameters.builder()
            .username(username)
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInWithContinuationTokenCommandParameters(
        withScopes: Boolean = false,
        correlationId: String
    ): SignInWithContinuationTokenCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponentsFactory.createFromContext(context),
            null
        )

        return SignInWithContinuationTokenCommandParameters.builder()
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .scopes(if (withScopes) scopes else emptyList())
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .continuationToken(continuationToken)
            .username(username)
            .correlationId(correlationId)
            .build()
    }

    private fun createSignInSubmitCodeCommandParameters(correlationId: String): SignInSubmitCodeCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponentsFactory.createFromContext(context),
            null
        )

        return SignInSubmitCodeCommandParameters.builder()
            .code(code)
            .authenticationScheme(authenticationScheme)
            .continuationToken(continuationToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInResendCodeCommandParameters(correlationId: String): SignInResendCodeCommandParameters {
        return SignInResendCodeCommandParameters.builder()
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .continuationToken(continuationToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInSubmitPasswordCommandParameters(correlationId: String): SignInSubmitPasswordCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponentsFactory.createFromContext(context),
            null
        )

        return SignInSubmitPasswordCommandParameters.builder()
            .password(password)
            .scopes(scopes)
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .continuationToken(continuationToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprStartCommandParameters(correlationId: String): ResetPasswordStartCommandParameters {
        return ResetPasswordStartCommandParameters.builder()
            .username(username)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprSubmitCodeCommandParameters(correlationId: String): ResetPasswordSubmitCodeCommandParameters {
        return ResetPasswordSubmitCodeCommandParameters.builder()
            .code(code)
            .continuationToken(continuationToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprResendCodeCommandParameters(correlationId: String): ResetPasswordResendCodeCommandParameters {
        return ResetPasswordResendCodeCommandParameters.builder()
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .continuationToken(continuationToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprSubmitNewPasswordCommandParameters(correlationId: String): ResetPasswordSubmitNewPasswordCommandParameters {
        return ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .newPassword(newPassword)
            .continuationToken(continuationToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createRemoveAccountCommandParameters(
        account: AccountRecord,
        correlationId: String
    ): RemoveAccountCommandParameters {
        return RemoveAccountCommandParameters.builder()
            .account(account)
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .build()
    }

    private fun createCache(): MsalOAuth2TokenCache<*, *, *, *, *> {
        return MsalOAuth2TokenCache.create(platformComponents)
    }

    private fun createSignUpStartWithPasswordCommandParameters(
        passwordValue: CharArray? = null,
        correlationId: String
    ): SignUpStartCommandParameters {
        return SignUpStartCommandParameters.builder()
            .username(username)
            .password(
                if (passwordValue == null || passwordValue.isEmpty()) {
                    password
                } else {
                    passwordValue
                }
            )
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .userAttributes(userAttributes)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignUpStartCommandParameters(
        correlationId: String
    ): SignUpStartCommandParameters {
        return SignUpStartCommandParameters.builder()
            .username(username)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignUpResendCodeCommandParameters(
        correlationId: String
    ): SignUpResendCodeCommandParameters {
        return SignUpResendCodeCommandParameters.builder()
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .continuationToken(continuationToken)
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignUpSubmitCodeCommandParameters(
        correlationId: String
    ): SignUpSubmitCodeCommandParameters {
        return SignUpSubmitCodeCommandParameters.builder()
            .continuationToken(continuationToken)
            .code(code)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignUpSubmitPasswordCommandParameters(
        correlationId: String
    ): SignUpSubmitPasswordCommandParameters {
        return SignUpSubmitPasswordCommandParameters.builder()
            .continuationToken(continuationToken)
            .password(password)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignUpSubmitUserAttributesCommandParameters(
        correlationId: String
    ): SignUpSubmitUserAttributesCommandParameters {
        return SignUpSubmitUserAttributesCommandParameters.builder()
            .continuationToken(continuationToken)
            .userAttributes(userAttributes)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .correlationId(correlationId)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }
}
