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
import com.microsoft.identity.common.java.controllers.results.Complete
import com.microsoft.identity.common.java.controllers.results.EmailVerificationRequired
import com.microsoft.identity.common.java.controllers.results.IncorrectCode
import com.microsoft.identity.common.java.controllers.results.PasswordIncorrect
import com.microsoft.identity.common.java.controllers.results.UserNotFound
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil
import org.junit.Before
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
    fun testSignInStartWithRopcEmailVerificationRequired() {
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
        assert(result is EmailVerificationRequired)
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
        assert(result is UserNotFound)
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
        assert(result is UserNotFound)
    }

    @Test
    fun testSignInStartWithEmailEmailVerificationRequired() {
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
        assert(result is EmailVerificationRequired)
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
        assert(result is EmailVerificationRequired)
    }

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

    private fun createCache(): MsalOAuth2TokenCache<*, *, *, *, *> {
        return MsalOAuth2TokenCache.create(platformComponents)
    }
}
