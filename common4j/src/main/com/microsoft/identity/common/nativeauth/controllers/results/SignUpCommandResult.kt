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
package com.microsoft.identity.common.nativeauth.controllers.results

import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.nativeauth.providers.responses.UserAttributeApiResult

sealed interface SignUpSubmitCodeCommandResult: INativeAuthCommandResult
sealed interface SignUpSubmitUserAttributesCommandResult: INativeAuthCommandResult
sealed interface SignUpSubmitPasswordCommandResult: INativeAuthCommandResult
sealed interface SignUpResendCodeCommandResult: INativeAuthCommandResult
sealed interface SignUpStartCommandResult: INativeAuthCommandResult

/**
 * Reflects the possible results from sign up commands.
 * Conforms to the INativeAuthCommandResult interface, and is mapped from the respective API result classes returned for each endpoint.
 * @see com.microsoft.identity.common.nativeauth.providers.responses.signup.SignUpStartApiResult
 * @see com.microsoft.identity.common.nativeauth.providers.responses.signup.SignUpChallengeApiResult
 * @see com.microsoft.identity.common.nativeauth.providers.responses.signup.SignUpContinueApiResult
 */
interface SignUpCommandResult {

    /**
     * There exists an account for the given username, so signup request cannot proceed.
     */
    data class UsernameAlreadyExists(
        val error: String,
        val errorDescription: String,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId
    ) : SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpSubmitUserAttributesCommandResult, SignUpSubmitPasswordCommandResult

    /**
     * Denotes completion of Signup operation
     */
    data class Complete (
        val signInSLT: String?,
        val expiresIn: Int?
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult

    /**
     * Signup is at a state where the user has to provide an out of band code to progress in the flow.
     */
    data class CodeRequired(
        val signupToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : SignUpStartCommandResult,
        SignUpResendCodeCommandResult


    /**
     * Signup operation requires user to supply a password to progress in the flow.
     */
    data class PasswordRequired(
        val signupToken: String
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult

    /**
     * Signup operation requires user to supply attributes to progress in the flow. The parameter
     * requiredAttributes contains the list of required attributes
     */
    data class AttributesRequired(
        val signupToken: String,
        val error: String,
        val errorDescription: String,
        val requiredAttributes: List<UserAttributeApiResult>,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult

    /**
     * The signup operation cannot progress as the provided email address is invalid.
     */
    data class InvalidEmail(
        val error: String,
        val errorDescription: String,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult

    /**
     * The signup operation cannot progress as the provided password is not acceptable
     */
    data class InvalidPassword(
        val error: String,
        val errorDescription: String,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult


    /**
     * The signup operation cannot progress as the provided out of band code is invalid.
     */
    data class InvalidCode(
        val error: String,
        val errorDescription: String,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpSubmitCodeCommandResult

    /**
     * The signup operation cannot progress as the provided attributes are not valid. Some user
     * attributes can be validated at the server and if the validation fails then this error is returned.
     */
    data class InvalidAttributes(
        val error: String,
        val errorDescription: String,
        val invalidAttributes: List<String>,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpStartCommandResult, SignUpSubmitUserAttributesCommandResult

    /**
     * Signup operation has failed as the server does not support requested authentication mechanism
     */
    data class AuthNotSupported(
        val error: String,
        val errorDescription: String,
        val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId  //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
    ) : SignUpStartCommandResult
}
