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

import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.AuthenticationMethodApiResult
import com.microsoft.identity.common.java.nativeauth.util.toUnsanitizedString

/**
 * Reflects the possible results of MFA commands.
 * Conforms to the INativeAuthCommandResult interface, and is mapped from the respective API result classes returned for each endpoint.
 */
sealed interface MFACommandResult : INativeAuthCommandResult {
    data class VerificationRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : MFACommandResult {
        override fun toUnsanitizedString(): String = "VerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String = "VerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    data class SelectionRequired(
        override val correlationId: String,
        val continuationToken: String,
        val authMethods: List<AuthenticationMethodApiResult>
    ) : MFACommandResult {
        override fun toUnsanitizedString(): String = "SelectionRequired(correlationId=$correlationId, authMethods=${authMethods.toUnsanitizedString()})"

        override fun toString(): String = "SelectionRequired(correlationId=$correlationId, authMethods=${authMethods})"
    }
}
