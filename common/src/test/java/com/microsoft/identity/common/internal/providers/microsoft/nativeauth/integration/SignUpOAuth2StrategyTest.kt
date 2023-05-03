package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.integration

import android.os.Build
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.exception.ClientException
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
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartErrorCodes
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
class SignUpOAuth2StrategyTest {
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
    private val challengeTypes = "oob password redirect"
    private val userAttributes = mapOf(Pair("city", "Dublin"))
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
        whenever(mockConfig.challengeType).thenReturn(challengeTypes)

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
                nativeAuthRequestProvider = NativeAuthRequestProvider(
                    mockConfig
                ),
                nativeAuthResponseHandler = NativeAuthResponseHandler()
            )
        )
    }

    @Test
    fun testPerformSignUpStartSuccessWithOnlyEmail() {
        val mockSignUpStartCommandParameters = mockk<SignUpStartCommandParameters>()
        every { mockSignUpStartCommandParameters.getEmail() } returns email
        every { mockSignUpStartCommandParameters.getPassword() } returns null
        every { mockSignUpStartCommandParameters.getUserAttributes() } returns null

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )
        assertTrue(signupResult.success)
    }

    @Test
    fun testPerformSignUpStartSuccessWithEmailAndPassword() {
        val mockSignUpStartCommandParameters = mockk<SignUpStartCommandParameters>()
        every { mockSignUpStartCommandParameters.getEmail() } returns email
        every { mockSignUpStartCommandParameters.getPassword() } returns password
        every { mockSignUpStartCommandParameters.getUserAttributes() } returns null

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )
        assertTrue(signupResult.success)
    }

    @Test
    fun testPerformSignUpStartVerificationRequiredError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.VERIFICATION_REQUIRED
        )

        val mockSignUpStartCommandParameters = mockk<SignUpStartCommandParameters>()
        every { mockSignUpStartCommandParameters.getEmail() } returns email

        val signupResult = nativeAuthOAuth2Strategy.performSignUpStart(
            mockSignUpStartCommandParameters
        )
        assertFalse(signupResult.success)
        assertEquals(signupResult.errorResponse!!.error, SignUpStartErrorCodes.VERIFICATION_REQUIRED.toString())
    }

    @Test
    fun testPerformSignUpChallengeSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val signupChallengeResult = nativeAuthOAuth2Strategy.performSignUpChallenge(
            signUpToken = "1234"
        )
        assertTrue(signupChallengeResult.success)
        assertEquals(signupChallengeResult.successResponse!!.challengeType, NativeAuthChallengeType.OOB)
    }

    @Test
    fun testPerformSignUpContinueSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = "1234",
            mockSignUpContinueCommandParameters
        )
        assertTrue(signupContinueResult.success)
    }

    @Test(expected = ClientException::class)
    fun testPerformSignUpContinueErrorWithUnknownErrorCode() {
        configureMockApi(
            endpointType = MockApiEndpointType.SignUpContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_CLIENT
        )

        val mockSignUpContinueCommandParameters = mockk<SignUpContinueCommandParameters>()
        every { mockSignUpContinueCommandParameters.getOob() } returns oobCode

        val signupContinueResult = nativeAuthOAuth2Strategy.performSignUpContinue(
            signUpToken = "1234",
            mockSignUpContinueCommandParameters
        )
        assertFalse(signupContinueResult.success)
        assertEquals(signupContinueResult.errorResponse!!.error, null)
    }
}
