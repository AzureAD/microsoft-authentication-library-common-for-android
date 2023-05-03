package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.integration

import android.os.Build
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiEndpointType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiResponseType
import com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCommandParameters
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
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeErrorCodes
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartErrorCodes
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitErrorCodes
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.UUID

/**
 * These are integration tests using real API responses instead of mocked API responses. This class
 * covers all sign up endpoints.
 * These tests run on the mock API, see: https://native-ux-mock-api.azurewebsites.net/
 */

@RunWith(
    PowerMockRunner::class
)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest(DiagnosticContext::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SsprOAuth2StrategyTest {
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

    /**
     * Sspr Start Tests
     */
    @Test
    fun testPerformSsprStartSuccessWithUsername() {
        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        assertTrue(ssprStartResult.success)
    }

    @Test
    fun testPerformSsprStartVerificationInvalidClientError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.INVALID_CLIENT
        )

        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username

        val ssprResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        assertFalse(ssprResult.success)
        assertEquals(ssprResult.errorResponse!!.error, SsprStartErrorCodes.INVALID_CLIENT)
    }

    @Test
    fun testPerformSsprStartUnsupportedChallengeTypeRequestError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )

        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        assertFalse(ssprStartResult.success)
        assertEquals(ssprStartResult.errorResponse!!.error, SsprStartErrorCodes.UNSUPPORTED_CHALLENGE_TYPE)
    }

    @Test
    fun testPerformSsprStartChallengeTypeRedirectError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        assertTrue(ssprStartResult.success)
        assertEquals(ssprStartResult.successResponse!!.challengeType, NativeAuthChallengeType.REDIRECT)
    }

    /**
     * expected:<[invalid_grant]> but was:<[user_not_found]>
     * Expected :[invalid_grant]
     * Actual   :[user_not_found]
     */
    @Test
    fun testPerformSsprStartUserNotFoundError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRStart,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPLICITLY_USER_NOT_FOUND
        )

        val mockSsprStartCommandParameters = mockk<SsprStartCommandParameters>()
        every { mockSsprStartCommandParameters.getUsername() } returns username

        val ssprStartResult = nativeAuthOAuth2Strategy.performSsprStart(
            mockSsprStartCommandParameters
        )
        assertFalse(ssprStartResult.success)
        assertEquals(SsprStartErrorCodes.USER_NOT_FOUND, ssprStartResult.errorResponse!!.error)
    }

    /**
     * Sspr Challenge Tests
     */
    @Test
    fun testPerformSsprChallengeSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performSsprChallenge(
            passwordResetToken = "1234"
        )
        assertTrue(ssprChallengeResult.success)
        assertEquals(ssprChallengeResult.successResponse!!.challengeType, NativeAuthChallengeType.OOB)
    }

    @Test
    fun testPerformSsprChallengeExpiredTokenError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRChallenge,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.EXPIRED_TOKEN
        )

        val ssprChallengeResult = nativeAuthOAuth2Strategy.performSsprChallenge(
            passwordResetToken = "1234"
        )

        assertFalse(ssprChallengeResult.success)
        assertEquals(ssprChallengeResult.errorResponse!!.error, SsprChallengeErrorCodes.EXPIRED_TOKEN)
    }

    @Test
    fun testPerformSsprContinueSuccess() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRContinue,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )

        val mockSsprContinueCommandParameters = mockk<SsprContinueCommandParameters>()
        every { mockSsprContinueCommandParameters.getOobCode() } returns oobCode

        val ssprContinueResult = nativeAuthOAuth2Strategy.performSsprContinue(
            passwordResetToken = "1234",
            mockSsprContinueCommandParameters
        )
        assertTrue(ssprContinueResult.success)
    }

    @Test
    fun testPerformSsprSubmitSuccess() {
        val mockSsprSubmitCommandParameters = mockk<SsprSubmitCommandParameters>()
        every { mockSsprSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performSsprSubmit(
            passwordSubmitToken = "1234",
            mockSsprSubmitCommandParameters
        )
        assertTrue(ssprSubmitResult.success)
    }

    @Test
    fun testPerformSsprSubmitPasswordTooWeakError() {
        configureMockApi(
            endpointType = MockApiEndpointType.SSPRSubmit,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val mockSsprSubmitCommandParameters = mockk<SsprSubmitCommandParameters>()
        every { mockSsprSubmitCommandParameters.getNewPassword() } returns password

        val ssprSubmitResult = nativeAuthOAuth2Strategy.performSsprSubmit(
            passwordSubmitToken = "1234",
            mockSsprSubmitCommandParameters
        )
        assertFalse(ssprSubmitResult.success)
        assertEquals(ssprSubmitResult.errorResponse!!.error, SsprSubmitErrorCodes.PASSWORD_TOO_WEAK)
    }

    @Test
    @Ignore
    fun testPerformSsprPollCompletionSuccess() {
        val ssprPollCompletionResult = nativeAuthOAuth2Strategy.performSsprPollCompletion(
            passwordResetToken = "1234"
        )
        assertTrue(ssprPollCompletionResult.success)
    }
}
