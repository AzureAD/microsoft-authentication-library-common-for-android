// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.controllers.results

import com.microsoft.identity.common.java.nativeauth.providers.responses.UserAttributeApiResult

sealed interface SignUpSubmitCodeCommandResult: INativeAuthCommandResult
sealed interface SignUpSubmitUserAttributesCommandResult: INativeAuthCommandResult
sealed interface SignUpSubmitPasswordCommandResult: INativeAuthCommandResult
sealed interface SignUpResendCodeCommandResult: INativeAuthCommandResult
sealed interface SignUpStartCommandResult: INativeAuthCommandResult

/**
 * Reflects the possible results from sign up commands.
 * Conforms to the INativeAuthCommandResult interface, and is mapped from the respective API result classes returned for each endpoint.
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
 */
interface SignUpCommandResult {

    /**
     * There exists an account for the given username, so signup request cannot proceed.
     */
    data class UsernameAlreadyExists(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpSubmitUserAttributesCommandResult, SignUpSubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String = "UsernameAlreadyExists(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "UsernameAlreadyExists(correlationId=$correlationId)"
    }

    /**
     * Denotes completion of Signup operation
     */
    data class Complete (
        override val correlationId: String,
        val continuationToken: String?,
        val expiresIn: Int?
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult {
        override fun toUnsanitizedString(): String = "Complete(correlationId=$correlationId, expiresIn=$expiresIn)"

        override fun containsPii(): Boolean = false

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * Signup is at a state where the user has to provide an out of band code to progress in the flow.
     */
    data class CodeRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : SignUpStartCommandResult,
        SignUpResendCodeCommandResult {
        override fun toUnsanitizedString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }


    /**
     * Signup operation requires user to supply a password to progress in the flow.
     */
    data class PasswordRequired(
        override val correlationId: String,
        val continuationToken: String
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "PasswordRequired(correlationId=$correlationId)"

        override fun containsPii(): Boolean = false

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * Signup operation requires user to supply attributes to progress in the flow. The parameter
     * requiredAttributes contains the list of required attributes
     */
    data class AttributesRequired(
        override val correlationId: String,
        val continuationToken: String,
        val error: String,
        val errorDescription: String,
        val requiredAttributes: List<UserAttributeApiResult>,
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "AttributesRequired(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, requiredAttributes=$requiredAttributes)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "AttributesRequired(correlationId=$correlationId, requiredAttributes=$requiredAttributes)"
    }

    /**
     * The signup operation cannot progress as the provided password is not acceptable
     */
    data class InvalidPassword(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String = "InvalidPassword(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "InvalidPassword(correlationId=$correlationId)"
    }

    /**
     * The signup operation cannot progress as the provided out of band code is invalid.
     */
    data class InvalidCode(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String
    ) : SignUpSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "InvalidCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "InvalidCode(correlationId=$correlationId)"
    }

    /**
     * The signup operation cannot progress as the provided attributes are not valid. Some user
     * attributes can be validated at the server and if the validation fails then this error is returned.
     */
    data class InvalidAttributes(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val invalidAttributes: List<String>,
    ) : SignUpStartCommandResult, SignUpSubmitUserAttributesCommandResult {
        override fun toUnsanitizedString(): String = "InvalidAttributes(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "InvalidAttributes(correlationId=$correlationId, invalidAttributes=$invalidAttributes)"
    }

    /**
     * Signup operation has failed as the server does not support requested authentication mechanism
     */
    data class AuthNotSupported(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : SignUpStartCommandResult {
        override fun toUnsanitizedString(): String = "AuthNotSupported(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun containsPii(): Boolean = true

        override fun toString(): String = "AuthNotSupported(correlationId=$correlationId"
    }
}
