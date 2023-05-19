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
package com.microsoft.identity.common.internal.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.AndroidPlatformComponents
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils
import com.microsoft.identity.common.java.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.CodeRequired
import com.microsoft.identity.common.java.controllers.results.Complete
import com.microsoft.identity.common.java.controllers.results.IncorrectCode
import com.microsoft.identity.common.java.controllers.results.PasswordIncorrect
import com.microsoft.identity.common.java.controllers.results.PasswordResetFailed
import com.microsoft.identity.common.java.controllers.results.Redirect
import com.microsoft.identity.common.java.controllers.results.SignInPasswordRequired
import com.microsoft.identity.common.java.controllers.results.SignInUserNotFound
import com.microsoft.identity.common.java.controllers.results.SsprCodeRequired
import com.microsoft.identity.common.java.controllers.results.SsprComplete
import com.microsoft.identity.common.java.controllers.results.SsprIncorrectCode
import com.microsoft.identity.common.java.controllers.results.SsprPasswordRequired
import com.microsoft.identity.common.java.controllers.results.SsprUserNotFound
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Tests for [com.microsoft.identity.common.internal.controllers.NativeAuthController].
 */
@RunWith(RobolectricTestRunner::class)
class NativeAuthControllerTest {
    private val code = "12345"
    private val credentialToken = "sk490fj8a83n*@f-1"
    private val username = "user@email.com"
    private val password = "verySafePassword"
    private val passwordResetToken = "sk490fj8a83n*@f-2"
    private val passwordSubmitToken = "sk490fj8a83n*@f-3"
    private val newPassword = "newPassword"
    private val clientId = "079af063-4ea7-4dcd-91ff-2b24f54621ea"
    private val authorityUrl = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"

    private lateinit var platformComponents: IPlatformComponents
    private lateinit var context: Context

    private val controller = NativeAuthController()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        platformComponents = AndroidPlatformComponentsFactory.createFromContext(
            context
        )
    }

    // region Sign In
    @Test
    fun testSignInStartWithRopcSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInStartWithPasswordCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is Complete)
    }

    @Test
    fun testSignInStartWithRopcCodeRequired() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.CREDENTIAL_REQUIRED
        )

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInStartWithPasswordCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is CodeRequired)
    }

    @Test
    fun testSignInStartWithRopcPasswordIncorrect() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_PASSWORD
        )

        val parameters = createSignInStartWithPasswordCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is PasswordIncorrect)
    }

    @Test
    fun testSignInStartWithRopcUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSignInStartWithPasswordCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is SignInUserNotFound)
    }

    @Test
    fun testSignInStartWithEmailUserNotFound() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInInitiate,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSignInStartCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is SignInUserNotFound)
    }

    @Test
    fun testSignInStartWithEmailCodeRequired() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInInitiate,
            correlationId = correlationId,
            responseType = MockApiResponseType.INITIATE_SUCCESS
        )
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInStartCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is CodeRequired)
    }

    @Test
    fun testSignInSubmitCodeWithSuccess() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInSubmitCodeCommandParameters()
        val result = controller.signInSubmitCode(parameters)
        assert(result is Complete)
    }

    @Test
    fun testSignInSubmitCodeWithInvalidCode() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSignInSubmitCodeCommandParameters()
        val result = controller.signInSubmitCode(parameters)
        assert(result is IncorrectCode)
    }

    @Test
    fun testSignInResendCodeSuccess() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSignInResendCodeCommandParameters()
        val result = controller.signInResendCode(parameters)
        assert(result is CodeRequired)
    }

    fun testSignInSubmitPasswordWithPasswordInvalid() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_PASSWORD
        )

        val parameters = createSignInSubmitPasswordCommandParameters()
        val result = controller.signInSubmitPassword(parameters)
        assert(result is PasswordIncorrect)
    }

    @Test
    fun testSignInSubmitPasswordWithSuccess() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val parameters = createSignInSubmitPasswordCommandParameters()
        val result = controller.signInSubmitPassword(parameters)
        assert(result is Complete)
    }

    @Test
    fun testSignInStartWithEmailPasswordRequired() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInInitiate,
            correlationId = correlationId,
            responseType = MockApiResponseType.INITIATE_SUCCESS
        )
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val parameters = createSignInStartCommandParameters()
        val result = controller.signInStart(parameters)
        assert(result is SignInPasswordRequired)
    }
    //endregion

    // region Sspr
    @Test
    fun testSsprStartSsprEmailVerificationRequired() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSsprStartCommandParameters()
        val result = controller.ssprStart(parameters)
        assert(result is SsprCodeRequired)
    }

    @Test
    fun testSsprStartSsprRedirect() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val parameters = createSsprStartCommandParameters()
        val result = controller.ssprStart(parameters)
        assert(result is Redirect)
    }

    @Test
    @Ignore("TODO remove ignore when sspr start user not found implemented in mock api")
    fun testSsprStartUserNotFound() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        val parameters = createSsprStartCommandParameters()
        val result = controller.ssprStart(parameters)
        assert(result is SsprUserNotFound)
    }

    @Test
    fun testSsprSubmitCodeWithSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )

        val parameters = createSsprSubmitCodeCommandParameters()
        val result = controller.ssprSubmitCode(parameters)
        assert(result is SsprPasswordRequired)
    }

    @Test
    fun testSsprSubmitCodeWithInvalidCode() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val parameters = createSsprSubmitCodeCommandParameters()
        val result = controller.ssprSubmitCode(parameters)
        assert(result is SsprIncorrectCode)
    }

    @Test
    fun testSsprResendCode() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val parameters = createSsprResendCodeCommandParameters()
        val result = controller.ssprResendCode(parameters)
        assert(result is SsprCodeRequired)
    }

    @Test
    fun testSsprSubmitNewPasswordSuccess() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )

        val parameters = createSsprSubmitNewPasswordCommandParameters()
        val result = controller.ssprSubmitNewPassword(parameters)
        assert(result is SsprComplete)
    }

    @Test
    fun testSsprSubmitNewPasswordFailed() {
        val correlationId = UUID.randomUUID().toString()
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )

        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_FAILED
        )

        val parameters = createSsprSubmitNewPasswordCommandParameters()
        val result = controller.ssprSubmitNewPassword(parameters)
        assert(result is PasswordResetFailed)
    }
    // endregion

    private fun createSignInStartWithPasswordCommandParameters(): SignInStartWithPasswordCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponents.createFromContext(context),
            null
        )

        return SignInStartWithPasswordCommandParameters.builder()
            .username(username)
            .password(password)
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInStartCommandParameters(): SignInStartCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponents.createFromContext(context),
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
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInSubmitCodeCommandParameters(): SignInSubmitCodeCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponents.createFromContext(context),
            null
        )

        return SignInSubmitCodeCommandParameters.builder()
            .code(code)
            .authenticationScheme(authenticationScheme)
            .credentialToken(credentialToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInResendCodeCommandParameters(): SignInResendCodeCommandParameters {
        return SignInResendCodeCommandParameters.builder()
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .credentialToken(credentialToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSignInSubmitPasswordCommandParameters(): SignInSubmitPasswordCommandParameters {
        val authenticationScheme = AuthenticationSchemeFactory.createScheme(
            AndroidPlatformComponents.createFromContext(context),
            null
        )

        return SignInSubmitPasswordCommandParameters.builder()
            .password(password)
            .authenticationScheme(authenticationScheme)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .credentialToken(credentialToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprStartCommandParameters(): SsprStartCommandParameters {
        return SsprStartCommandParameters.builder()
            .username(username)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprSubmitCodeCommandParameters(): SsprSubmitCodeCommandParameters {
        return SsprSubmitCodeCommandParameters.builder()
            .code(code)
            .passwordResetToken(passwordResetToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprResendCodeCommandParameters(): SsprResendCodeCommandParameters {
        return SsprResendCodeCommandParameters.builder()
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .passwordResetToken(passwordResetToken)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createSsprSubmitNewPasswordCommandParameters(): SsprSubmitNewPasswordCommandParameters {
        return SsprSubmitNewPasswordCommandParameters.builder()
            .newPassword(newPassword)
            .passwordSubmitToken(passwordSubmitToken)
            .authority(NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(authorityUrl, clientId))
            .clientId(clientId)
            .platformComponents(platformComponents)
            .oAuth2TokenCache(createCache())
            .sdkType(SdkType.MSAL)
            .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
            .build()
    }

    private fun createCache(): MsalOAuth2TokenCache<*, *, *, *, *> {
        return MsalOAuth2TokenCache.create(platformComponents)
    }
}
