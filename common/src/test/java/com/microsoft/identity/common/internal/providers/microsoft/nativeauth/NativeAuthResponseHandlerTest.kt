package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.Attribute
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.exceptions.ErrorCodes
import com.microsoft.identity.common.java.exception.ClientException
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class NativeAuthResponseHandlerTest {
    private val username = "user@email.com"
    private val password = "verySafePassword"
    private val clientId = "1234"
    private val requestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val challengeTypes = "oob password redirect"
    private val emptyString = ""

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns requestUrl
        every { challengeTypes } returns this@NativeAuthResponseHandlerTest.challengeTypes
        every { clientId } returns this@NativeAuthResponseHandlerTest.clientId
    }

    private val nativeAuthResponseHandler = NativeAuthResponseHandler()

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithSuccessAndMissingObject() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(true)
        whenever(signUpStartResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithSuccessAndMissingSignupToken() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(true)
        val signUpResultSuccessResponse = SignUpStartResponse(
            signupToken = null
        )
        whenever(signUpStartResult.successResponse).thenReturn(signUpResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test
    fun testValidateSignUpStartResultWithSuccessAndNoMissingObject() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(true)
        val signUpResultSuccessResponse = mock<SignUpStartResponse>()
        whenever(signUpResultSuccessResponse.signupToken).thenReturn("1234")
        whenever(signUpStartResult.successResponse).thenReturn(signUpResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndMissingObject() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        whenever(signUpStartResult.errorResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndMissingErrorCode() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = null,
            errorDescription = "error description",
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndEmptyErrorCode() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = "",
            errorDescription = "error description",
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndMissingErrorDescription() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = "invalid_request",
            errorDescription = null,
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndEmptyErrorDescription() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = "invalid_request",
            errorDescription = "",
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithVerificationRequiredErrorAndMissingAttributes() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = ErrorCodes.VERIFICATION_REQUIRED,
            errorDescription = null,
            verifyAttributes = null,
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithVerificationRequiredErrorAndMissingSignUpToken() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = ErrorCodes.VERIFICATION_REQUIRED,
            errorDescription = null,
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = null
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithValidationFailedErrorAndMissingAttributes() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = ErrorCodes.VALIDATION_FAILED,
            errorDescription = null,
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = null,
            challengeType = "oob",
            signupToken = null
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithSuccessAndMissingObject() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        whenever(signUpChallengeResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithSuccessAndMissingSignupToken() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = null,
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = "prompt",
            interval = "300",
            displayName = "...r@microsoft.com"
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithSuccessAndMissingChallengeType() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = null,
            codeLength = 6,
            bindingMethod = "prompt",
            interval = "300",
            displayName = "...r@microsoft.com"
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingCodeLength() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = "oob",
            codeLength = null,
            bindingMethod = "prompt",
            interval = "300",
            displayName = "...r@microsoft.com"
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingBindingMethod() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = null,
            interval = "300",
            displayName = "...r@microsoft.com"
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingInterval() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = "prompt",
            interval = null,
            displayName = "...r@microsoft.com"
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingDisplayName() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = "prompt",
            interval = "300",
            displayName = null
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndNoMissingObject() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = mock<SignUpChallengeResponse>()
        whenever(signUpChallengeResponse.signupToken).thenReturn("1234")
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithErrorAndMissingObject() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(false)
        whenever(signUpChallengeResult.errorResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult,
        )
    }
}
