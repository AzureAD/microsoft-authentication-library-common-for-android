package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.scenario

import com.microsoft.identity.common.internal.commands.parameters.SignUpContinueCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.UserAttributes
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.internal.providers.oauth2.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.SsprInteractor
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartErrorCodes
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
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

class SignUpScenarioTest {
    private val username = "user@email.com"
    private val email = "user@email.com"
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
        whenever(mockConfig.getSignUpContinueEndpoint()).thenReturn(signUpContinueRequestUrl)
        whenever(mockConfig.getSignInInitiateEndpoint()).thenReturn(signInInitiateRequestUrl)
        whenever(mockConfig.getSignInChallengeEndpoint()).thenReturn(signInChallengeRequestUrl)
        whenever(mockConfig.getSignInTokenEndpoint()).thenReturn(signInTokenRequestUrl)
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
    // Scenario 1.1.1: Set email and password and then verify email OOB as last step
    @Test
    fun testSignUpScenarioEmailPasswordWithOOBVerification() {
        var signUpToken = "1234"
        val correlationId = UUID.randomUUID().toString()

        val mockSignUpStartCommandParameters = mockk<SignUpStartCommandParameters>()
        every { mockSignUpStartCommandParameters.getEmail() } returns email
        every { mockSignUpStartCommandParameters.getPassword() } returns password
        every { mockSignUpStartCommandParameters.getUserAttributes() } returns null

        val signupStartResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )
        Assert.assertTrue(signupStartResult.success)
        signUpToken = signupStartResult.successResponse!!.signupToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        Assert.assertTrue(signupChallengeResult.success)
        Assert.assertEquals(signupChallengeResult.successResponse!!.challengeType, NativeAuthChallengeType.OOB)
        signUpToken = signupChallengeResult.successResponse!!.signupToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            mockSignUpContinueCommandParameters
        )
        Assert.assertTrue(signupContinueResult.success)
    }

    // Acceptance criteria for Native Authentication:
    // https://microsofteur-my.sharepoint.com/:w:/r/personal/sodenhoven_microsoft_com/Documents/NativeAuth%20-%20Acceptance%20criteria.docx?d=w4fc5ef1ac9d948b0be7ab551f54a59a8&csf=1&web=1&e=8OYikN
    // Scenario 1.1.7: Verify email address using email OTP and then set password
    @Ignore
    @Test
    fun testSignUpScenarioEmailVerificationFirstThenPassword() {
        var signUpToken = "1234"
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        val mockSignUpStartCommandParameters = mockk<SignUpStartCommandParameters>()
        every { mockSignUpStartCommandParameters.getEmail() } returns email
        every { mockSignUpStartCommandParameters.getUserAttributes() } returns null

        val signupStartResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )

        Assert.assertFalse(signupStartResult.success)
        Assert.assertEquals(signupStartResult.errorResponse!!.error, SignUpStartErrorCodes.VERIFICATION_REQUIRED.toString())
        signUpToken = signupStartResult.errorResponse!!.signupToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = signUpToken
        )
        Assert.assertTrue(signupChallengeResult.success)
        Assert.assertEquals(signupChallengeResult.successResponse!!.challengeType, "oob")
        signUpToken = signupChallengeResult.successResponse!!.signupToken.toString()

        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = signUpToken,
            mockSignUpContinueCommandParameters
        )
        Assert.assertTrue(signupContinueResult.success)
    }
}
