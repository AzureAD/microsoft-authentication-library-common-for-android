package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.internal.commands.parameters.SignUpChallengeCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
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
    private val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/challenge")
    private val challengeTypes = "oob password redirect"
    private val emptyString = ""

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns signUpStartRequestUrl
        every { getSignUpChallengeEndpoint() } returns signUpChallengeRequestUrl
        every { challengeTypes } returns this@NativeAuthRequestHandlerTest.challengeTypes
        every { clientId } returns this@NativeAuthRequestHandlerTest.clientId
    }

    private val nativeAthRequestProvider =
        NativeAuthRequestProvider(
            config = mockConfig
        )

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
        every { mockConfig.challengeTypes } returns emptyString

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
        assertEquals(challengeTypes, result.parameters.challengeTypes)
        assertEquals(signUpStartRequestUrl, result.requestUrl)
        assertEquals("{\"city\":\"Dublin\"}", result.parameters.attributes.toString())
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptySignUpTokenShouldThrowException() {
        val commandParameters = SignUpChallengeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .build()

        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "",
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString
        every { mockConfig.challengeTypes } returns "oob redirect password"

        val commandParameters = SignUpChallengeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .build()

        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "",
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.clientId } returns "1234"
        every { mockConfig.challengeTypes } returns emptyString

        val commandParameters = SignUpChallengeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .build()

        nativeAthRequestProvider.createSignUpChallengeRequest(
            signUpToken = "",
            commandParameters = commandParameters
        )
    }
}
