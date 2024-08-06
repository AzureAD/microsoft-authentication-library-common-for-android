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
 * Represents the potential result types returned from the Sign Up /continue endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface SignUpContinueApiResult: ApiResult {

    /**
     * The response from Signup Continue is to redirect to a browser based authentication.
     */
    data class Redirect(
        override val correlationId: String,
    ) : SignUpContinueApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * Signup operation has successfully completed.
     */
    data class Success(
        override val correlationId: String,
        val continuationToken: String?,
        val expiresIn: Int?
    ) : SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "Success(correlationId=$correlationId, " +
                "expiresIn=$expiresIn)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * Signup operation requires user attributes to continue further.
     */
    data class AttributesRequired(
        override val correlationId: String,
        val continuationToken: String,
        override val error: String,
        override val errorDescription: String,
        val requiredAttributes: List<UserAttributeApiResult>
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "AttributesRequired(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, " +
                "requiredAttributes=$requiredAttributes)"

        override fun toString(): String = "AttributesRequired(correlationId=$correlationId, " +
                "requiredAttributes=$requiredAttributes)"
    }

    /**
     * Signup operation requires user credentials to continue further.
     */
    data class CredentialRequired(
        override val correlationId: String,
        val continuationToken: String,
        override val error: String,
        override val errorDescription: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "CredentialRequired(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "CredentialRequired(correlationId=$correlationId)"
    }

    /**
     * Signup continue request was issues with an expired token.
     */
    data class ExpiredToken(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "ExpiredToken(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "ExpiredToken(correlationId=$correlationId)"
    }

    /**
     * Signup operation was started for a username that already has an account.
     */
    data class UsernameAlreadyExists(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "UsernameAlreadyExists(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UsernameAlreadyExists(correlationId=$correlationId)"
    }

    /**
     * The OOB value sent as part of Signup continue request was incorrect.
     */
    data class InvalidOOBValue(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "InvalidOOBValue(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "InvalidOOBValue(correlationId=$correlationId)"
    }

    /**
     * The user attributes sent as part of Signup Continue request failed
     * validation on server.
     */
    data class InvalidAttributes(
        override val correlationId: String,
        val invalidAttributes: List<String>,
        override val error: String,
        override val errorDescription: String,
        override val subError: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString(): String = "InvalidAttributes(correlationId=$correlationId, " +
                "invalidAttributes=$invalidAttributes, error=$error, errorDescription=$errorDescription, " +
                "subError=$subError)"

        override fun toString(): String = "InvalidAttributes(correlationId=$correlationId)"
    }

    /**
     * The Signup Continue API request failed due to an unknown error.
     */
    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }

    /**
     * The Sign Up continue request failed as the password failed server side validation.
     */
    data class InvalidPassword(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpContinueApiResult {
        override fun toUnsanitizedString() = "InvalidPassword(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes, subError=$subError)"

        override fun toString(): String = "InvalidPassword(correlationId=$correlationId)"
    }
}
