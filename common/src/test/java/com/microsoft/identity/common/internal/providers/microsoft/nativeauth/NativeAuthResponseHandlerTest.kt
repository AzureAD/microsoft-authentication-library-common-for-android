package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthBindingMethod
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthDisplayType
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthPollCompletionStatus
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.Attribute
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueErrorCodes
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartErrorCodes
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class NativeAuthResponseHandlerTest {
    private val clientId = "1234"
    private val requestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val challengeType = "oob password redirect"
    private val emptyString = ""
    private val userAttributes = mapOf(Pair("city", "Dublin"))
    private val credentialToken = "uY29tL2F1dGhlbnRpY"
    private val error = "invalid_grant"
    private val errorCode = "41093"
    private val errorDescription = "User not found"

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns requestUrl
        every { challengeType } returns this@NativeAuthResponseHandlerTest.challengeType
        every { clientId } returns this@NativeAuthResponseHandlerTest.clientId
    }

    private val nativeAuthResponseHandler = NativeAuthResponseHandler()

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithSuccessAndMissingObject() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(true)
        whenever(signUpStartResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndMissingObject() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        whenever(signUpStartResult.errorResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
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
            unverifiedAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = NativeAuthChallengeType.OOB,
            signupToken = "1234"
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithErrorAndEmptyErrorCode() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = null,
            errorDescription = "error description",
            unverifiedAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = NativeAuthChallengeType.OOB,
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithVerificationRequiredErrorAndMissingAttributes() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = SignUpStartErrorCodes.VERIFICATION_REQUIRED,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = "invalid attributes",
            challengeType = NativeAuthChallengeType.OOB,
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithVerificationRequiredErrorAndMissingSignUpToken() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = SignUpStartErrorCodes.VERIFICATION_REQUIRED,
            errorDescription = null,
            unverifiedAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = NativeAuthChallengeType.OOB,
            signupToken = null
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpStartResultWithValidationFailedErrorAndMissingAttributes() {
        val signUpStartResult = mock<SignUpStartResult>()
        whenever(signUpStartResult.success).thenReturn(false)
        val signUpResultErrorResponse = SignUpStartErrorResponse(
            statusCode = 200,
            errorCode = SignUpStartErrorCodes.VALIDATION_FAILED,
            errorDescription = null,
            unverifiedAttributes = listOf(Attribute("username")),
            invalidAttributes = null,
            challengeType = NativeAuthChallengeType.OOB,
            signupToken = null
        )

        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithSuccessAndMissingObject() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        whenever(signUpChallengeResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
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
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            interval = "300",
            displayName = "...r@microsoft.com",
            displayType = NativeAuthDisplayType.EMAIL
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingCodeLength() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = NativeAuthChallengeType.OOB,
            codeLength = null,
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            interval = "300",
            displayName = "...r@microsoft.com",
            displayType = NativeAuthDisplayType.EMAIL
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingBindingMethod() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = NativeAuthChallengeType.OOB,
            codeLength = 6,
            bindingMethod = null,
            interval = "300",
            displayName = "...r@microsoft.com",
            displayType = NativeAuthDisplayType.EMAIL
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingInterval() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = NativeAuthChallengeType.OOB,
            codeLength = 6,
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            interval = null,
            displayName = "...r@microsoft.com",
            displayType = NativeAuthDisplayType.EMAIL
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
        )
    }

    @Test
    fun testValidateSignUpChallengeResultWithSuccessAndMissingDisplayName() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(true)
        val signUpChallengeResponse = SignUpChallengeResponse(
            signupToken = "1234",
            challengeType = NativeAuthChallengeType.OOB,
            codeLength = 6,
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            interval = "300",
            displayName = null,
            displayType = NativeAuthDisplayType.EMAIL
        )
        whenever(signUpChallengeResult.successResponse).thenReturn(signUpChallengeResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
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
            apiResult = signUpChallengeResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignUpChallengeResultWithErrorAndMissingObject() {
        val signUpChallengeResult = mock<SignUpChallengeResult>()
        whenever(signUpChallengeResult.success).thenReturn(false)
        whenever(signUpChallengeResult.errorResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpChallengeResult
        )
    }

    @Test
    fun testValidateSignUpContinueResultWithAttributedRequiredErrorAndAttribtues() {
        val signUpContinueResult = mock<SignUpContinueResult>()
        whenever(signUpContinueResult.success).thenReturn(false)
        val signUpContinueErrorResponse = SignUpContinueErrorResponse(
            errorCode = SignUpContinueErrorCodes.ATTRIBUTES_REQUIRED,
            statusCode = 400,
            errorDescription = null,
            signupToken = null,
            verifyAttributes = null,
            invalidAttributes = userAttributes.toString(),
            requiredAttributes = null
        )
        whenever(signUpContinueResult.errorResponse).thenReturn(signUpContinueErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpContinueResult
        )
    }

    // validate SsprStartResult
    @Test
    fun testValidateSsprStartResultWithSuccessReturnChallengeType() {
        val ssprStartResult = mock<SsprStartResult>()
        whenever(ssprStartResult.success).thenReturn(true)

        val ssprStartResponse = SsprStartResponse(
            challengeType = NativeAuthChallengeType.REDIRECT,
            passwordResetToken = null
        )
        whenever(ssprStartResult.successResponse).thenReturn(ssprStartResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprStartResult
        )
    }

    @Test
    fun testValidateSsprStartResultWithSuccessReturnPasswordResetToken() {
        val ssprStartResult = mock<SsprStartResult>()
        whenever(ssprStartResult.success).thenReturn(true)

        val ssprStartResponse = SsprStartResponse(
            passwordResetToken = "123456",
            challengeType = null
        )
        whenever(ssprStartResult.successResponse).thenReturn(ssprStartResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprStartResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprStartResultWithSuccessAndMissingObject() {
        val ssprStartResult = mock<SsprStartResult>()
        whenever(ssprStartResult.success).thenReturn(true)
        whenever(ssprStartResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprStartResult
        )
    }

    // validate SsprChallengeResult
    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultWithSuccessAndMissingObject() {
        val ssprChallengeResult = mock<SsprChallengeResult>()
        whenever(ssprChallengeResult.success).thenReturn(true)
        whenever(ssprChallengeResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprChallengeResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultWithSuccessAndMissingPasswordResetToken() {
        val ssprChallengeResult = mock<SsprChallengeResult>()
        whenever(ssprChallengeResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprChallengeResponse(
            passwordResetToken = emptyString,
            challengeType = NativeAuthChallengeType.OOB,
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            displayName = null,
            displayType = null,
            codeLength = null
        )
        whenever(ssprChallengeResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprChallengeResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultWithSuccessAndMissingChallengeType() {
        val ssprChallengeResult = mock<SsprChallengeResult>()
        whenever(ssprChallengeResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprChallengeResponse(
            passwordResetToken = "1234",
            challengeType = NativeAuthChallengeType.UNKNOWN,
            bindingMethod = NativeAuthBindingMethod.PROMPT,
            displayName = null,
            displayType = null,
            codeLength = null
        )
        whenever(ssprChallengeResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprChallengeResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultWithSuccessAndMissingBindingMethod() {
        val ssprChallengeResult = mock<SsprChallengeResult>()
        whenever(ssprChallengeResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprChallengeResponse(
            passwordResetToken = "1234",
            challengeType = NativeAuthChallengeType.OOB,
            bindingMethod = NativeAuthBindingMethod.UNKNOWN,
            codeLength = null,
            displayName = null,
            displayType = null
        )
        whenever(ssprChallengeResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprChallengeResult
        )
    }

    // validate SsprContinueResult
    @Test(expected = ClientException::class)
    fun testValidateSsprContinueResultWithSuccessAndMissingObject() {
        val ssprContinueResult = mock<SsprContinueResult>()
        whenever(ssprContinueResult.success).thenReturn(true)
        whenever(ssprContinueResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprContinueResult
        )
    }

    @Test
    fun testValidateSsprContinueResultSuccessReturnToken() {
        val ssprContinueResult = mock<SsprContinueResult>()
        whenever(ssprContinueResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprContinueResponse(
            passwordSubmitToken = "1234",
            expiresIn = 600,
            error = null
        )
        whenever(ssprContinueResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprContinueResult
        )
    }

    @Test
    fun testValidateSsprContinueResultSuccessReturn() {
        val ssprContinueResult = mock<SsprContinueResult>()
        whenever(ssprContinueResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprContinueResponse(
            passwordSubmitToken = "1234",
            error = "verification_required",
            expiresIn = 600
        )
        whenever(ssprContinueResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprContinueResult
        )
    }

    // validate SsprSubmitResult
    @Test(expected = ClientException::class)
    fun testValidateSsprSubmitResultWithSuccessAndMissingObject() {
        val ssprSubmitResult = mock<SsprSubmitResult>()
        whenever(ssprSubmitResult.success).thenReturn(true)
        whenever(ssprSubmitResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprSubmitResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprSubmitResultWithSuccessAndMissingPasswordResetToken() {
        val ssprSubmitResult = mock<SsprSubmitResult>()
        whenever(ssprSubmitResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprSubmitResponse(
            passwordResetToken = "",
            pollInterval = 2
        )
        whenever(ssprSubmitResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprSubmitResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprSubmitResultWithSuccessAndMissingPollInterval() {
        val ssprSubmitResult = mock<SsprSubmitResult>()
        whenever(ssprSubmitResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprSubmitResponse(
            passwordResetToken = "1234",
            pollInterval = 0
        )
        whenever(ssprSubmitResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprSubmitResult
        )
    }

    @Test
    fun testValidateSsprSubmitResultSuccess() {
        val ssprSubmitResult = mock<SsprSubmitResult>()
        whenever(ssprSubmitResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprSubmitResponse(
            passwordResetToken = "1234",
            pollInterval = 2
        )
        whenever(ssprSubmitResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprSubmitResult
        )
    }

    // validate SsprPollCompletionResult
    @Test
    fun testValidateSsprPollCompletionResultSucceeded() {
        val ssprPollCompletionResult = mock<SsprPollCompletionResult>()
        whenever(ssprPollCompletionResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprPollCompletionResponse(
            status = NativeAuthPollCompletionStatus.SUCCEEDED
        )
        whenever(ssprPollCompletionResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprPollCompletionResult
        )
    }

    @Test
    fun testValidateSsprPollCompletionResultInProgress() {
        val ssprPollCompletionResult = mock<SsprPollCompletionResult>()
        whenever(ssprPollCompletionResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprPollCompletionResponse(
            status = NativeAuthPollCompletionStatus.IN_PROGRESS
        )
        whenever(ssprPollCompletionResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprPollCompletionResult
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprPollCompletionResultWithSuccessAndMissingStatus() {
        val ssprPollCompletionResult = mock<SsprPollCompletionResult>()
        whenever(ssprPollCompletionResult.success).thenReturn(true)

        val ssprResultSuccessResponse = SsprPollCompletionResponse(
            status = NativeAuthPollCompletionStatus.UNKNOWN
        )
        whenever(ssprPollCompletionResult.successResponse).thenReturn(ssprResultSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = ssprPollCompletionResult
        )
    }

    @Test
    fun testSignInInitiateApiResponseWithRedirectChallenge() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = 200,
            challengeType = "redirect",
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertEquals(SignInInitiateApiResult.Redirect, apiResult)
    }

    @Test
    fun testSignInInitiateApiResponseWithSuccess() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = 200,
            challengeType = null,
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.Success)
        assertEquals(credentialToken, (apiResult as SignInInitiateApiResult.Success).credentialToken)
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateApiResponseWithMissingCredentialToken() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = 200,
            challengeType = null,
            credentialToken = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateApiResponseInvalidGrantWithMissingErrorCodes() {
        val error = "invalid_grant"
        val errorDescription = "This user is not found"
        val errorCodes = listOf(50034)

        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = 200,
            challengeType = null,
            credentialToken = null,
            error = error,
            errorCodes = errorCodes,
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UserNotFound)
        assertEquals(error, (apiResult as SignInInitiateApiResult.UserNotFound).error)
        assertEquals(errorDescription, (apiResult as SignInInitiateApiResult.UserNotFound).errorDescription)
    }

    @Test
    fun testSignInInitiateApiResponseWithUnknownError() {
        val error = "unknown_error"
        val errorDescription = "An unknown error happened"
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = 400,
            challengeType = null,
            credentialToken = null,
            error = error,
            errorCodes = listOf(1234),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInInitiateApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInInitiateApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeInvalidGrant() {
        val error = "invalid_grant"
        val errorDescription = "Tenant misconfiguration"
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 400,
            challengeType = "oob",
            credentialToken = credentialToken,
            error = error,
            errorCodes = null,
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            bindingMethod = "prompt",
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInChallengeApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInChallengeApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobSuccess() {
        val challengeTargetLabel = "user@contoso.com"
        val challengeChannel = "email"
        val codeLength = 6

        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "oob",
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = "prompt",
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = challengeChannel,
            codeLength = codeLength,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.OOBRequired)
        assertEquals(credentialToken, (apiResult as SignInChallengeApiResult.OOBRequired).credentialToken)
        assertEquals(challengeTargetLabel, (apiResult as SignInChallengeApiResult.OOBRequired).challengeTargetLabel)
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingCodeLength() {
        val challengeTargetLabel = "user@contoso.com"
        val challengeChannel = "email"
        val codeLength = null

        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "oob",
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = "prompt",
            challengeTargetLabel = null,
            challengeChannel = challengeChannel,
            codeLength = codeLength,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeChannel() {
        val challengeTargetLabel = "user@contoso.com"
        val challengeChannel = null
        val codeLength = 6

        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "oob",
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = "prompt",
            challengeTargetLabel = null,
            challengeChannel = challengeChannel,
            codeLength = codeLength,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeTargetLabel() {
        val challengeTargetLabel = null
        val challengeChannel = "email"
        val codeLength = 6

        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "oob",
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = "prompt",
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = challengeChannel,
            codeLength = codeLength,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypePasswordWithMissingCredentialToken() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "password",
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypePassword() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 200,
            challengeType = "password",
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.PasswordRequired)
        assertEquals(credentialToken, (apiResult as SignInChallengeApiResult.PasswordRequired).credentialToken)
    }

    @Test
    fun testSignInChallengeApiResponseWithUnknownError() {
        val error = "unknown_error"
        val errorDescription = "An unknown error happened"
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = 400,
            challengeType = null,
            credentialToken = null,
            error = error,
            errorCodes = listOf(1234),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInChallengeApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInChallengeApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseInvalidGrantMissingErrorCodes() {
        val error = "invalid_grant"
        val errorDescription = "Tenant misconfiguration"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = null,
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseUserDoesNotExist() {
        val error = "invalid_grant"
        val errorCode = 50034
        val errorDescription = "User does not exist"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(errorCode),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.UserNotFound)
        assertEquals(error, (apiResult as SignInTokenApiResult.UserNotFound).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.UserNotFound).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponsePasswordIncorrect() {
        val error = "invalid_grant"
        val errorCode = 50126
        val errorDescription = "Incorrect password"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(errorCode),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.PasswordIncorrect)
        assertEquals(error, (apiResult as SignInTokenApiResult.PasswordIncorrect).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.PasswordIncorrect).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrect() {
        val error = "invalid_grant"
        val errorCode = 50181
        val errorDescription = "Incorrect OTP code"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(errorCode),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(error, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.CodeIncorrect).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseMultipleErrorCodes() {
        val error = "invalid_grant"
        val errorCode = 50181
        val errorDescription = "Incorrect OTP code"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(0, errorCode),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseCredentialRequiredSuccess() {
        val error = "credential_required"
        val errorDescription = "Credential is required by the API"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = credentialToken,
            error = error,
            errorCodes = listOf(1234),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.CredentialRequired)
        assertEquals(credentialToken, (apiResult as SignInTokenApiResult.CredentialRequired).credentialToken)
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseCredentialRequiredMissingCredentialToken() {
        val error = "credential_required"
        val errorDescription = "Credential is required by the API"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(1234),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.CredentialRequired)
    }

    @Test
    fun testSignInTokenApiResponseSuccess() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 200,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = "Bearer",
            scope = "openid profile",
            expiresIn = 3600,
            extExpiresIn = 3600,
            accessToken = "1234",
            refreshToken = "5678",
            idToken = "9012"
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.Success)
        // TODO token validation
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingAccessToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 200,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = "Bearer",
            scope = "openid profile",
            expiresIn = 3600,
            extExpiresIn = 3600,
            accessToken = null,
            refreshToken = "5678",
            idToken = "9012"
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingRefreshToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 200,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = "Bearer",
            scope = "openid profile",
            expiresIn = 3600,
            extExpiresIn = 3600,
            accessToken = "1234",
            refreshToken = null,
            idToken = "9012"
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingIdToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 200,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = "Bearer",
            scope = "openid profile",
            expiresIn = 3600,
            extExpiresIn = 3600,
            accessToken = "1234",
            refreshToken = "5678",
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
    }

    @Test
    fun testSignInTokenApiResponseWithUnknownError() {
        val error = "unknown_error"
        val errorDescription = "An unknown error happened"
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = error,
            errorCodes = listOf(1234),
            errorDescription = errorDescription,
            errorUri = null,
            innerErrors = null,
            tokenType = null,
            scope = null,
            expiresIn = null,
            extExpiresIn = null,
            accessToken = null,
            refreshToken = null,
            idToken = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(error, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(errorDescription, (apiResult as SignInTokenApiResult.UnknownError).errorDescription)
    }
}
