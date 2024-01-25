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
    ) : SignUpStartApiResult


    /**
     * The next step in the Signup process is account verification.
     */
    data class Success(
        val continuationToken: String,
        override val correlationId: String
    ) : SignUpStartApiResult

    /**
     * The Sign Up Start request failed as the password failed server side validation.
     */
    data class InvalidPassword(
        override val error: String,
        override val errorDescription: String,
        val subError: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult

    /**
     * The user attributes sent as part of Signup Start request failed
     * validation on server.
     */
    data class InvalidAttributes(
        override val error: String,
        override val errorDescription: String,
        val invalidAttributes: List<String>,
        val subError: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult


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
    ), SignUpStartApiResult

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
    ), SignUpStartApiResult

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
    ), SignUpStartApiResult

    /**
     * Signup start operation was started for a malformed email address.
     */
    data class InvalidEmail(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), SignUpStartApiResult

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
    ), SignUpStartApiResult
}
