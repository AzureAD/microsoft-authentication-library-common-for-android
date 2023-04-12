package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.internal.commands.parameters.SignInCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpContinueCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprContinueCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprStartCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprSubmitCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.UserAttributes
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthGrantType
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.URL

class NativeAuthRequestHandlerTest {
    private val username = "user@email.com"
    private val password = "verySafePassword"
    private val clientId = "1234"
    private val tenant = "samtoso.onmicrosoft.com"
    private val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val signUpContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/continue")
    private val signInInitiateRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/initiate")
    private val signInChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/challenge")
    private val signInTokenRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth/v2.0/token")
    private val ssprStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val ssprChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val ssprContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val ssprSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val ssprPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeType = "oob redirect"
    private val userAttributes = UserAttributes.customAttribute("city", "Dublin").build()
    private val oobGrantType = "oob"
    private val oobCode = "123456"
    private val emptyString = ""
    private val credentialToken = "uY29tL2F1dGhlbnRpY"
    private val grantType = NativeAuthGrantType.PASSWORDLESS_OTP.jsonValue

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns signUpStartRequestUrl
        every { getSignUpChallengeEndpoint() } returns signUpChallengeRequestUrl
        every { getSignUpContinueEndpoint() } returns signUpContinueRequestUrl
        every { getSignInInitiateEndpoint() } returns signInInitiateRequestUrl
        every { getSignInChallengeEndpoint() } returns signInChallengeRequestUrl
        every { getSignInTokenEndpoint() } returns signInTokenRequestUrl
        every { getSsprStartEndpoint() } returns ssprStartRequestUrl
        every { getSsprChallengeEndpoint() } returns ssprChallengeRequestUrl
        every { getSsprContinueEndpoint() } returns ssprContinueRequestUrl
        every { getSsprSubmitEndpoint() } returns ssprSubmitRequestUrl
        every { getSsprPollCompletionEndpoint() } returns ssprPollCompletionRequestUrl
        every { challengeType } returns this@NativeAuthRequestHandlerTest.challengeType
        every { clientId } returns this@NativeAuthRequestHandlerTest.clientId
    }

    private val nativeAuthRequestProvider =
        NativeAuthRequestProvider(
            config = mockConfig
        )

    // signup start tests
    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyUsernameShouldThrowException() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .email(emptyString)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .email(username)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .email(username)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignUpStartSuccess() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .email(username)
            .userAttributes(UserAttributes.customAttribute("city", "Dublin").build())
            .build()

        val result = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(signUpStartRequestUrl, result.requestUrl)
        assertEquals("{\"city\":\"Dublin\"}", result.parameters.attributes.toString())
    }

    // signup challenge tests
    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptySignUpTokenShouldThrowException() {
        nativeAuthRequestProvider.createSignUpChallengeRequest(
            signUpToken = emptyString
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString
        every { mockConfig.challengeType } returns "oob redirect password"

        nativeAuthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "1234"
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.clientId } returns "1234"
        every { mockConfig.challengeType } returns emptyString

        nativeAuthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "1234"
        )
    }

    // signin tests
    @Test(expected = ClientException::class)
    fun testSignInInitiateWithEmptyUsernameShouldThrowException() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInInitiateSuccess() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(signInInitiateRequestUrl, result.requestUrl)
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeWithEmptyCredentialTokenShouldThrowException() {
        nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = emptyString
        )
    }

    @Test
    fun testSignInChallengeSuccess() {
        val result = nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(credentialToken, result.parameters.credentialToken)
        assertEquals(signInChallengeRequestUrl, result.requestUrl)
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createTokenRequest(
            signInCommandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyGrantTypeShouldThrowException() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createTokenRequest(
            signInCommandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyUsernameShouldThrowException() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .build()

        nativeAuthRequestProvider.createTokenRequest(
            signInCommandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyOobProvidedGrantTypeShouldThrowException() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .oob(emptyString)
            .build()

        nativeAuthRequestProvider.createTokenRequest(
            signInCommandParameters = commandParameters
        )
    }

    @Test
    fun testSignInTokenSuccess() {
        val commandParameters = SignInCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .oob(oobCode)
            .build()

        val result = nativeAuthRequestProvider.createTokenRequest(
            signInCommandParameters = commandParameters,
            credentialToken = credentialToken
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(oobGrantType, result.parameters.grantType)
        assertEquals(signInTokenRequestUrl, result.requestUrl)
    }

    @Test(expected = ClientException::class)
    fun testSignUpContinueWithEmptySignUpTokenThrowException() {
        val commandParameters = SignUpContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oob(oobCode)
            .build()

        nativeAuthRequestProvider.createSignUpContinueRequest(
            signUpToken = emptyString,
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpContinueWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignUpContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oob(oobCode)
            .build()

        nativeAuthRequestProvider.createSignUpContinueRequest(
            signUpToken = "1234",
            commandParameters = commandParameters
        )
    }

    // signup continue tests
    @Test
    fun testSignUpContinueSuccess() {
        val commandParameters = SignUpContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oob(oobCode)
            .userAttributes(UserAttributes.customAttribute("city", "Dublin").build())
            .build()

        val result = nativeAuthRequestProvider.createSignUpContinueRequest(
            signUpToken = "1234",
            commandParameters = commandParameters
        )

        assertEquals(oobCode, result.parameters.oob)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(grantType, result.parameters.grantType)
        assertEquals(signUpContinueRequestUrl, result.requestUrl)
        assertEquals("{\"city\":\"Dublin\"}", result.parameters.attributes.toString())
    }

    // sspr start tests
    @Test(expected = ClientException::class)
    fun testSsprStartWithEmptyUsernameShouldThrowException() {
        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .build()

        nativeAuthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprStartWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprStartWithEmptyChallengeTypeShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        nativeAuthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSsprStartSuccess() {
        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        val result = nativeAuthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    // sspr challenge tests
    @Test(expected = ClientException::class)
    fun testSsprChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = "123456"
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprChallengeWithEmptyPasswordResetTokenShouldThrowException() {
        nativeAuthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = emptyString
        )
    }

    @Test
    fun testSsprChallengeSuccess() {
        nativeAuthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = "123456"
        )
    }

    // sspr continue tests
    @Test(expected = ClientException::class)
    fun testSsprContinueWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SsprContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oobCode("123456")
            .build()

        nativeAuthRequestProvider.createSsprContinueRequest(
            passwordResetToken = "123456",
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprContinueWithEmptyPasswordResetTokenShouldThrowException() {
        val commandParameters = SsprContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oobCode("123456")
            .build()

        nativeAuthRequestProvider.createSsprContinueRequest(
            passwordResetToken = emptyString,
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprContinueWithEmptyOobCodeShouldThrowException() {
        val commandParameters = SsprContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oobCode(emptyString)
            .build()

        nativeAuthRequestProvider.createSsprContinueRequest(
            passwordResetToken = "123456",
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSsprContinueSucces() {
        val commandParameters = SsprContinueCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .oobCode("123456")
            .build()

        nativeAuthRequestProvider.createSsprContinueRequest(
            passwordResetToken = "123456",
            commandParameters = commandParameters
        )
    }

    // sspr submit tests
    @Test(expected = ClientException::class)
    fun testSsprSubmitWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SsprSubmitCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .newPassword(password)
            .build()

        nativeAuthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = "123456",
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprSubmitWithEmptyPasswordShouldThrowException() {
        val commandParameters = SsprSubmitCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .newPassword(emptyString)
            .build()

        nativeAuthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = "123456",
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprSubmitWithEmptyPasswordSubmitTokenShouldThrowException() {
        val commandParameters = SsprSubmitCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .newPassword(password)
            .build()

        nativeAuthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = emptyString,
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSsprSubmitSuccess() {
        val commandParameters = SsprSubmitCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .newPassword(password)
            .build()

        nativeAuthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = "123456",
            commandParameters = commandParameters
        )
    }

    // sspr completion poll tests
    @Test(expected = ClientException::class)
    fun testSsprPollCompletionWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = "123456"
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprPollCompletionWithEmptyPasswordSubmitTokenShouldThrowException() {
        nativeAuthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = emptyString
        )
    }

    @Test
    fun testSsprPollCompletionSuccess() {
        nativeAuthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = "123456"
        )
    }
}
