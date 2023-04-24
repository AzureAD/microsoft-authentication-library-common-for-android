package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.scenario

import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.UserAttributes
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SsprInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthPollCompletionStatus
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL
import java.util.UUID

class SsprScenarioTest {
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
    private val ssprStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/start")
    private val ssprChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/challenge")
    private val ssprContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/continue")
    private val ssprSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/submit")
    private val ssprPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/poll_completion")
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeType = "oob redirect"
    private val userAttributes = UserAttributes.customAttribute("city", "Dublin").build()
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
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(signInTokenRequestUrl)
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(signUpContinueRequestUrl)
        whenever(mockConfig.getSsprStartEndpoint()).thenReturn(ssprStartRequestUrl)
        whenever(mockConfig.getSsprChallengeEndpoint()).thenReturn(ssprChallengeRequestUrl)
        whenever(mockConfig.getSsprContinueEndpoint()).thenReturn(ssprContinueRequestUrl)
        whenever(mockConfig.getSsprSubmitEndpoint()).thenReturn(ssprSubmitRequestUrl)
        whenever(mockConfig.getSsprPollCompletionEndpoint()).thenReturn(ssprPollCompletionRequestUrl)
        whenever(mockConfig.challengeType).thenReturn(challengeType)

        nativeAuthOAuth2Strategy = NativeAuthOAuth2Strategy(
            config = mockConfig,
            strategyParameters = mockStrategyParams,
            signInInteractor = SignInInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            signUpInteractor = SignUpInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            ),
            ssprInteractor = SsprInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.1: Verify email with email OTP first and then reset password
    @Test
    @Ignore
    fun testSsprScenarioEmailVerificationThenResetPassword() {
        var passwordResetToken = "1234"
        var passwordSubmitToken = "1234"
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult.success)
        passwordResetToken = ssprStartResult.successResponse!!.passwordResetToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performSsprChallenge(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprChallengeResult.success)
        passwordResetToken = ssprChallengeResult.successResponse!!.passwordResetToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockSsprContinueCommandParameters = mockk<SsprContinueCommandParameters>()
        every { mockSsprContinueCommandParameters.getOobCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performSsprContinue(
            passwordResetToken = passwordResetToken,
            mockSsprContinueCommandParameters
        )
        Assert.assertTrue(ssprContinueResult.success)
        passwordSubmitToken = ssprContinueResult.successResponse!!.passwordSubmitToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        val mockSsprSubmitCommandParameters = mockk<SsprSubmitCommandParameters>()
        every { mockSsprSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performSsprSubmit(
            passwordSubmitToken = passwordSubmitToken,
            mockSsprSubmitCommandParameters
        )
        Assert.assertTrue(ssprSubmitResult.success)
        passwordResetToken = ssprSubmitResult.successResponse!!.passwordResetToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )

        val ssprPollResult = nativeAuthOAuth2Strategy.performSsprPollCompletion(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprPollResult.success)
        Assert.assertEquals(ssprPollResult.successResponse!!.status, NativeAuthPollCompletionStatus.SUCCEEDED)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.4: Email is not found in records
    @Test
    @Ignore
    fun testSsprScenarioEmailNotFound() {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )
        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        Assert.assertFalse(ssprStartResult.success)
        Assert.assertEquals(
            ssprStartResult.errorResponse!!.error,
            "invalid_grant"
        )
        Assert.assertEquals(
            ssprStartResult.errorResponse!!.errorDescription,
            "User not found"
        )
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.8: New password being set does not meet password complexity requirements set on portal
    @Test
    fun testSsprScenarioPasswordComplexity() {
        var passwordResetToken = "1234"
        var passwordSubmitToken = "1234"
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult.success)

        passwordResetToken = ssprStartResult.successResponse!!.passwordResetToken.toString()
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performSsprChallenge(
            passwordResetToken = passwordResetToken
        )
        Assert.assertTrue(ssprChallengeResult.success)
        passwordResetToken = ssprChallengeResult.successResponse!!.passwordResetToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        val mockSsprContinueCommandParameters = mockk<SsprContinueCommandParameters>()
        every { mockSsprContinueCommandParameters.getOobCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performSsprContinue(
            passwordResetToken = passwordResetToken,
            mockSsprContinueCommandParameters
        )
        Assert.assertTrue(ssprContinueResult.success)
        passwordSubmitToken = ssprContinueResult.successResponse!!.passwordSubmitToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )
        val mockSsprSubmitCommandParameters = mockk<SsprSubmitCommandParameters>()
        every { mockSsprSubmitCommandParameters.getNewPassword() } returns password
        val ssprSubmitResult = nativeAuthOAuth2Strategy.performSsprSubmit(
            passwordSubmitToken = passwordSubmitToken,
            mockSsprSubmitCommandParameters
        )
        Assert.assertFalse(ssprSubmitResult.success)

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )
        every { mockSsprSubmitCommandParameters.getNewPassword() } returns password
        val ssprPollResult = nativeAuthOAuth2Strategy.performSsprPollCompletion(
            passwordResetToken = passwordSubmitToken
        )
        Assert.assertFalse(ssprPollResult.success)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 3.1.9: Continuous attempts to reset password for single email with wrong OTP
    @Test
    fun testSsprScenarioSingleEmailWrongOTP() {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username
        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        Assert.assertTrue(ssprStartResult.success)

        var passwordResetToken = ssprStartResult.successResponse!!.passwordResetToken
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        val ssprChallengeResult = nativeAuthOAuth2Strategy.performSsprChallenge(
            passwordResetToken = passwordResetToken.toString()
        )
        Assert.assertTrue(ssprChallengeResult.success)
        passwordResetToken = ssprChallengeResult.successResponse!!.passwordResetToken

        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )
        val mockSsprContinueCommandParameters = mockk<SsprContinueCommandParameters>()
        every { mockSsprContinueCommandParameters.getOobCode() } returns oobCode
        val ssprContinueResult = nativeAuthOAuth2Strategy.performSsprContinue(
            passwordResetToken = passwordResetToken.toString(),
            mockSsprContinueCommandParameters
        )
        Assert.assertFalse(ssprContinueResult.success)
        Assert.assertEquals(
            ssprContinueResult.errorResponse!!.error,
            "invalid_grant"
        )
    }
}
