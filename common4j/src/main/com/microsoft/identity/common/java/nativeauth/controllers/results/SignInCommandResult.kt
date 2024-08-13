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
package com.microsoft.identity.common.java.nativeauth.controllers.results

import com.microsoft.identity.common.java.result.ILocalAuthenticationResult

sealed interface SignInStartCommandResult: INativeAuthCommandResult
sealed interface SignInWithContinuationTokenCommandResult: INativeAuthCommandResult
sealed interface SignInSubmitCodeCommandResult: INativeAuthCommandResult
sealed interface SignInResendCodeCommandResult: INativeAuthCommandResult
sealed interface SignInSubmitPasswordCommandResult: INativeAuthCommandResult

/**
 * Reflects the possible results from sign in commands.
 * Conforms to the INativeAuthCommandResult interface, and is mapped from the respective API result classes returned for each endpoint.
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
 */
interface SignInCommandResult {
    data class Complete(
        override val correlationId: String,
        val authenticationResult: ILocalAuthenticationResult
    ) : SignInStartCommandResult, SignInWithContinuationTokenCommandResult,
        SignInSubmitCodeCommandResult, SignInSubmitPasswordCommandResult,
        MFASubmitChallengeCommandResult {
        override fun toUnsanitizedString(): String = "Complete(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    data class PasswordRequired(
        override val correlationId: String,
        val continuationToken: String
    ) : SignInStartCommandResult {
        override fun toUnsanitizedString(): String = "PasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    data class CodeRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : SignInStartCommandResult, SignInResendCodeCommandResult {
        override fun toUnsanitizedString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    data class UserNotFound(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>
    ) : SignInStartCommandResult {
        override fun toUnsanitizedString(): String = "UserNotFound(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UserNotFound(correlationId=$correlationId)"
    }

    data class InvalidCredentials(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>
    ) : SignInStartCommandResult, SignInSubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String = "InvalidCredentials(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "InvalidCredentials(correlationId=$correlationId)"
    }

    data class IncorrectCode(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>,
        val subError: String
    ) : SignInSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "IncorrectCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes, subError=$subError)"

        override fun toString(): String = "IncorrectCode(correlationId=$correlationId)"
    }

    data class MFARequired(
        override val correlationId: String,
        val continuationToken: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>,
        val subError: String
    ) : SignInStartCommandResult, SignInSubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String = "MFARequired(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes, subError=$subError)"

        override fun toString(): String = "MFARequired(correlationId=$correlationId)"
    }
}
