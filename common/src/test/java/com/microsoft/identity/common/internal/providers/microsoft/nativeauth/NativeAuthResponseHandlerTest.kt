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
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class NativeAuthResponseHandlerTest {
    private val clientId = "1234"
    private val requestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/start")
    private val challengeType = "oob password redirect"
    private val oobChallengeType = "oob"
    private val passwordChallengeType = "password"
    private val invalidChallengeType = "invalid_challenge_type"
    private val emptyString = ""
    private val userAttributes = listOf(mapOf("city" to "Dublin"))
    private val invalidAttributes = listOf(mapOf("name" to "city"))
    private val unverifiedAttributes = listOf(mapOf("name" to "phone"))
    private val requiredAttributes =
        listOf(mapOf("name" to "city"), mapOf("type" to "string"), mapOf("required" to "true"))
    private val credentialToken = "uY29tL2F1dGhlbnRpY"
    private val signupToken = "token123"
    private val passwordResetToken = "1234"
    private val passwordSubmitToken = "1234"
    private val invalidGrantError = "invalid_grant"
    private val userNotFoundError = "user_not_found"
    private val userNotFoundErrorCode = 50034
    private val errorStatusCode = 400
    private val successStatusCode = 200
    private val expiresIn400 = 400
    private val expiresIn3600 = 3600L
    private val codeLength = 6
    private val pollInterval = 5
    private val interval = 300
    private val incorrectOOBErrorCode = 50181
    private val invalidGrantErrorCode = 50034
    private val incorrectPasswordErrorCode = 50126
    private val errorDescription = "User not found"
    private val signInUnknownError = "unknown_error"
    private val redirect = "redirect"
    private val verificationRequiredErrorCode = "verification_required"
    private val authNotSupportedErrorCode = "auth_not_supported"
    private val invalidAttributesErrorCode = "invalid_attributes"
    private val passwordBasedAuthNotSupported = "Password based authentication is not supported."
    private val attributesValidationFailed = "Attributes validation failed."
    private val challengeTargetLabel = "tester@contoso.com"
    private val emailChallengeChannel = "email"
    private val bindingMethod = "prompt"
    private val nullString = "null"
    private val attributesRequiredError = "attributes_required"
    private val passwordTooWeakError = "password_too_weak"
    private val passwordTooLongError = "password_too_long"
    private val passwordTooShortError = "password_too_short"
    private val passwordBannedError = "password_banned"
    private val passwordRecentlyUsedError = "password_recently_used"
    private val succeededStatus = "succeeded"
    private val inProgressStatus = "in_progress"
    private val failedStatus = "failed"
    private val unknownError = ":("
    private val unknownErrorCode = 1234
    private val credentialRequiredError = "credential_required"
    private val tenantMisconfiguration = "Tenant misconfiguration"
    private val unknownErrorDescription = "An unknown error happened"
    private val userAttributesRequiredErrorDescription = "User attributes required"
    private val credentialRequiredErrorDescription = "Credential required."
    private val userDoesNotExistErrorDescription = "User does not exist"
    private val incorrectPasswordDescription = "Incorrect password"
    private val incorrectOtpDescription = "Incorrect OTP code"
    private val credentialRequiredTokenErrorDescription = "Credential is required by the API"
    private val tokenType = "Bearer"
    private val scope = "openid profile"
    private val refreshToken = "5678"
    private val signInSLT = "12345"
    private val expiresIn = 500
    private val idToken = "9012"
    private val accessToken = "1234"
    private val attributeValidationFailedErrorCode = "attribute_validation_failed"
    private val invalidOOBValueErrorCode = "invalid_oob_value"

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns requestUrl
        every { challengeType } returns this@NativeAuthResponseHandlerTest.challengeType
        every { clientId } returns this@NativeAuthResponseHandlerTest.clientId
    }

    private val nativeAuthResponseHandler = NativeAuthResponseHandler()

    @Test
    fun testSignUpStartApiResponseVerificationRequired() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = signupToken,
            error = verificationRequiredErrorCode,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = userAttributes,
            invalidAttributes = null,
            details = null
        )

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.VerificationRequired)
        assertEquals(
            signupToken,
            (apiResult as SignUpStartApiResult.VerificationRequired).signupToken
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpStartApiResponseVerificationRequiredWithMissingSignUpToken() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = userAttributes,
            invalidAttributes = null,
            details = null
        )

        signUpStartApiResponse.toResult()
    }

    @Test
    fun testSignUpStartApiResponseRedirect() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = successStatusCode,
            challengeType = redirect,
            signupToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = invalidAttributes,
            invalidAttributes = null,
            details = null
        )

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.Redirect)
    }

    @Test
    fun testSignUpStartApiResponseAuthNotSupported() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            errorCodes = null,
            error = authNotSupportedErrorCode,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.AuthNotSupported)
    }

    @Test
    fun testSignUpStartApiResponseValidationFailed() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = attributeValidationFailedErrorCode,
            errorCodes = null,
            errorDescription = attributesValidationFailed,
            unverifiedAttributes = null,
            invalidAttributes = invalidAttributes,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidAttributes)
    }

    @Test
    fun testSignUpChallengeApiResponseOOBRequired() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = codeLength,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.OOBRequired)
        assertEquals(
            signupToken,
            (apiResult as SignUpChallengeApiResult.OOBRequired).signupToken
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeApiResponseOOBRequiredWithCodeLengthMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        signUpChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeApiResponseOOBRequiredWithDisplayNameMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = emailChallengeChannel,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        signUpChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeApiResponseOOBRequiredWithDisplayTypeMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = null,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        signUpChallengeApiResponse.toResult()
    }

    @Test
    fun testSignUpChallengeApiResponsePasswordRequired() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = passwordChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.PasswordRequired)
        assertEquals(
            signupToken,
            (apiResult as SignUpChallengeApiResult.PasswordRequired).signupToken
        )
    }

    @Test
    fun testSignUpChallengeApiResponseIncorrectChallengeType() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = invalidChallengeType,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.UnknownError)
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeApiResponseWithNoSignupToken() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        signUpChallengeApiResponse.toResult()
    }

    @Test
    fun testSignUpContinueApiResponse() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = successStatusCode,
            signupToken = null,
            error = null,
            errorCodes = null,
            signInSLT = signInSLT,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = expiresIn,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.Success)
        assertEquals((apiResult as SignUpContinueApiResult.Success).signInSLT, signInSLT)
    }

    fun testSignUpContinueApiResponseNoSLT() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = successStatusCode,
            signupToken = null,
            error = null,
            errorCodes = null,
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.Success)
        assertEquals((apiResult as SignUpContinueApiResult.Success).signInSLT, null)
    }

    @Test(expected = ClientException::class)
    fun testSignUpContinueApiResponseErrorWithNoSignupToken() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = null,
            errorCodes = null,
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        signUpContinueApiResponse.toResult()
    }

    @Test
    fun testSignUpContinueApiResponseWithUnknownError() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = signupToken,
            error = nullString,
            errorCodes = null,
            signInSLT = null,
            errorDescription = nullString,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
    }

    @Test
    fun testSignUpContinueApiUserAttributesRequiredResponse() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = signupToken,
            error = attributesRequiredError,
            errorCodes = null,
            signInSLT = null,
            errorDescription = userAttributesRequiredErrorDescription,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = requiredAttributes,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.AttributesRequired)
        assertEquals(signupToken, (apiResult as SignUpContinueApiResult.AttributesRequired).signupToken)
    }

    @Test
    fun testSignUpContinueApiAuthenticationRequiredResponse() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = signupToken,
            error = credentialRequiredError,
            signInSLT = null,
            errorCodes = null,
            errorDescription = credentialRequiredErrorDescription,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.CredentialRequired)
        assertEquals(signupToken, (apiResult as SignUpContinueApiResult.CredentialRequired).signupToken)
    }

    // validate SsprStartResult
    @Test
    fun testValidateSsprStartResultWithSuccessReturnChallengeType() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            challengeType = redirect,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.Redirect)
    }

    @Test
    fun testValidateSsprStartResultWithSuccessReturnPasswordResetToken() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.Success)
        assertNotNull((apiResult as ResetPasswordStartApiResult.Success).passwordResetToken)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprStartResultWithSuccessNoRedirectButMissingToken() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordStartApiResponse.toResult()
    }

    @Test
    fun testValidateSsprStartResultUserNotFound() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = userNotFoundError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UserNotFound)
    }

    @Test
    fun testValidateSsprStartResultInvalidGrantWithNoCodes() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = invalidGrantError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprStartResultUnknownError() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = unknownError,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordStartApiResult.UnknownError).errorDescription)
    }

    // validate SsprChallengeResult
    @Test
    fun testValidateSsprChallengeResultSuccessWithOobChallenge() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = oobChallengeType,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.CodeRequired)
        assertNotNull((apiResult as ResetPasswordChallengeApiResult.CodeRequired).passwordResetToken)
        assertNotNull(apiResult.challengeTargetLabel)
        assertNotNull(apiResult.codeLength)
    }

    @Test
    fun testValidateSsprChallengeResultSuccessWithRedirectChallenge() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = redirect,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.Redirect)
    }

    @Test
    fun testValidateSsprChallengeResultSuccessWithPasswordChallenge() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
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

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingPasswordResetToken() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            challengeType = oobChallengeType,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeTargetLabel() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = oobChallengeType,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = challengeTargetLabel,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordChallengeApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeChannel() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = oobChallengeType,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = null,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordChallengeApiResponse.toResult()
    }

    @Test
    fun testValidateSsprChallengeResultSuccessAndMissingChallengeType() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprChallengeResultSuccessAndMissingCodeLength() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            challengeType = oobChallengeType,
            bindingMethod = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = null,
            interval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordChallengeApiResponse.toResult()
    }

    @Test
    fun testValidateSsprChallengeResultInvalidGrant() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null,
            error = invalidGrantError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    // validate SsprContinueResult
    @Test
    fun testValidateSsprContinueResultWithSuccessWithSubmitToken() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = passwordSubmitToken,
            challengeType = null,
            expiresIn = expiresIn400,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.PasswordRequired)
        assertNotNull((apiResult as ResetPasswordContinueApiResult.PasswordRequired).passwordSubmitToken)
        assertNotNull(apiResult.expiresIn)
    }

    @Test
    fun testValidateSsprContinueResultSuccessNullExpiresIn() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = passwordSubmitToken,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.PasswordRequired)
        assertNotNull((apiResult as ResetPasswordContinueApiResult.PasswordRequired).passwordSubmitToken)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprContinueResultSuccessNullSubmitToken() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = expiresIn400,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordContinueApiResponse.toResult()
    }

    @Test
    fun testValidateSsprContinueResultSuccessRedirect() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = redirect,
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.Redirect)
    }

    @Test
    fun testValidateSsprContinueResultOOBIncorrect() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = invalidOOBValueErrorCode,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.CodeIncorrect)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorCodes() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = invalidGrantError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorName() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = unknownError,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }

    // validate SsprSubmitResult
    @Test
    fun testValidateSsprSubmitResultSuccessStartPolling() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            pollInterval = pollInterval,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.SubmitSuccess).passwordResetToken)
        assertNotNull(apiResult.pollInterval)
    }

    @Test(expected = ClientException::class)
    fun testValidateSsprSubmitResultWithSuccessAndMissingPasswordResetToken() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            pollInterval = pollInterval,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        resetPasswordSubmitApiResponse.toResult()
    }

    @Test
    fun testValidateSsprSubmitResultWithSuccessAndMissingPollInterval() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            pollInterval = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.SubmitSuccess).passwordResetToken)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooWeak() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = passwordTooWeakError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooLong() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = passwordTooLongError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordTooShort() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = passwordTooShortError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordBanned() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = passwordBannedError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultPasswordRecentlyUsed() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = passwordRecentlyUsedError,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprSubmitResultUnknownError() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = invalidGrantError,
            errorDescription = unknownError,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.UnknownError).errorCode)
        assertNotNull(apiResult.errorDescription)
    }

    // validate SsprPollCompletionResult
    @Test
    fun testValidateSsprPollCompletionResultSucceeded() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = successStatusCode,
            status = succeededStatus,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PollingSucceeded)
    }

    @Test
    fun testValidateSsprPollCompletionResultInProgress() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = successStatusCode,
            status = inProgressStatus,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.InProgress)
    }

    @Test
    fun testValidateSsprPollCompletionResultPollingFailed() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = successStatusCode,
            status = failedStatus,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PollingFailed)
    }

    @Test
    fun testValidateSsprPollCompletionResultWithSuccessAndMissingStatus() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = successStatusCode,
            status = null,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PollingFailed)
    }

    @Test
    fun testSignInInitiateResultWithRedirectChallenge() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = successStatusCode,
            challengeType = redirect,
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
            statusCode = successStatusCode,
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
            statusCode = successStatusCode,
            challengeType = null,
            credentialToken = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            errorCodes = null,
            innerErrors = null,
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateApiResponseInvalidGrantWithMissingErrorCodes() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = successStatusCode,
            challengeType = null,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(invalidGrantErrorCode),
            errorDescription = userNotFoundError,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UserNotFound)
        assertEquals(invalidGrantError, (apiResult as SignInInitiateApiResult.UserNotFound).error)
        assertEquals(userNotFoundError, apiResult.errorDescription)
    }

    @Test
    fun testSignInInitiateApiResponseWithUnknownError() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = signInUnknownError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = unknownErrorDescription,
            errorUri = null,
            innerErrors = null,
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
        assertEquals(signInUnknownError, (apiResult as SignInInitiateApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeInvalidGrant() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = errorStatusCode,
            challengeType = oobChallengeType,
            credentialToken = credentialToken,
            error = invalidGrantError,
            errorCodes = null,
            errorDescription = tenantMisconfiguration,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
        assertEquals(invalidGrantError, (apiResult as SignInChallengeApiResult.UnknownError).error)
        assertEquals(tenantMisconfiguration, apiResult.errorDescription)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobSuccess() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.OOBRequired)
        assertEquals(credentialToken, (apiResult as SignInChallengeApiResult.OOBRequired).credentialToken)
        assertEquals(challengeTargetLabel, apiResult.challengeTargetLabel)
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingCodeLength() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = null,
            challengeChannel = emailChallengeChannel,
            codeLength = null,
            interval = null
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeChannel() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = codeLength,
            interval = null
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeTargetLabel() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            credentialToken = credentialToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = null,
            challengeChannel = emailChallengeChannel,
            codeLength = codeLength,
            interval = null
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInChallengeApiResponseChallengeTypePasswordWithMissingCredentialToken() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = passwordChallengeType,
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

        signInInitiateApiResponse.toResult()
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypePassword() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = passwordChallengeType,
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
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = signInUnknownError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = unknownErrorDescription,
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
        assertEquals(signInUnknownError, (apiResult as SignInChallengeApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseInvalidGrantMissingErrorCodes() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = null,
            errorDescription = tenantMisconfiguration,
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
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(tenantMisconfiguration, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseUserDoesNotExist() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = 400,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(invalidGrantErrorCode),
            errorDescription = userDoesNotExistErrorDescription,
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
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UserNotFound).error)
        assertEquals(userDoesNotExistErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponsePasswordIncorrect() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(incorrectPasswordErrorCode),
            errorDescription = incorrectPasswordDescription,
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
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.PasswordIncorrect).error)
        assertEquals(incorrectPasswordDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrect() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(incorrectOOBErrorCode),
            errorDescription = incorrectOtpDescription,
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
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseMultipleErrorCodes() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(0, incorrectOOBErrorCode),
            errorDescription = incorrectOtpDescription,
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
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseCredentialRequiredSuccess() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = credentialToken,
            error = credentialRequiredError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = credentialRequiredTokenErrorDescription,
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
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = credentialRequiredError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = credentialRequiredTokenErrorDescription,
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
            statusCode = successStatusCode,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = tokenType,
            scope = scope,
            expiresIn = 3600,
            extExpiresIn = 3600,
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInTokenApiResult.Success)
        // TODO token validation
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingAccessToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = successStatusCode,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = "Bearer",
            scope = "openid profile",
            expiresIn = expiresIn3600,
            extExpiresIn = expiresIn3600,
            accessToken = null,
            refreshToken = refreshToken,
            idToken = idToken
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingRefreshToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = successStatusCode,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = tokenType,
            scope = scope,
            expiresIn = expiresIn3600,
            extExpiresIn = expiresIn3600,
            accessToken = accessToken,
            refreshToken = null,
            idToken = idToken
        )

        signInInitiateApiResponse.toResult()
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenApiResponseMissingIdToken() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = successStatusCode,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            tokenType = tokenType,
            scope = scope,
            expiresIn = expiresIn3600,
            extExpiresIn = expiresIn3600,
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = null
        )

        signInInitiateApiResponse.toResult()
    }

    @Test
    fun testSignInTokenApiResponseWithUnknownError() {
        val signInInitiateApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            credentialToken = null,
            error = unknownError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = unknownErrorDescription,
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
        assertEquals(unknownError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }
}
