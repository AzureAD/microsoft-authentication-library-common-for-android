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
package com.microsoft.identity.common.java.providers.nativeauth.responses.signup

import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.RequiredUserAttributeApiResult

sealed interface SignUpContinueApiResult {
    object Redirect : SignUpContinueApiResult

    data class Success(
        val signInSLT: String?,
        val expiresIn: Int?
    ) : SignUpContinueApiResult

    data class AttributesRequired(
        val signupToken: String,
        override val error: String,
        override val errorDescription: String,
        val requiredAttributes: List<RequiredUserAttributeApiResult>
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class CredentialRequired(
        val signupToken: String,
        override val error: String,
        override val errorDescription: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class ExpiredToken(
        override val error: String,
        override val errorDescription: String
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class UsernameAlreadyExists(
        override val error: String,
        override val errorDescription: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class InvalidOOBValue(
        override val error: String,
        override val errorDescription: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class InvalidAttributes(
        override val error: String,
        override val errorDescription: String,
        val invalidAttributes: List<String>
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val details: List<Map<String, String>>?
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult

    data class InvalidPassword(
        override val error: String,
        override val errorDescription: String,
    ): ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
    ), SignUpContinueApiResult
}
