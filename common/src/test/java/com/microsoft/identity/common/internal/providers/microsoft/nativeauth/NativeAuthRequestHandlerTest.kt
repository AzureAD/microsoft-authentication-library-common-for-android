package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprContinueCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprStartCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SsprSubmitCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.UserAttributes
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthRequestProvider
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
    private val emptyString = ""
    private val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val ssprStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val ssprChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val ssprContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val ssprSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val ssprPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeType = "oob redirect"
    private val userAttributes = UserAttributes.customAttribute("city", "Dublin").build()
    private val challengeTargetKey = "user1"
    private val grantType = "password"
    private val oobCode = "123456"

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns signUpStartRequestUrl
        every { getSignUpChallengeEndpoint() } returns signUpChallengeRequestUrl
        every { getSsprStartEndpoint() } returns ssprStartRequestUrl
        every { getSsprChallengeEndpoint() } returns ssprChallengeRequestUrl
        every { getSsprContinueEndpoint() } returns ssprContinueRequestUrl
        every { getSsprSubmitEndpoint() } returns ssprSubmitRequestUrl
        every { getSsprPollCompletionEndpoint() } returns ssprPollCompletionRequestUrl
        every { challengeType } returns this@NativeAuthRequestHandlerTest.challengeType
        every { clientId } returns this@NativeAuthRequestHandlerTest.clientId
        every { grantType } returns this@NativeAuthRequestHandlerTest.grantType
    }

    private val nativeAthRequestProvider = NativeAuthRequestProvider(
        config = mockConfig
    )

    // signup start tests
    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyUsernameShouldThrowException() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .email(emptyString)
            .build()

        nativeAthRequestProvider.createSignUpStartRequest(
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

        nativeAthRequestProvider.createSignUpStartRequest(
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

        nativeAthRequestProvider.createSignUpStartRequest(
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

        val result = nativeAthRequestProvider.createSignUpStartRequest(
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
        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = emptyString
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString
        every { mockConfig.challengeType } returns "oob redirect password"

        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "1234"
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.clientId } returns "1234"
        every { mockConfig.challengeType } returns emptyString

        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "1234"
        )
    }

    // sspr start tests
    @Test(expected = ClientException::class)
    fun testSsprStartWithEmptyUsernameShouldThrowException() {
        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .build()

        nativeAthRequestProvider.createSsprStartRequest(
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

        nativeAthRequestProvider.createSsprStartRequest(
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

        nativeAthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSsprStartSuccess() {
        val commandParameters = SsprStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .build()

        val result = nativeAthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    // sspr challenge tests
    @Test(expected = ClientException::class)
    fun testSsprChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = "123456"
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprChallengeWithEmptyPasswordResetTokenShouldThrowException() {
        nativeAthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = emptyString
        )
    }

    @Test
    fun testSsprChallengeSuccess() {
        nativeAthRequestProvider.createSsprChallengeRequest(
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

        nativeAthRequestProvider.createSsprContinueRequest(
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

        nativeAthRequestProvider.createSsprContinueRequest(
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

        nativeAthRequestProvider.createSsprContinueRequest(
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

        nativeAthRequestProvider.createSsprContinueRequest(
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

        nativeAthRequestProvider.createSsprSubmitRequest(
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

        nativeAthRequestProvider.createSsprSubmitRequest(
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

        nativeAthRequestProvider.createSsprSubmitRequest(
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

        nativeAthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = "123456",
            commandParameters = commandParameters
        )
    }

    // sspr completion poll tests
    @Test(expected = ClientException::class)
    fun testSsprPollCompletionWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = "123456"
        )
    }

    @Test(expected = ClientException::class)
    fun testSsprPollCompletionWithEmptyPasswordSubmitTokenShouldThrowException() {
        nativeAthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = emptyString
        )
    }

    @Test
    fun testSsprPollCompletionSuccess() {
        nativeAthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = "123456"
        )
    }
}
