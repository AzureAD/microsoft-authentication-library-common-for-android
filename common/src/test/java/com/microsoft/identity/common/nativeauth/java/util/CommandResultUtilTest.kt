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
package com.microsoft.identity.common.java.nativeauth.util

import com.microsoft.identity.common.java.commands.ICommandResult.ResultStatus
import com.microsoft.identity.common.java.controllers.CommandResult
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

private const val SIGNUP_TOKEN = "1234"
private const val CREDENTIAL_TOKEN = "ABCD"
private const val PASSWORD_RESET_TOKEN = "klsdjf"
private const val PASSWORD_SUBMIT_TOKEN = "ioamf43"
private const val ERROR = "error_code"
private const val ERROR_DESCRIPTION = "error description"
private const val CHALLENGE_TARGET_LABEL = "user@contoso.com"
private const val CHALLENGE_TYPE = "email"
private const val CODE_LENGTH = 6

/**
 * Split into multiple tests, as JUnit4 doesn't have support for @MethodSource like JUnit5 does.
 */

//region sign-up

private val redirectCommandResult = INativeAuthCommandResult.Redirect()

private val unknownErrorCommandResult = INativeAuthCommandResult.UnknownError(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
)

//region sign-in
private val signInCodeRequiredCommandResult = SignInCommandResult.CodeRequired(
    credentialToken = CREDENTIAL_TOKEN,
    challengeChannel = CHALLENGE_TYPE,
    challengeTargetLabel = CHALLENGE_TARGET_LABEL,
    codeLength = CODE_LENGTH
)

private val signInCompleteCommandResult = SignInCommandResult.Complete(
    authenticationResult = mock<ILocalAuthenticationResult>()
)

private val signInInvalidCredentialsCommandResult = SignInCommandResult.InvalidCredentials(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList()
)

private val signInIncorrectCodeCommandResult = SignInCommandResult.IncorrectCode(
    error = ERROR,
    errorDescription = ERROR_DESCRIPTION,
    errorCodes = emptyList()
)

private val signInPasswordRequiredCommandResult = SignInCommandResult.PasswordRequired(
    credentialToken = CREDENTIAL_TOKEN
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
            ResetPasswordCommandResult.Complete,
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
class CommandResultUtilTestSignInWithSLTCommandResult(private val resultValue: Any) {

    companion object {
        @JvmStatic
        @Parameters
        fun getSignInWithSLTCommandResults() = listOf(
            signInCodeRequiredCommandResult,
            signInCompleteCommandResult,
            signInPasswordRequiredCommandResult,
            redirectCommandResult,
            unknownErrorCommandResult
        )
    }

    @Test
    fun checkAndWrapCommandResultTypeSignInWithSLTCommandResultSuccess() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            resultValue,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithSLTCommandResult>()
        assertEquals(resultValue.javaClass, result.javaClass)
    }

    @Test
    fun testCheckAndWrapCommandResultTypeCompletedStatusWrongType() {
        val commandResult = CommandResult<Any>(
            ResultStatus.COMPLETED,
            ResetPasswordCommandResult.Complete,
            null
        )

        val result = commandResult.checkAndWrapCommandResultType<SignInWithSLTCommandResult>()
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

        val result = commandResult.checkAndWrapCommandResultType<SignInWithSLTCommandResult>()
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

        val result = commandResult.checkAndWrapCommandResultType<SignInWithSLTCommandResult>()
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
            ResetPasswordCommandResult.Complete,
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
            ResetPasswordCommandResult.Complete,
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
            ResetPasswordCommandResult.Complete,
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
