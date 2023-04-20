package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.integration

import android.os.Build
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.UserAttributes
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SsprInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.exceptions.ErrorCodes
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
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
class SignInOAuthStrategyTest {
    private val username = "user@email.com"
    private val password = "verySafePassword"
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
    private val challengeType = "password"
    private val userAttributes = UserAttributes.customAttribute("city", "Dublin").build()
    private val credentialToken = "uY29tL2F1dGhlbnRpY"
    private val grantType = "oob"
    private val oob = "1234"

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
            ssprInteractor = SsprInteractor(
                httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                nativeAuthRequestProvider = NativeAuthRequestProvider(mockConfig),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    @Ignore
    @Test
    fun testPerformSignInInitiateSuccessWithOnlyEmail() {
        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username

        val signInInitiateResult = nativeAuthOAuth2Strategy.performSignInInitiate(
            signInCommandParameters
        )
        Assert.assertTrue(signInInitiateResult.success)
    }

    @Ignore
    @Test
    fun testPerformSignInChallengeSuccess() {
        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            credentialToken = "1234"
        )

        Assert.assertTrue(signInChallengeResult.success)
        Assert.assertEquals(signInChallengeResult.successResponse!!.challengeType, "redirect")
    }

    @Ignore
    @Test
    fun testPerformSignInTokenWithPasswordSuccess() {
        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username
        every { signInCommandParameters.getPassword() } returns password

        val signInChallengeResult = nativeAuthOAuth2Strategy.performGetToken(
            credentialToken = "1234",
            signInCommandParameters = signInCommandParameters
        )

        Assert.assertTrue(signInChallengeResult.success)
        Assert.assertTrue(!signInChallengeResult.successResponse!!.accessToken.isNullOrBlank())
    }

    @Ignore
    @Test
    fun testPerformSignInTokenWithOobSuccess() {
        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username
        every { signInCommandParameters.getOob() } returns oob

        val signInChallengeResult = nativeAuthOAuth2Strategy.performGetToken(
            credentialToken = "1234",
            signInCommandParameters = signInCommandParameters
        )

        Assert.assertTrue(signInChallengeResult.success)
        Assert.assertTrue(!signInChallengeResult.successResponse!!.accessToken.isNullOrBlank())
    }

    @Ignore
    @Test
    fun testPerformSignInInitiateWithChallengeTypeRedirectSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInInitiate,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username

        val signInInitiateResult = nativeAuthOAuth2Strategy.performSignInInitiate(
            signInCommandParameters
        )
        Assert.assertTrue(signInInitiateResult.success)
        Assert.assertEquals(
            signInInitiateResult.successResponse!!.challengeType,
            NativeAuthChallengeType.REDIRECT.toString().lowercase()
        )
    }

    @Ignore
    @Test
    fun testPerformSignInChallengeWithChallengeTypeOobSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username

        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            credentialToken = credentialToken
        )
        Assert.assertTrue(signInChallengeResult.success)
        Assert.assertEquals(
            signInChallengeResult.successResponse!!.challengeType,
            NativeAuthChallengeType.OOB.toString().lowercase()
        )
    }

    @Ignore
    @Test
    fun testPerformSignInChallengeWithRedirectSuccess() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username

        val signInChallengeResult = nativeAuthOAuth2Strategy.performSignInChallenge(
            credentialToken = credentialToken
        )
        Assert.assertTrue(signInChallengeResult.success)
        Assert.assertEquals(
            signInChallengeResult.successResponse!!.challengeType,
            NativeAuthChallengeType.REDIRECT.toString().lowercase()
        )
    }

    @Ignore
    @Test
    fun testPerformSignInTokenWithCredentialRequiredError() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CREDENTIAL_REQUIRED
        )

        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username
        every { signInCommandParameters.getPassword() } returns password

        val signInChallengeResult = nativeAuthOAuth2Strategy.performGetToken(
            credentialToken = credentialToken,
            signInCommandParameters = signInCommandParameters
        )
        Assert.assertFalse(signInChallengeResult.success)
        Assert.assertEquals(
            signInChallengeResult.errorResponse!!.error,
            ErrorCodes.CREDENTIAL_REQUIRED
        )
    }

    @Ignore
    @Test
    fun testPerformSignInTokenWithInvalidGrantError() {
        MockApiUtils.configureMockApi(
            endpointType = MockApiEndpointType.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_GRANT
        )

        val signInCommandParameters = mockk<SignInCommandParameters>()
        every { signInCommandParameters.getUsername() } returns username
        every { signInCommandParameters.getPassword() } returns password

        val signInChallengeResult = nativeAuthOAuth2Strategy.performGetToken(
            credentialToken = credentialToken,
            signInCommandParameters = signInCommandParameters
        )
        Assert.assertFalse(signInChallengeResult.success)
        Assert.assertEquals(
            signInChallengeResult.errorResponse!!.error,
            ErrorCodes.INVALID_GRANT
        )
    }
}
