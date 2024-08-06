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
package com.microsoft.identity.common.java.nativeauth.providers.responses.signup

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the Sign Up /start endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface SignUpStartApiResult: ApiResult {

    /**
     * The response from Signup Start is to redirect to a browser based authentication.
     */
    data class Redirect(
        override val correlationId: String
    ) : SignUpStartApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }


    /**
     * The next step in the Signup process is account verification.
     */
    data class Success(
        val continuationToken: String,
        override val correlationId: String
    ) : SignUpStartApiResult {
        override fun toUnsanitizedString(): String = "Success(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The Sign Up Start request failed as the password failed server side validation.
     */
    data class InvalidPassword(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "InvalidPassword(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "InvalidPassword(correlationId=$correlationId)"
    }

    /**
     * The user attributes sent as part of Signup Start request failed
     * validation on server.
     */
    data class InvalidAttributes(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        val invalidAttributes: List<String>,
        override val subError: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "InvalidAttributes(correlationId=$correlationId, " +
                "invalidAttributes=$invalidAttributes, error=$error, errorDescription=$errorDescription, " +
                "errorCodes=$errorCodes, subError=$subError)"

        override fun toString(): String = "InvalidPassword(correlationId=$correlationId)"
    }

    /**
     * The Signup Start API request failed due to an unknown error.
     */
    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }

    /**
     * The server does not support the challenge types presented by client as part of
     * Signup start request.
     */
    data class UnsupportedChallengeType(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "UnsupportedChallengeType(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnsupportedChallengeType(correlationId=$correlationId)"
    }

    /**
     * Signup start operation was started for a username that already has an account.
     */
    data class UsernameAlreadyExists(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "UsernameAlreadyExists(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UsernameAlreadyExists(correlationId=$correlationId)"
    }

    /**
     * Signup start operation was started for a malformed email address.
     */
    data class InvalidUsername(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "InvalidUsername(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "InvalidUsername(correlationId=$correlationId)"
    }

    /**
     * Signup start operation has failed as the server does not support requested authentication mechanism
     */
    data class AuthNotSupported(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult {
        override fun toUnsanitizedString() = "AuthNotSupported(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "AuthNotSupported(correlationId=$correlationId)"
    }
}
