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
import org.mockito.kotlin.mock

private const val CONTINUATION_TOKEN = "1234"
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
    requiredAttributes = emptyList()
)

private val signUpAuthNotSupportedCommandResult = SignUpCommandResult.AuthNotSupported(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
)

private val signUpCodeRequiredCommandResult = SignUpCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH
)

private val signUpCompleteCommandResult = SignUpCommandResult.Complete(
    continuationToken = null,
    expiresIn = null
)

private val signUpInvalidAttributesCommandResult = SignUpCommandResult.InvalidAttributes(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    invalidAttributes = emptyList()
)

private val signUpInvalidCodeCommandResult = SignUpCommandResult.InvalidCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB
)

private val signUpInvalidPasswordCommandResult = SignUpCommandResult.InvalidPassword(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_PASSWORD
)

private val signUpPasswordRequiredCommandResult = SignUpCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN,
)

private val signUpUsernameAlreadyExistsCommandResult = SignUpCommandResult.UsernameAlreadyExists(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
)

private val redirectCommandResult = INativeAuthCommandResult.Redirect()

private val unknownErrorCommandResult = INativeAuthCommandResult.UnknownError(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
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
            unknownErrorCommandResult,
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult,
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult,
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult,
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult,
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
    }
}
//endregion

//region sign-in
private val signInCodeRequiredCommandResult = SignInCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH
)

private val signInCompleteCommandResult = SignInCommandResult.Complete(
    authenticationResult = mockk<ILocalAuthenticationResult>()
)

private val signInInvalidCredentialsCommandResult = SignInCommandResult.InvalidCredentials(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList()
)

private val signInIncorrectCodeCommandResult = SignInCommandResult.IncorrectCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList(),
    subError = SUBERROR_INVALID_OOB
)

private val signInPasswordRequiredCommandResult = SignInCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN,
    )

private val signInUserNotFoundCommandResult = SignInCommandResult.UserNotFound(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList()
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
            unknownErrorCommandResult
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
    }
}

// SignUpStartCommandResult
@RunWith(Parameterized::class)
class CommandResultUtilTestSignInWithContinuationTokenCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInWithContinuationTokenCommandResults() = listOf(
            signInCodeRequiredCommandResult,
            signInCompleteCommandResult,
            signInPasswordRequiredCommandResult,
            redirectCommandResult,
            unknownErrorCommandResult
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithContinuationTokenCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult
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
            ResetPasswordCommandResult.Complete("", null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
    }
}
//endregion

//region reset password
private val resetPasswordCodeRequiredCommandResult = ResetPasswordCommandResult.CodeRequired(
    continuationToken = CONTINUATION_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH
)

private val resetPasswordCompleteCommandResult = ResetPasswordCommandResult.Complete("", null)

private val resetPasswordEmailNotVerifiedCommandResult = ResetPasswordCommandResult.EmailNotVerified(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION
)

private val resetPasswordIncorrectCodeCommandResult = ResetPasswordCommandResult.IncorrectCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB
)

private val resetPasswordPasswordNotAcceptedCommandResult = ResetPasswordCommandResult.PasswordNotAccepted(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    subError = SUBERROR_INVALID_OOB
)

private val resetPasswordPasswordNotSetCommandResult = ResetPasswordCommandResult.PasswordNotSet(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION
)

private val resetPasswordPasswordResetFailedCommandResult = ResetPasswordCommandResult.PasswordResetFailed(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION
)

private val resetPasswordPasswordRequiredCommandResult = ResetPasswordCommandResult.PasswordRequired(
    continuationToken = CONTINUATION_TOKEN
)

private val resetPasswordUserNotFoundCommandResult = ResetPasswordCommandResult.UserNotFound(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION
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
            unknownErrorCommandResult
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
            SignUpCommandResult.Complete(continuationToken = null, expiresIn = null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult
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
            SignUpCommandResult.Complete(continuationToken = null, expiresIn = null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult
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
            SignUpCommandResult.Complete(continuationToken = null, expiresIn = null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordResendCodeCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
            unknownErrorCommandResult,
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
            SignUpCommandResult.Complete(continuationToken = null, expiresIn = null),
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<ResetPasswordSubmitNewPasswordCommandResult>()
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
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
        assertTrue(result is INativeAuthCommandResult.UnknownError)
    }
}
// emdregion
