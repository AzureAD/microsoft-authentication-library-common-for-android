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
package com.microsoft.identity.common.nativeauth.util

import com.microsoft.identity.common.java.commands.ICommandResult.ResultStatus
import com.microsoft.identity.common.java.controllers.CommandResult
import io.mockk.mockk
import com.microsoft.identity.common.java.nativeauth.controllers.results.*
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private const val CONTINUATION_TOKEN = "1234"
private const val CORRELATION_ID = "Lkjsdf89034nsdflkjsdf"
private const val ERROR = "error_code"
private const val ERROR_DESCRIPTION = "error description"
private const val CHALLENGE_TARGET_LABEL = "user@contoso.com"
private const val CHALLENGE_TYPE = "email"
private const val CODE_LENGTH = 6
private const val EXPIRES_IN = 1000
private const val SUBERROR_INVALID_OOB = "invalid_oob_value"
private const val SUBERROR_INVALID_PASSWORD = "password_is_invalid"

/**
 * Split into multiple tests, as JUnit4 doesn't have support for @MethodSource like JUnit5 does.
 */

//region sign-up
private val signUpAttributesRequiredCommandResult = SignUpCommandResult.AttributesRequired(
    continuationToken = CONTINUATION_TOKEN,
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    requiredAttributes = emptyList(),
    correlationId = CORRELATION_ID
)

private val signUpAuthNotSupportedCommandResult = SignUpCommandResult.AuthNotSupported(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

private val signUpCodeRequiredCommandResult = SignUpCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH,
    correlationId = CORRELATION_ID
)

private val signUpCompleteCommandResult = SignUpCommandResult.Complete(
    continuationToken = null,
    expiresIn = null,
    correlationId = CORRELATION_ID
)

private val signUpInvalidAttributesCommandResult = SignUpCommandResult.InvalidAttributes(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    invalidAttributes = emptyList(),
    correlationId = CORRELATION_ID
)

private val signUpInvalidCodeCommandResult = SignUpCommandResult.InvalidCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB,
    correlationId = CORRELATION_ID
)

private val signUpInvalidPasswordCommandResult = SignUpCommandResult.InvalidPassword(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_PASSWORD,
    correlationId = CORRELATION_ID
)

private val signUpPasswordRequiredCommandResult = SignUpCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN,
    correlationId = CORRELATION_ID
)

private val signUpUsernameAlreadyExistsCommandResult = SignUpCommandResult.UsernameAlreadyExists(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

private val redirectCommandResult = INativeAuthCommandResult.Redirect(
    correlationId = CORRELATION_ID
)

private val APIErrorCommandResult = INativeAuthCommandResult.APIError(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

// SignUpStartCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignUpStartCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignUpStartCommandResults() = listOf(
            signUpAttributesRequiredCommandResult,
            signUpAuthNotSupportedCommandResult,
            signUpCodeRequiredCommandResult,
            signUpCompleteCommandResult,
            signUpInvalidAttributesCommandResult,
            signUpInvalidPasswordCommandResult,
            signUpPasswordRequiredCommandResult,
            redirectCommandResult,
            APIErrorCommandResult,
            signUpUsernameAlreadyExistsCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignUpStartCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignUpSubmitCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignUpSubmitCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun getSignUpSubmitCodeCommandResults() = listOf(
            signUpAttributesRequiredCommandResult,
            signUpCompleteCommandResult,
            signUpInvalidCodeCommandResult,
            redirectCommandResult,
            APIErrorCommandResult,
            signUpUsernameAlreadyExistsCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignUpSubmitCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignUpSubmitUserAttributesCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignUpSignUpSubmitUserAttributesCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun getSignUpSubmitUserAttributesCommandResults() = listOf(
            signUpAttributesRequiredCommandResult,
            signUpCompleteCommandResult,
            signUpInvalidAttributesCommandResult,
            redirectCommandResult,
            APIErrorCommandResult,
            signUpUsernameAlreadyExistsCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignUpSubmitUserAttributesCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignUpSubmitPasswordCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignUpSubmitPasswordCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun getSignUpSubmitPasswordCommandResults() = listOf(
            signUpAttributesRequiredCommandResult,
            signUpCompleteCommandResult,
            signUpInvalidPasswordCommandResult,
            redirectCommandResult,
            APIErrorCommandResult,
            signUpUsernameAlreadyExistsCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignUpSubmitPasswordCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignUpResendCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignUpResendCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun getSignUpResendCodeCommandResults() = listOf(
            signUpCodeRequiredCommandResult,
            redirectCommandResult,
            APIErrorCommandResult,
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignUpResendCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}
//endregion

//region sign-in
private val signInCodeRequiredCommandResult = SignInCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH,
    correlationId = CORRELATION_ID
)

private val signInCompleteCommandResult = SignInCommandResult.Complete(
    authenticationResult = mockk<ILocalAuthenticationResult>(),
    correlationId = CORRELATION_ID
)

private val signInInvalidCredentialsCommandResult = SignInCommandResult.InvalidCredentials(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList(),
    correlationId = CORRELATION_ID
)

private val signInIncorrectCodeCommandResult = SignInCommandResult.IncorrectCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList(),
    subError = SUBERROR_INVALID_OOB,
    correlationId = CORRELATION_ID
)

private val signInPasswordRequiredCommandResult = SignInCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN,
    correlationId = CORRELATION_ID
    )

private val signInUserNotFoundCommandResult = SignInCommandResult.UserNotFound(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList(),
    correlationId = CORRELATION_ID
)

// SignInStartCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInStartCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInStartCommandResults() = listOf(
            signInCodeRequiredCommandResult,
            signInCompleteCommandResult,
            signInInvalidCredentialsCommandResult,
            signInPasswordRequiredCommandResult,
            signInUserNotFoundCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInStartCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignUpStartCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInWithContinuationTokenCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInWithContinuationTokenCommandResults() = listOf(
            signInCompleteCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInWithContinuationTokenCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithContinuationTokenCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithContinuationTokenCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithContinuationTokenCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithContinuationTokenCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignInSubmitCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInSubmitCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInSubmitCodeCommandResults() = listOf(
            signInCompleteCommandResult,
            signInIncorrectCodeCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInSubmitCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignInResendCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInResendCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInResendCodeCommandResults() = listOf(
            signInCodeRequiredCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInResendCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// SignInSubmitPasswordCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInSubmitPasswordCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInSubmitPasswordCommandResults() = listOf(
            signInCompleteCommandResult,
            signInInvalidCredentialsCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInSubmitPasswordCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}
//endregion

//region reset password
private val resetPasswordCodeRequiredCommandResult = ResetPasswordCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH,
    correlationId = CORRELATION_ID
)

private val resetPasswordCompleteCommandResult = ResetPasswordCommandResult.Complete(continuationToken = "", expiresIn = null, correlationId = CORRELATION_ID)

private val resetPasswordEmailNotVerifiedCommandResult = ResetPasswordCommandResult.EmailNotVerified(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

private val resetPasswordIncorrectCodeCommandResult = ResetPasswordCommandResult.IncorrectCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB,
    correlationId = CORRELATION_ID
)

private val resetPasswordPasswordNotAcceptedCommandResult = ResetPasswordCommandResult.PasswordNotAccepted(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB,
    correlationId = CORRELATION_ID
)

private val resetPasswordPasswordNotSetCommandResult = ResetPasswordCommandResult.PasswordNotSet(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

private val resetPasswordPasswordResetFailedCommandResult = ResetPasswordCommandResult.PasswordResetFailed(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

private val resetPasswordPasswordRequiredCommandResult = ResetPasswordCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN,
    correlationId = CORRELATION_ID
)

private val resetPasswordUserNotFoundCommandResult = ResetPasswordCommandResult.UserNotFound(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    correlationId = CORRELATION_ID
)

// ResetPasswordStartCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestResetPasswordStartCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getResetPasswordStartCommandResults() = listOf(
            resetPasswordCodeRequiredCommandResult,
            resetPasswordEmailNotVerifiedCommandResult,
            resetPasswordPasswordNotSetCommandResult,
            resetPasswordUserNotFoundCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeResetPasswordStartCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            SignUpCommandResult.Complete(
                continuationToken = null,
                correlationId = CORRELATION_ID,
                expiresIn = null
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// ResetPasswordSubmitCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestResetPasswordSubmitCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getResetPasswordStartCommandResults() = listOf(
            resetPasswordIncorrectCodeCommandResult,
            resetPasswordPasswordRequiredCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeResetPasswordSubmitCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            SignUpCommandResult.Complete(
                continuationToken = null,
                correlationId = CORRELATION_ID,
                expiresIn = null
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// ResetPasswordResendCodeCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestResetPasswordResendCodeCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getResetPasswordStartCommandResults() = listOf(
            resetPasswordCodeRequiredCommandResult,
            redirectCommandResult,
            APIErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeResetPasswordResendCodeCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordResendCodeCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            SignUpCommandResult.Complete(
                continuationToken = null,
                correlationId = CORRELATION_ID,
                expiresIn = null
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}

// ResetPasswordSubmitNewPasswordCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestResetPasswordSubmitNewPasswordCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getResetPasswordSubmitNewPasswordCommandResults() = listOf(
            resetPasswordCompleteCommandResult,
            resetPasswordPasswordNotAcceptedCommandResult,
            resetPasswordPasswordResetFailedCommandResult,
            APIErrorCommandResult,
            resetPasswordUserNotFoundCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeResetPasswordSubmitNewPasswordCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitNewPasswordCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            SignUpCommandResult.Complete(
                continuationToken = null,
                correlationId = CORRELATION_ID,
                expiresIn = null
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitNewPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeErrorStatus() {
        val commandResult = CommandResult<Any>(
            ResultStatus.ERROR,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitNewPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWithException() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ClientException(
                ERROR
            ),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitNewPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.APIError)
    }
}
// emdregion
