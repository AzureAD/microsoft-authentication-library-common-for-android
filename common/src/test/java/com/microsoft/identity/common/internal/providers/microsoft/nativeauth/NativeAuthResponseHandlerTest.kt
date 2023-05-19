//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthBindingMethod
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthDisplayType
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
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResult
import org.junit.Assert
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
    private val userAttributes = mapOf(Pair("city", "Dublin"))
    private val credentialToken = "uY29tL2F1dGhlbnRpY"

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
    fun testValidateSsprStartResultWithSuccessRedirectChallengeType() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            challengeType = "redirect",
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprStartApiResponse.toResult()
        assertTrue(apiResult is SsprStartApiResult.Redirect)
    }

    @Test
    fun testValidateSsprStartResultWithSuccessReturnPasswordResetToken() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprStartApiResponse.toResult()
        assertTrue(apiResult is SsprStartApiResult.Success)
        Assert.assertNotNull((apiResult as SsprStartApiResult.Success).passwordResetToken)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprStartResultWithSuccessNoRedirectButMissingToken() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprStartApiResponse.toResult()
    }

    @Test
    fun testValidateSsprStartResultUserNotFound() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            challengeType = null,
            error = "invalid_grant",
            errorDescription = null,
            errorUri = null,
            errorCodes = listOf(50034),
            innerErrors = null,
        )

        val apiResult = ssprStartApiResponse.toResult()
        assertTrue(apiResult is SsprStartApiResult.UserNotFound)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprStartResultInvalidGrantWithNoCodes() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            challengeType = null,
            error = "invalid_grant",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprStartApiResponse.toResult()
    }

    @Test
    fun testValidateSsprStartResultUnknownError() {
        val ssprStartApiResponse = SsprStartApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = ":(",
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprStartApiResponse.toResult()
        assertTrue(apiResult is SsprStartApiResult.UnknownError)
        Assert.assertNotNull((apiResult as SsprStartApiResult.UnknownError).errorDescription)
    }

    // validate SsprChallengeResult
    @Test
    fun testValidateSsprChallengeResultSuccessWithOobChallenge() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "oob",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprChallengeApiResponse.toResult()
        assertTrue(apiResult is SsprChallengeApiResult.OOBRequired)
        Assert.assertNotNull((apiResult as SsprChallengeApiResult.OOBRequired).passwordResetToken)
        Assert.assertNotNull((apiResult as SsprChallengeApiResult.OOBRequired).challengeTargetLabel)
        Assert.assertNotNull((apiResult as SsprChallengeApiResult.OOBRequired).codeLength)
    }

    @Test
    fun testValidateSsprChallengeResultSuccessWithRedirectChallenge() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "redirect",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprChallengeApiResponse.toResult()
        assertTrue(apiResult is SsprChallengeApiResult.Redirect)
    }

    @Test
    fun testValidateSsprChallengeResultSuccessWithPasswordChallenge() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "password",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprChallengeApiResponse.toResult()
        assertTrue(apiResult is SsprChallengeApiResult.UnknownError)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingPasswordResetToken() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            challengeType = "oob",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeTargetLabel() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "oob",
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeChannel() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "oob",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = null,
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprChallengeApiResponse.toResult()
    }

    @Test
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeType() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = 4,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprChallengeApiResponse.toResult()
        assertTrue(apiResult is SsprChallengeApiResult.UnknownError)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingCodeLength() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            challengeType = "oob",
            bindingMethod = null,
            challengeTargetLabel = "label",
            challengeChannel = "channel",
            codeLength = null,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprChallengeApiResponse.toResult()
    }

    @Test
    fun testValidateSsprChallengeResultInvalidGrant() {
        val ssprChallengeApiResponse = SsprChallengeApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null,
            error = "invalid_grant",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprChallengeApiResponse.toResult()
        assertTrue(apiResult is SsprChallengeApiResult.UnknownError)
    }

    // validate SsprContinueResult
    @Test
    fun testValidateSsprContinueResultWithSuccessWithSubmitToken() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            passwordSubmitToken = "1234",
            challengeType = null,
            expiresIn = 400,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.PasswordRequired)
        Assert.assertNotNull((apiResult as SsprContinueApiResult.PasswordRequired).passwordSubmitToken)
        Assert.assertNotNull((apiResult as SsprContinueApiResult.PasswordRequired).expiresIn)
    }

    @Test
    fun testValidateSsprContinueResultSuccessNullExpiresIn() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            passwordSubmitToken = "1234",
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.PasswordRequired)
        Assert.assertNotNull((apiResult as SsprContinueApiResult.PasswordRequired).passwordSubmitToken)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprContinueResultSuccessNullSubmitToken() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = 400,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprContinueApiResponse.toResult()
    }

    @Test
    fun testValidateSsprContinueResultSuccessRedirect() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = "redirect",
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.Redirect)
    }

    @Test
    fun testValidateSsprContinueResultOOBIncorrect() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = "invalid_grant",
            errorDescription = null,
            errorUri = null,
            errorCodes = listOf(50181),
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.OOBIncorrect)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorCodes() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = "invalid_grant",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorName() {
        val ssprContinueApiResponse = SsprContinueApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = ":(",
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprContinueApiResponse.toResult()
        assertTrue(apiResult is SsprContinueApiResult.UnknownError)
    }

    // validate SsprSubmitResult
    @Test
    fun testValidateSsprSubmitResultSuccessStartPolling() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            pollInterval = 5,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.SubmitSuccess)
        Assert.assertNotNull((apiResult as SsprSubmitApiResult.SubmitSuccess).passwordResetToken)
        Assert.assertNotNull((apiResult as SsprSubmitApiResult.SubmitSuccess).pollInterval)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprSubmitResultWithSuccessAndMissingPasswordResetToken() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 200,
            passwordResetToken = null,
            pollInterval = 5,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        ssprSubmitApiResponse.toResult()
    }

    @Test
    fun testValidateSsprSubmitResultWithSuccessAndMissingPollInterval() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 200,
            passwordResetToken = "1234",
            pollInterval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.SubmitSuccess)
        Assert.assertNotNull((apiResult as SsprSubmitApiResult.SubmitSuccess).passwordResetToken)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooWeak() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "password_too_weak",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooLong() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "password_too_long",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooShort() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "password_too_short",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordBanned() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "password_banned",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordRecentlyUsed() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "password_recently_used",
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultUnknownError() {
        val ssprSubmitApiResponse = SsprSubmitApiResponse(
            statusCode = 400,
            passwordResetToken = null,
            pollInterval = null,
            error = "invalid_grant",
            errorDescription = ":(",
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprSubmitApiResponse.toResult()
        assertTrue(apiResult is SsprSubmitApiResult.UnknownError)
        Assert.assertNotNull((apiResult as SsprSubmitApiResult.UnknownError).errorCode)
        Assert.assertNotNull((apiResult as SsprSubmitApiResult.UnknownError).errorDescription)
    }

    // validate SsprPollCompletionResult
    @Test
    fun testValidateSsprPollCompletionResultSucceeded() {
        val ssprPollCompletionApiResponse = SsprPollCompletionApiResponse(
            statusCode = 200,
            status = "succeeded",
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprPollCompletionApiResponse.toResult()
        assertTrue(apiResult is SsprPollCompletionApiResult.PollingSucceeded)
    }

    @Test
    fun testValidateSsprPollCompletionResultInProgress() {
        val ssprPollCompletionApiResponse = SsprPollCompletionApiResponse(
            statusCode = 200,
            status = "in_progress",
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprPollCompletionApiResponse.toResult()
        assertTrue(apiResult is SsprPollCompletionApiResult.InProgress)
    }

    @Test
    fun testValidateSsprPollCompletionResultPollingFailed() {
        val ssprPollCompletionApiResponse = SsprPollCompletionApiResponse(
            statusCode = 200,
            status = "failed",
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprPollCompletionApiResponse.toResult()
        assertTrue(apiResult is SsprPollCompletionApiResult.PollingFailed)
    }

    @Test
    fun testValidateSsprPollCompletionResultWithSuccessAndMissingStatus() {
        val ssprPollCompletionApiResponse = SsprPollCompletionApiResponse(
            statusCode = 200,
            status = null,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = ssprPollCompletionApiResponse.toResult()
        assertTrue(apiResult is SsprPollCompletionApiResult.PollingFailed)
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

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobChannelText() {
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
            challengeChannel = "SMS",
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
