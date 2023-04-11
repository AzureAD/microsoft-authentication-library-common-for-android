package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.InnerError
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthBindingMethod
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthPollCompletionStatus
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInInitiateErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInInitiateResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInInitiateSuccessResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.exceptions.ErrorCodes
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.Attribute
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.challenge.SsprChallengeResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.challenge.SsprChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.cont.SsprContinueResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.cont.SsprContinueResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.start.SsprStartResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.start.SsprStartResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.submit.SsprSubmitResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.submit.SsprSubmitResult
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
    private val challengeType = "oob password redirect"
    private val emptyString = ""

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
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
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
            errorCode = "",
            errorDescription = "error description",
            verifyAttributes = listOf(Attribute("username")),
            invalidAttributes = "invalid attributes",
            challengeType = "oob",
            signupToken = "1234"
        )
        whenever(signUpStartResult.errorResponse).thenReturn(signUpResultErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
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
            apiResult = signUpStartResult
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
            bindingMethod = "prompt",
            interval = "300",
            displayName = "...r@microsoft.com"
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
            challengeType = "oob",
            codeLength = null,
            bindingMethod = "prompt",
            interval = "300",
            displayName = "...r@microsoft.com"
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
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = null,
            interval = "300",
            displayName = "...r@microsoft.com"
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
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = "prompt",
            interval = null,
            displayName = "...r@microsoft.com"
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
            challengeType = "oob",
            codeLength = 6,
            bindingMethod = "prompt",
            interval = "300",
            displayName = null
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

    // Because I comment the validation part in response
//    @Test(expected = ClientException::class)
//    fun testValidateSsprContinueResultWithSuccessAndMissingPasswordResetToken() {
//        val ssprContinueResult = mock<SsprContinueResult>()
//        whenever(ssprContinueResult.success).thenReturn(true)
//
//        val ssprResultSuccessResponse = SsprContinueResponse(
//            passwordSubmitToken = emptyString
//        )
//        whenever(ssprContinueResult.successResponse).thenReturn(ssprResultSuccessResponse)
//
//        nativeAuthResponseHandler.validateApiResult(
//            apiResult =  ssprContinueResult
//        )
//    }

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

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithSuccessAndMissingObject() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(true)
        whenever(signInInitiateResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithSuccessAndMissingCredentialTokenAndChallengeType() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(true)
        val signInInitiateSuccessResponse = SignInInitiateSuccessResponse(
            credentialToken = null,
            challengeType = null
        )
        whenever(signInInitiateResult.successResponse).thenReturn(signInInitiateSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithSuccessAndHaveBothCredentialTokenAndChallengeType() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(true)
        val signInInitiateSuccessResponse = SignInInitiateSuccessResponse(
            credentialToken = "1234",
            challengeType = "oob"
        )
        whenever(signInInitiateResult.successResponse).thenReturn(signInInitiateSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test
    fun testValidateSignInInitiateResultWithSuccessAndNoMissingObject() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(true)
        val signInInitiateSuccessResponse = mock<SignInInitiateSuccessResponse>()
        whenever(signInInitiateSuccessResponse.credentialToken).thenReturn("1234")
        whenever(signInInitiateResult.successResponse).thenReturn(signInInitiateSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test
    fun testValidateSignInInitiateResultWithSuccessAndNoMissingObjectWithRedirect() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(true)
        val signInInitiateSuccessResponse = mock<SignInInitiateSuccessResponse>()
        whenever(signInInitiateSuccessResponse.challengeType).thenReturn("redirect")
        whenever(signInInitiateResult.successResponse).thenReturn(signInInitiateSuccessResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithErrorAndMissingObject() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(false)
        whenever(signInInitiateResult.errorResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithErrorAndMissingErrorCode() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(false)
        val signInInitiateErrorResponse = SignInInitiateErrorResponse(
            statusCode = 400,
            errorCode = null,
            errorDescription = "error description",
            errorUri = requestUrl.toString(),
            innerErrors = listOf(
                InnerError(
                    innerError = "errorCode1",
                    errorDescription = "detailed description1"
                )
            )
        )

        whenever(signInInitiateResult.errorResponse).thenReturn(signInInitiateErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInInitiateResultWithErrorAndEmptyErrorCode() {
        val signInInitiateResult = mock<SignInInitiateResult>()
        whenever(signInInitiateResult.success).thenReturn(false)
        val signInInitiateErrorResponse = SignInInitiateErrorResponse(
            statusCode = 400,
            errorCode = "",
            errorDescription = "error description",
            errorUri = requestUrl.toString(),
            innerErrors = listOf(
                InnerError(
                    innerError = "errorCode1",
                    errorDescription = "detailed description1"
                )
            )
        )

        whenever(signInInitiateResult.errorResponse).thenReturn(signInInitiateErrorResponse)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInInitiateResult,
        )
    }

    @Test(expected = ClientException::class)
    fun testValidateSignInChallengeResultWithSuccessAndMissingObject() {
        val signInChallengeResult = mock<SignInChallengeResult>()
        whenever(signInChallengeResult.success).thenReturn(true)
        whenever(signInChallengeResult.successResponse).thenReturn(null)

        nativeAuthResponseHandler.validateApiResult(
            apiResult = signInChallengeResult,
        )
    }
}
