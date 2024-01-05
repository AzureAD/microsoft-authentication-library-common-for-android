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

import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.responses.UserAttributeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.UserAttributeOptionsApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    private val invalidAttributes = listOf(mapOf("name" to "city"), mapOf("name" to "username"))
    private val unverifiedAttributes = listOf(mapOf("name" to "phone"))
    private val handler = NativeAuthResponseHandler()
    private val nullHttpResponse = HttpResponse(400, null, null)
    private val emptyHttpResponse = HttpResponse(400, "", mapOf())
    private val requiredAttributes =
        listOf(
            UserAttributeApiResult(
                "city",
                "string",
                true,
                UserAttributeOptionsApiResult(
                    "someregex"
                )
            ),
            UserAttributeApiResult(
                "surname",
                "string",
                true,
                null
            )
        )
    private val credentialToken = "uY29tL2F1dGhlbnRpY"
    private val signupToken = "token123"
    private val passwordResetToken = "1234"
    private val passwordSubmitToken = "1234"
    private val invalidGrantError = "invalid_grant"
    private val invalidOOBValueError = "invalid_oob_value"
    private val invalidRequestError = "invalid_request"
    private val unsupportedChallengeTypeError = "unsupported_challenge_type"
    private val expiredTokenError = "expired_token"
    private val userNotFoundError = "user_not_found"
    private val userAlreadyExistsError = "user_already_exists"
    private val invalidErrorCode = 0
    private val invalidParameterErrorCode = 90100
    private val userNotFoundErrorCode = 50034
    private val errorStatusCode = 400
    private val successStatusCode = 200
    private val uncommonErrorStatusCode = 477
    private val expiresIn400 = 400
    private val expiresIn3600 = 3600L
    private val codeLength = 6
    private val pollInterval = 5
    private val interval = 300
    private val incorrectOOBErrorCode1 = 50181
    private val incorrectOOBErrorCode2 = 50184
    private val incorrectOOBErrorCode3 = 501811
    private val incorrectPasswordErrorCode = 50126
    private val mfaRequiredErrorCode = 50076
    private val randomErrorCode = 0
    private val errorDescription = "User not found"
    private val signInUnknownError = "unknown_error"
    private val redirect = "redirect"
    private val verificationRequiredErrorCode = "verification_required"
    private val authNotSupportedErrorCode = "unsupported_auth_method"
    private val invalidAttributesErrorCode = "invalid_attributes"
    private val passwordBasedAuthNotSupported = "Password based authentication is not supported."
    private val attributesValidationFailed = "Attributes validation failed."
    private val challengeTargetLabel = "tester@contoso.com"
    private val emailChallengeChannel = "email"
    private val bindingMethod = "prompt"
    private val nullString = "null"
    private val attributeValidationFailed = "attribute_validation_failed"
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
    private val unauthorizedClientError = "unauthorized_client"
    private val invalidClientError = "invalid_client"
    private val credentialRequiredError = "credential_required"
    private val tenantMisconfiguration = "Tenant misconfiguration"
    private val unknownErrorDescription = "An unknown error happened"
    private val userAttributesRequiredErrorDescription = "User attributes required"
    private val invalidAttributesErrorDescription = "Invalid user attributes"
    private val credentialRequiredErrorDescription = "Credential required."
    private val userDoesNotExistErrorDescription = "User does not exist"
    private val incorrectPasswordDescription = "Incorrect password"
    private val incorrectOtpDescription = "Incorrect OTP code"
    private val credentialRequiredTokenErrorDescription = "Credential is required by the API"
    private val mfaRequiredTokenErrorDescription = "MFA is required by the API"
    private val tokenType = "Bearer"
    private val scope = "openid profile"
    private val refreshToken = "5678"
    private val signInSLT = "12345"
    private val expiresIn = 500
    private val idToken = "9012"
    private val accessToken = "1234"
    private val invalidAuthenticationTypeErrorCode = 400002
    private val attributeValidationFailedErrorCode = "attribute_validation_failed"

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns requestUrl
        every { challengeType } returns this@NativeAuthResponseHandlerTest.challengeType
        every { clientId } returns this@NativeAuthResponseHandlerTest.clientId
        every { useMockApiForNativeAuth  } returns true
    }

    private val nativeAuthResponseHandler = NativeAuthResponseHandler()

    // region SignUp Start
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

    @Test
    fun testSignUpStartApiResponseUnknownErrorWithUnauthorizedClientError() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = unauthorizedClientError,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = userAttributes,
            invalidAttributes = null,
            details = null
        )

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnknownError)
    }

    @Test
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

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnknownError)
    }

    @Test
    fun testSignUpStartApiResponseVerificationRequiredWithMissingUnverifiedAttributes() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = signupToken,
            error = null,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnknownError)
    }

    @Test
    fun testSignUpStartApiResponseUserAlreadyExists() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = userAlreadyExistsError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UsernameAlreadyExists)
    }

    @Test
    fun testSignUpStartApiResponseInvalidEmail() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = null,
            errorCodes = listOf(invalidParameterErrorCode),
            errorDescription = "username parameter is empty or not valid.",
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidEmail)
    }

    @Test
    fun testSignUpStartApiResponseInvalidParameter() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = null,
            errorCodes = listOf(invalidParameterErrorCode),
            errorDescription = "client_id parameter is empty or not valid.",
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnknownError)
    }

    @Test
    fun testSignUpStartApiResponseUnsupportedChallengeType() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = unsupportedChallengeTypeError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnsupportedChallengeType)
    }

    @Test
    fun testSignUpStartApiResponsePasswordTooWeak() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = passwordTooWeakError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpStartApiResponsePasswordTooLong() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = passwordTooLongError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpStartApiResponsePasswordTooShort() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = passwordTooShortError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpStartApiResponsePasswordBanned() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = passwordBannedError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpStartApiResponsePasswordRecentlyUsed() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = passwordRecentlyUsedError,
            errorCodes = null,
            errorDescription = passwordBasedAuthNotSupported,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )
        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.InvalidPassword)
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
            error = authNotSupportedErrorCode,
            errorCodes = null,
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
    fun testSignUpStartApiResponseUncommonErrorStatusCode() {
        val signUpStartApiResponse = SignUpStartApiResponse(
            statusCode = uncommonErrorStatusCode,
            challengeType = null,
            signupToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            details = null
        )

        val apiResult = signUpStartApiResponse.toResult()
        assertTrue(apiResult is SignUpStartApiResult.UnknownError)
    }
    // endregion

    // region SignUp Challenge
    @Test
    fun testSignUpChallengeApiResponseUnsupportedChallengeType() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = unsupportedChallengeTypeError,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.UnsupportedChallengeType)
    }

    @Test
    fun testSignUpChallengeApiResponseExpiredToken() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            signupToken = null,
            error = expiredTokenError,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.ExpiredToken)
    }

    @Test
    fun testSignUpChallengeApiResponseOOBRequired() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
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

    @Test
    fun testSignUpChallengeApiResponseOOBRequiredWithCodeLengthMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = emailChallengeChannel,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )

        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignUpChallengeApiResponseOOBRequiredWithChallengeTargetLabelMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = emailChallengeChannel,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignUpChallengeApiResponseOOBRequiredWithChallengeChannelMissing() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = signupToken,
            error = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = null,
            bindingMethod = bindingMethod,
            interval = interval,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignUpChallengeApiResponsePasswordRequired() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = passwordChallengeType,
            signupToken = signupToken,
            error = null,
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
    fun testSignUpChallengeApiResponseRedirect() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = redirect,
            signupToken = signupToken,
            error = null,
            errorDescription = null,
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            bindingMethod = null,
            interval = null,
            details = null
        )
        val apiResult = signUpChallengeApiResponse.toResult()
        assertTrue(apiResult is SignUpChallengeApiResult.Redirect)
    }

    @Test
    fun testSignUpChallengeApiResponseIncorrectChallengeType() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = invalidChallengeType,
            signupToken = signupToken,
            error = null,
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

    @Test
    fun testSignUpChallengeApiResponseWithNoSignupToken() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = oobChallengeType,
            signupToken = null,
            error = null,
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

    @Test
    fun testSignUpChallengeApiResponsePasswordWithNoSignupToken() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = successStatusCode,
            challengeType = passwordChallengeType,
            signupToken = null,
            error = null,
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

    @Test
    fun testSignUpChallengeApiResponseUncommonStatusCode() {
        val signUpChallengeApiResponse = SignUpChallengeApiResponse(
            statusCode = uncommonErrorStatusCode,
            challengeType = null,
            signupToken = null,
            error = null,
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
    // endregion

    // region SignUp Continue
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

    @Test
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

    @Test
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
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
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
    fun testSignUpContinueApiPasswordTooWeak() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = passwordTooWeakError,
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
        assertTrue(apiResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpContinueApiPasswordTooLong() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = passwordTooLongError,
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
        assertTrue(apiResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpContinueApiPasswordTooShort() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = passwordTooShortError,
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
        assertTrue(apiResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpContinueApiPasswordBanned() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = passwordBannedError,
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
        assertTrue(apiResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpContinueApiPasswordRecentlyUsed() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = passwordRecentlyUsedError,
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
        assertTrue(apiResult is SignUpContinueApiResult.InvalidPassword)
    }

    @Test
    fun testSignUpContinueApiUserAlreadyExists() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = userAlreadyExistsError,
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
        assertTrue(apiResult is SignUpContinueApiResult.UsernameAlreadyExists)
    }

    @Test
    fun testSignUpContinueApiInvalidOOBWithErrorCode1() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode1),
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.InvalidOOBValue)
    }

    @Test
    fun testSignUpContinueApiInvalidOOBWithErrorCode2() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode2),
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.InvalidOOBValue)
    }

    @Test
    fun testSignUpContinueApiInvalidOOBWithErrorCode3() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode3),
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = null,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.InvalidOOBValue)
    }

    @Test
    fun testSignUpContinueApiInvalidRequestNullErrorCode() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = invalidRequestError,
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
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
    }

    @Test
    fun testSignUpContinueApiInvalidRequestInvalidErrorCode() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = invalidRequestError,
            errorCodes = listOf(invalidErrorCode),
            signInSLT = null,
            errorDescription = null,
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
    fun testSignUpContinueApiExpiredToken() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = expiredTokenError,
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
        assertTrue(apiResult is SignUpContinueApiResult.ExpiredToken)
    }

    @Test
    fun testSignUpContinueApiAttributeValidationFailed() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = attributeValidationFailedErrorCode,
            errorCodes = null,
            signInSLT = null,
            errorDescription = null,
            unverifiedAttributes = null,
            invalidAttributes = userAttributes,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.InvalidAttributes)
    }

    @Test
    fun testSignUpContinueApiAttributeValidationFailedMissingInvalidAttributes() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = attributeValidationFailedErrorCode,
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
    fun testSignUpContinueApiUserInvalidAttributesResponse() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = signupToken,
            error = attributeValidationFailed,
            errorCodes = null,
            signInSLT = null,
            errorDescription = invalidAttributesErrorDescription,
            unverifiedAttributes = null,
            invalidAttributes = invalidAttributes,
            expiresIn = null,
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.InvalidAttributes)
        assertTrue((apiResult as SignUpContinueApiResult.InvalidAttributes).invalidAttributes.containsAll(listOf("username", "city")))
    }

    @Test
    fun testSignUpContinueApiUserAttributesRequiredResponseNoFlowToken() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = null,
            error = attributeValidationFailed,
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
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
    }

    @Test
    fun testSignUpContinueApiUserAttributesRequiredResponseNoRequiredAttributes() {
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
            requiredAttributes = null,
            details = null
        )
        val apiResult = signUpContinueApiResponse.toResult()
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
    }

    @Test
    fun testSignUpContinueApiAuthenticationRequiredResponse() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = errorStatusCode,
            signupToken = signupToken,
            error = credentialRequiredError,
            errorCodes = null,
            signInSLT = null,
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

    @Test
    fun testSignUpContinueApiUncommonStatusCode() {
        val signUpContinueApiResponse = SignUpContinueApiResponse(
            statusCode = uncommonErrorStatusCode,
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
        assertTrue(apiResult is SignUpContinueApiResult.UnknownError)
    }
    // endregion

    // region ResetPassword Start
    @Test
    fun testValidateSsprStartResultWithSuccessReturnChallengeType() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            challengeType = redirect,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.Success)
        assertNotNull((apiResult as ResetPasswordStartApiResult.Success).passwordResetToken)
    }

    @Test
    fun testSsprStartResultUnknownErrorWithUnauthorizedClientError() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = unauthorizedClientError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
    }


    @Test
    fun testValidateSsprStartResultWithSuccessNoRedirectButMissingToken() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UserNotFound)
    }

    @Test
    fun testValidateSsprStartResultInvalidGrant() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = invalidGrantError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprStartResultUnsupportedChallengeType() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = unsupportedChallengeTypeError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnsupportedChallengeType)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordStartApiResult.UnknownError).errorDescription)
    }

    @Test
    fun testValidateSsprStartResultUncommonStatusCode() {
        val resetPasswordStartApiResponse = ResetPasswordStartApiResponse(
            statusCode = uncommonErrorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            error = null,
            errorDescription = unknownError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordStartApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordStartApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordStartApiResult.UnknownError).errorDescription)
    }
    //endregion

    // region ResetPassword Challenge
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprChallengeResultExpiredToken() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null,
            error = expiredTokenError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.ExpiredToken)
    }

    @Test
    fun testValidateSsprChallengeResultUnsupportedChallengeType() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            challengeType = null,
            bindingMethod = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null,
            error = unsupportedChallengeTypeError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnsupportedChallengeType)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprChallengeResultUncommonStatusCode() {
        val resetPasswordChallengeApiResponse = ResetPasswordChallengeApiResponse(
            statusCode = uncommonErrorStatusCode,
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordChallengeApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordChallengeApiResult.UnknownError)
    }
    // endregion

    // region ResetPassword Continue
    @Test
    fun testValidateSsprContinueResultWithSuccessWithSubmitToken() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordSubmitToken = passwordSubmitToken,
            challengeType = null,
            expiresIn = expiresIn400,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
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
            passwordSubmitToken = passwordSubmitToken,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.PasswordRequired)
        assertNotNull((apiResult as ResetPasswordContinueApiResult.PasswordRequired).passwordSubmitToken)
    }

    @Test
    fun testValidateSsprContinueResultSuccessNullSubmitToken() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = expiresIn400,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprContinueResultSuccessRedirect() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = successStatusCode,
            passwordSubmitToken = null,
            challengeType = redirect,
            expiresIn = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.Redirect)
    }

    @Test
    fun testValidateSsprContinueResultOOBIncorrect() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = invalidOOBValueError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.CodeIncorrect)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorCodes() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = invalidGrantError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprContinueResultExpiredToken() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = expiredTokenError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.ExpiredToken)
    }

    @Test
    fun testValidateSsprContinueResultNoErrorName() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = errorStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = unknownError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }

    @Test
    fun testValidateSsprContinueResultUncommonStatusCode() {
        val resetPasswordContinueApiResponse = ResetPasswordContinueApiResponse(
            statusCode = uncommonErrorStatusCode,
            passwordSubmitToken = null,
            challengeType = null,
            expiresIn = null,
            error = null,
            errorDescription = unknownError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordContinueApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordContinueApiResult.UnknownError)
    }
    // endregion

    // region ResetPassword Submit
    @Test
    fun testValidateSsprSubmitResultSuccessStartPolling() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = passwordResetToken,
            pollInterval = pollInterval,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.SubmitSuccess).passwordResetToken)
        assertNotNull(apiResult.pollInterval)
    }

    @Test
    fun testValidateSsprSubmitResultWithSuccessAndMissingPasswordResetToken() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = successStatusCode,
            passwordResetToken = null,
            pollInterval = pollInterval,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.UnknownError)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.SubmitSuccess)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.SubmitSuccess).passwordResetToken)
    }

    @Test
    fun testValidateSsprSubmitResultExpiredToken() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = errorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = expiredTokenError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.ExpiredToken)
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.UnknownError).error)
        assertNotNull(apiResult.errorDescription)
    }

    @Test
    fun testValidateSsprSubmitResultUncommonStatusCode() {
        val resetPasswordSubmitApiResponse = ResetPasswordSubmitApiResponse(
            statusCode = uncommonErrorStatusCode,
            passwordResetToken = null,
            pollInterval = null,
            error = invalidGrantError,
            errorDescription = unknownError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordSubmitApiResult.UnknownError)
        assertNotNull((apiResult as ResetPasswordSubmitApiResult.UnknownError).error)
        assertNotNull(apiResult.errorDescription)
    }
    // endregion

    // region ResetPassword PollCompletion
    @Test
    fun testValidateSsprPollCompletionResultSucceeded() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = successStatusCode,
            status = succeededStatus,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PollingFailed)
    }

    @Test
    fun testValidateSsprPollCompletionResultPasswordBanned() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = passwordBannedError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprPollCompletionResultPasswordTooShort() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = passwordTooShortError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprPollCompletionResultPasswordTooLong() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = passwordTooLongError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprPollCompletionResultPasswordRecentlyUsed() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = passwordRecentlyUsedError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprPollCompletionResultPasswordTooWeak() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = passwordTooWeakError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PasswordInvalid)
    }

    @Test
    fun testValidateSsprPollCompletionExpiredToken() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = expiredTokenError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.ExpiredToken)
    }

    @Test
    fun testValidateSsprPollCompletionExplicitUserNotFound() {
        val resetPasswordSubmitApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = errorStatusCode,
            error = userNotFoundError,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            status = null,
            signinSlt = null
        )

        val apiResult = resetPasswordSubmitApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.UserNotFound)
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
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.PollingFailed)
    }

    @Test
    fun testValidateSsprPollCompletionResultUncommonStatusCode() {
        val resetPasswordPollCompletionApiResponse = ResetPasswordPollCompletionApiResponse(
            statusCode = uncommonErrorStatusCode,
            status = null,
            signinSlt = null,
            error = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = resetPasswordPollCompletionApiResponse.toResult()
        assertTrue(apiResult is ResetPasswordPollCompletionApiResult.UnknownError)
    }
    // endregion

    // region SignIn Initiate
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
            details = null
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
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.Success)
        assertEquals(credentialToken, (apiResult as SignInInitiateApiResult.Success).credentialToken)
    }

    @Test
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
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
    }

    @Test
    fun testSignInInitiateApiResponseUnknownErrorWithUnauthorizedClientError() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = unauthorizedClientError,
            errorCodes = listOf(),
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
    }

    @Test
    fun testSignInInitiateApiResponseInvalidGrantWithMissingErrorCodes() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(),
            errorDescription = userNotFoundError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
    }

    @Test
    fun testSignInInitiateApiResponseUserNotFound() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(userNotFoundErrorCode),
            errorDescription = userNotFoundError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UserNotFound)
        assertEquals(invalidGrantError, (apiResult as SignInInitiateApiResult.UserNotFound).error)
        assertEquals(userNotFoundError, apiResult.errorDescription)
    }

    @Test
    fun testSignInInitiateApiResponseUnknownErrorCode() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = listOf(randomErrorCode),
            errorDescription = userNotFoundError,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
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
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
        assertEquals(signInUnknownError, (apiResult as SignInInitiateApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInInitiateUncommonStatusCode() {
        val signInInitiateApiResponse = SignInInitiateApiResponse(
            statusCode = uncommonErrorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInInitiateApiResult.UnknownError)
    }
    // endregion

    // region SignIn Challenge
    @Test
    fun testSignInChallengeApiResponseInvalidGrant() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = errorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = invalidGrantError,
            errorCodes = null,
            errorDescription = tenantMisconfiguration,
            errorUri = null,
            innerErrors = null,
            bindingMethod = bindingMethod,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            interval = null,
            details = null
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
            interval = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.OOBRequired)
        assertEquals(credentialToken, (apiResult as SignInChallengeApiResult.OOBRequired).credentialToken)
        assertEquals(challengeTargetLabel, apiResult.challengeTargetLabel)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobMissingCodeLength() {
        val signInChallengeApiResponse = SignInChallengeApiResponse(
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
            interval = null,
            details = null
        )

        val apiResult = signInChallengeApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeChannel() {
        val signInChallengeApiResponse = SignInChallengeApiResponse(
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
            interval = null,
            details = null
        )

        val apiResult = signInChallengeApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypeOobMissingChallengeTargetLabel() {
        val signInChallengeApiResponse = SignInChallengeApiResponse(
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
            interval = null,
            details = null
        )

        val apiResult = signInChallengeApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypePasswordWithMissingCredentialToken() {
        val signInChallengeApiResponse = SignInChallengeApiResponse(
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
            interval = null,
            details = null
        )

        val apiResult = signInChallengeApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
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
            interval = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.PasswordRequired)
        assertEquals(credentialToken, (apiResult as SignInChallengeApiResult.PasswordRequired).credentialToken)
    }

    @Test
    fun testSignInChallengeApiResponseChallengeTypePasswordMissingFlowToken() {
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
            interval = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
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
            interval = null,
            details = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
        assertEquals(signInUnknownError, (apiResult as SignInChallengeApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInChallengeUncommonStatusCode() {
        val signInInitiateApiResponse = SignInChallengeApiResponse(
            statusCode = uncommonErrorStatusCode,
            challengeType = null,
            credentialToken = null,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            codeLength = null,
            bindingMethod = null,
            interval = null
        )

        val apiResult = signInInitiateApiResponse.toResult()
        assertTrue(apiResult is SignInChallengeApiResult.UnknownError)
    }
    // endregion

    // region SignIn Token
    @Test
    fun testSignInTokenApiResponseInvalidGrantMissingErrorCodes() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = null,
            errorDescription = tenantMisconfiguration,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(tenantMisconfiguration, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseUnknownErrorWithUnauthorizedClientError() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = unauthorizedClientError,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
    }

    @Test
    fun testSignInTokenApiResponseUnknownErrorWithInvalidClientError() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidClientError,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
    }

    @Test
    fun testSignInTokenApiResponseUserNotFound() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(userNotFoundErrorCode),
            errorDescription = userDoesNotExistErrorDescription,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UserNotFound)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UserNotFound).error)
        assertEquals(userDoesNotExistErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseInvalidCredentials() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(incorrectPasswordErrorCode),
            errorDescription = incorrectPasswordDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.InvalidCredentials)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.InvalidCredentials).error)
        assertEquals(incorrectPasswordDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidGrantErrorCode1() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(incorrectOOBErrorCode1),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidRequestErrorCode1() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode1),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidRequestError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidGrantErrorCode2() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(incorrectOOBErrorCode2),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidRequestErrorCode2() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode2),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidRequestError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidGrantErrorCode3() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(incorrectOOBErrorCode3),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseOtpCodeIncorrectWithInvalidRequestErrorCode3() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidRequestError,
            errorCodes = listOf(incorrectOOBErrorCode3),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.CodeIncorrect)
        assertEquals(invalidRequestError, (apiResult as SignInTokenApiResult.CodeIncorrect).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseInvalidAuthenticationType() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(invalidAuthenticationTypeErrorCode),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.InvalidAuthenticationType)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.InvalidAuthenticationType).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseMultipleErrorCodes() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(randomErrorCode, incorrectOOBErrorCode1, incorrectOOBErrorCode2, incorrectOOBErrorCode2),
            errorDescription = incorrectOtpDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(incorrectOtpDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseSuccess() {
        val response = mock<HttpResponse>()
        whenever(response.statusCode).thenReturn(200)
        val body = "{\"token_type\":\"Bearer\",\"scope\":\"openid offline_access\",\"expires_in\":3600,\"ext_expires_in\":3600,\"access_token\":\"EwBwA8l6BAAUAOyDv0l6PcCVu89kmzvqZmkWABkAAQ27/JswzTr5JApngbbLvCRL6dxztu4d+m7p9E0LpFsRZMOsH1w7EUmFynerA2s0HGpM8aMeja90GALzXj8ZQ1L0rJ39q4UzEe1nodBHz5cSvI0SzDDi1FuD2o3zKYIDV/zELm8GnayljTShXI6ml2u7WEahlKtXVrwDe1Ek/IJepNc9EirZaxL51A/OqETGsyiYKz6/D55+OIgWcSf6oApxg6iCQpIAAzoiF91Ab2xptHKkc7lp5z/ucB09dc8Y2SWiJLLSH4pkGf3h15m3OHo3hA1C1aOTUuc/bk7hA7CEVbMX/UGF5XrKwbJtG0SO97b2+xgKdY53JzWB6zp6kggDZgAACL6Ttxs6Lc1sQAI20md0RrEgIZN2EvvGimyQrRIK3j3DDiq3fuSTWrDPrfNnK14CW+JpSuQapG+n3349kdt9OFPUd57pYcHrj5kf9kf8srdOm1D8EqKAXExlpjPFWZ32JpDFpvPFCVPil+inkAH9iSjj5d3ScWJg9mznRlZKM1NpRSAOfEXuXWE3EWXygS5Ka5PnY9uTce52FvifZ+QGU/j27ostv48XQx1rVR0elvkT+yG9VErKZiypHOp9MvK2JyzsYdgkM+v1NwEVnW2ZBSbF1zYkLPUL8MqkmwYnXm0TRGxUV0mfDi/2R4/WrzVZRCAs/+sE/QrmAK1qIrbCc4uAjTzjU2Aqb1z6twuLUFuVZuXxO/S4iucOk4kUFzSIBtA9/bIooR3uXwJLKMbw0Ghhi9xrJatOFKj8MBMohsgQSCMGjRcli3oNm++vs8XTynupjVJJON5nLb4JVfV2CMhXCQ07IIxW0CsA4W+eJj2gPan6mIn91iPQcQrtD4rNcwBH8Kcj14j+8UtnpNAYy1h8oOtmxFbckzEo01fGZ3KEYBTE4+bvW05k/ujj1Ot+k8lj6GUBFY23aD0qB5fv6fsWxiyPvRYyzXBZaHs7dq1xvR8zGHQ6eBp4gqlrZTQSCF/v1D//WAtlAJX+XToAETfD2P/lVhNunpXn/bKV3pBoKaRkVlHHk0/nJGboCPGv1VhrPzmXSBq6bm/mOntxpRHr8QUwHH1vtwFXnS0bhwjF1P+wi6c8GmW/v+2tFmGDofYYRqUgWnUGgE6LAg==\",\"refresh_token\":\"M.C106_BL2.-CWTREwEuljWTTz!UQXU!SujJppWGaLzL*qBnGRB!ldp3lvNQcImJG8Y8P5YlpsWxF56F4GU5NE5efKpZ0leEC2H2pvOqK4OLj2cc81JVE*O8IbHJYyPLPVX9HQg5TLW*MFFSxVnLMJvux6umg9NdLqdkIgUzAyhi*qBJTNC!0*I4jRsPbCJkMr6EVThw8GlP*MGX0bJZeJWcNEGxJBMyzzGXnS!PGVCNOyM9HL8xVaM4WX*NjcA!gFee1PCnAauNrYtYv!PnvorihhyCvRMw8q5cIvvPrlyWxaaoLN0!mX!cWnfhyJRsA1lR8vw3EOAaOKEs*2vaCN!tSwk*AUQVybLw7kEVAuiCFgY28C!8cIFk\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImJXOFpjTWpCQ25KWlMtaWJYNVVRRE5TdHZ4NCJ9.eyJ2ZXIiOiIyLjAiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTE4ODA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkL3YyLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFQV0t1dkFxNDdlZmxzSjdNd2dpbWtVIiwiYXVkIjoiMDk4NGE3YjYtYmMxMy00MTQxLThiMGQtOGY3NjdlMTM2YmI3IiwiZXhwIjoxNjgxNDYzMDIzLCJpYXQiOjE2ODEzNzYzMjMsIm5iZiI6MTY4MTM3NjMyMywibmFtZSI6IlNhbW15IE9kZW5ob3ZlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNhbW15Lm9kZW5ob3ZlbkBnbWFpbC5jb20iLCJvaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNDQ3Yi0yNzNlZWMwMGRkNTciLCJ0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQiLCJhaW8iOiJEVGhGY3dSdFgwT0tqNXBTSEdOZUdVR1NVNGhaNFJoNU83TmhnUjYzMnpldEM5WmgzM3dWRypXeUJqIVFPM0twU0dXRVRla25sMDA1WE8qQWg0bXhRamVuR2VRZXIqakx3Nypkcmh1cDdTc0NJRThraUlsempYMDZuaWNWNFFFTGZxR3BoYkRuemI0RWtOZEZXTHBOTmhJJCJ9.WRe3tNCsvIuYfw8bIY1D8spFJXg-ZrGm2MiDYkUlfNR-bbW_7niJg372U-wG65OfRA99NauR511IKWcg6i5FRzx3Xcx4AfGCJhOCGagD4fRDaU4I1pE-C3lJlGY6bIodTSXIlS0VUPw_YmvzQ-X9lyJP-l-89hxQNtvCSbdm2zlSJPvdynJmRH58s4PTJSGuv7zn5Jq-Uc0s2DZx0nLfBfLee8bQpaUQamaxQ6Noz7zAjz7-TkCRriqZyvJLE9dBvRd6uSzYR_qm4VDpsH5wnGsMRvW7F_hcjjZo2gZyxI6BWy0kONF8juL6H1ar1EMi3Xn9jIU1Tde3yafjTpkmyw\",\"client_info\":\"eyJ2ZXIiOiIxLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFBcEhXMjhma09DSE9xNlJZX2l2V0tZIiwibmFtZSI6IlNpbHZpdSBQZXRyZXNjdSIsInByZWZlcnJlZF91c2VybmFtZSI6InNwZXRyZXNjdW1zQG91dGxvb2suY29tIiwib2lkIjoiMDAwMDAwMDAtMDAwMC0wMDAwLTc5MzYtYWEzMjBhMmNmN2JiIiwidGlkIjoiOTE4ODA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkIiwiaG9tZV9vaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNzkzNi1hYTMyMGEyY2Y3YmIiLCJ1aWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNzkzNi1hYTMyMGEyY2Y3YmIiLCJ1dGlkIjoiOTE4ODA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkIn0\"}"
        whenever(response.body).thenReturn(body)

        val result = nativeAuthResponseHandler.getSignInTokenApiResultFromHttpResponse(
            response
        )
        assertTrue(result is SignInTokenApiResult.Success)
    }

    @Test
    fun testSignInTokenApiResponseWithUnknownError() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = unknownError,
            errorCodes = listOf(unknownErrorCode),
            errorDescription = unknownErrorDescription,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
        assertEquals(unknownError, (apiResult as SignInTokenApiResult.UnknownError).error)
        assertEquals(unknownErrorDescription, apiResult.errorDescription)
    }

    @Test
    fun testSignInTokenApiResponseCredentialRequiredExplicitError() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = uncommonErrorStatusCode,
            error = credentialRequiredError,
            errorCodes = null,
            errorDescription = unknownErrorDescription,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
    }

    @Test
    fun testSignInTokenApiResponseMFARequired() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = errorStatusCode,
            error = invalidGrantError,
            errorCodes = listOf(mfaRequiredErrorCode),
            errorDescription = mfaRequiredTokenErrorDescription,
            errorUri = null,
            innerErrors = null,
            details = null,
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.MFARequired)
        assertEquals(invalidGrantError, (apiResult as SignInTokenApiResult.MFARequired).error)
        assertEquals(mfaRequiredTokenErrorDescription, apiResult.errorDescription)
        assertEquals(listOf(mfaRequiredErrorCode), apiResult.errorCodes)
    }

    @Test
    fun testSignInTokenApiResponseUnknownError() {
        val signInTokenApiResponse = SignInTokenApiResponse(
            statusCode = uncommonErrorStatusCode,
            error = null,
            errorCodes = null,
            errorDescription = null,
            errorUri = null,
            innerErrors = null,
            details = null
        )

        val apiResult = signInTokenApiResponse.toErrorResult()
        assertTrue(apiResult is SignInTokenApiResult.UnknownError)
    }

    @Test
    fun testSignUpStartNullResponse() {
        val response = handler.getSignUpStartResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signupToken)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpStartEmptyResponse() {
        val response = handler.getSignUpStartResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signupToken)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpChallengeNullResponse() {
        val response = handler.getSignUpChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.codeLength)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.interval)
        Assert.assertNull(response.signupToken)
    }

    @Test
    fun testSignUpChallengeEmptyResponse() {
        val response = handler.getSignUpChallengeResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.codeLength)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.interval)
        Assert.assertNull(response.signupToken)
    }

    @Test
    fun testSignUpContinueNullResponse() {
        val response = handler.getSignUpContinueResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signInSLT)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.details)
        Assert.assertNull(response.expiresIn)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.requiredAttributes)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpContinueEmptyResponse() {
        val response = handler.getSignUpContinueResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signInSLT)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.details)
        Assert.assertNull(response.expiresIn)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.requiredAttributes)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignInInitiateNullResponse() {
        val response = handler.getSignInInitiateResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testSignInInitiateEmptyResponse() {
        val response = handler.getSignInInitiateResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testSignInChallengeNullResponse() {
        val response = handler.getSignInChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testSignInChallengeEmptyResponse() {
        val response = handler.getSignInChallengeResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testSignInTokenNullResponse() {
        val response = handler.getSignInTokenApiResultFromHttpResponse(nullHttpResponse)

        Assert.assertNotNull(response)
    }

    @Test
    fun testSignInTokenEmptyResponse() {
        val response = handler.getSignInTokenApiResultFromHttpResponse(emptyHttpResponse)

        Assert.assertNotNull(response)
    }

    @Test
    fun testResetPasswordStartNullResponse() {
        val response = handler.getResetPasswordChallengeApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testResetPasswordStartEmptyResponse() {
        val response = handler.getResetPasswordChallengeApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testResetPasswordChallengeNullResponse() {
        val response = handler.getResetPasswordStartApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordChallengeEmptyResponse() {
        val response = handler.getResetPasswordStartApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordContinueNullResponse() {
        val response = handler.getResetPasswordContinueApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordSubmitToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.expiresIn)
    }

    @Test
    fun testResetPasswordContinueEmptyResponse() {
        val response = handler.getResetPasswordContinueApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordSubmitToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.expiresIn)
    }

    @Test
    fun testResetPasswordSubmitNullResponse() {
        val response = handler.getResetPasswordSubmitApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordSubmitEmptyResponse() {
        val response = handler.getResetPasswordSubmitApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordPollNullResponse() {
        val response = handler.getResetPasswordPollCompletionApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordPollEmptyResponse() {
        val response = handler.getResetPasswordPollCompletionApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }
    // endregion
}
