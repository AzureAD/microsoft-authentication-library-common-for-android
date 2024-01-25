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
package com.microsoft.identity.common.java.nativeauth.providers.responses.signin

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the OAuth /token endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface SignInTokenApiResult: ApiResult {
    data class Success(
        override val correlationId: String,
        val tokenResponse: MicrosoftStsTokenResponse
    ) : SignInTokenApiResult

    data class MFARequired(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult

    data class UserNotFound(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val correlationId: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult

    data class InvalidCredentials(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val correlationId: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult

    data class CodeIncorrect(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        val subError: String,
        override val correlationId: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult

    data class InvalidAuthenticationType(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val correlationId: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val correlationId: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), SignInTokenApiResult
}
