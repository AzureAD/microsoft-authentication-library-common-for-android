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

sealed interface JITChallengeAuthMethodCommandResult: INativeAuthCommandResult
sealed interface JITSubmitChallengeCommandResult: INativeAuthCommandResult
interface JITCommandResult {

    data class VerificationRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : JITChallengeAuthMethodCommandResult {
        override fun toUnsanitizedString(): String = "VerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String = "VerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    data class BlockedVerificationContact(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>
    ) : JITChallengeAuthMethodCommandResult {
        override fun toUnsanitizedString(): String = "BlockedVerificationContact(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "BlockedVerificationContact(correlationId=$correlationId)"
    }

    data class IncorrectVerificationContact(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>
    ) : JITChallengeAuthMethodCommandResult {
        override fun toUnsanitizedString(): String = "IncorrectVerificationContact(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "IncorrectVerificationContact(correlationId=$correlationId)"
    }

    data class IncorrectChallenge(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>,
        val subError: String
    ) : JITSubmitChallengeCommandResult {
        override fun toUnsanitizedString(): String = "IncorrectChallenge(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes, subError=$subError)"

        override fun toString(): String = "IncorrectChallenge(correlationId=$correlationId)"
    }
}