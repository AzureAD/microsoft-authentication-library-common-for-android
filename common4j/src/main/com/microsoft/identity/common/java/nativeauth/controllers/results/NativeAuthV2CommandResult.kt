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

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2RequiredAttribute
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult

/** Every V2 interaction uses the same result contract, independent of the submitted operation. */
sealed interface NativeAuthV2InteractionCommandResult : INativeAuthCommandResult

// Source aliases keep command names descriptive without enforcing per-operation outcome lists.
typealias NativeAuthV2ResetPasswordStartCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SelectAuthMethodCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SubmitCodeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2ResetPasswordSubmitCodeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignUpSubmitCodeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignInSubmitCodeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2ResendCodeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SubmitNewPasswordCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignInAfterResetPasswordCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignInStartCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SubmitPasswordCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SubmitMFAChallengeCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignUpStartCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SubmitAttributesCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2SignInAfterSignUpCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2StrongAuthCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2ExistingAccountCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2RegistrationCommandResult = NativeAuthV2InteractionCommandResult
typealias NativeAuthV2FlowCompletionCommandResult = NativeAuthV2InteractionCommandResult

/**
 * Contains the possible results from every V2 interaction command.
 *
 * Conforms to [INativeAuthCommandResult] and covers all terminal and continuation states
 * returned by the V2 interaction protocol endpoints.
 *
 * Error cases reuse the shared [INativeAuthCommandResult.APIError] and
 * [INativeAuthCommandResult.Redirect] types declared in [INativeAuthCommandResult]; those types
 * share [NativeAuthV2InteractionCommandResult] with the result classes below.
 */
interface NativeAuthV2CommandResult {
    /** A sign-up attempt must continue by verifying the existing account's email code. */
    data class ExistingAccountCodeRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String
    ) : NativeAuthV2InteractionCommandResult {
        override fun toString(): String = "ExistingAccountCodeRequired(correlationId=$correlationId)"
        override fun toUnsanitizedString(): String = toString()
    }

    /** A sign-up attempt must continue with the existing account's password, not a new password. */
    data class ExistingAccountPasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState
    ) : NativeAuthV2InteractionCommandResult {
        override fun toString(): String = "ExistingAccountPasswordRequired(correlationId=$correlationId)"
        override fun toUnsanitizedString(): String = toString()
    }

    /** The app must choose an offered enrollment method and collect an explicit contact. */
    data class StrongAuthRegistrationRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val authMethods: List<NativeAuthV2AuthMethod>
    ) : NativeAuthV2RegistrationCommandResult {
        override fun toString(): String = "StrongAuthRegistrationRequired(correlationId=$correlationId)"
        override fun toUnsanitizedString(): String = toString()
    }

    /** The service sent or resent an activation code; the successor retains its current links. */
    data class StrongAuthVerificationRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String? = null
    ) : NativeAuthV2InteractionCommandResult {
        override fun toString(): String = "StrongAuthVerificationRequired(correlationId=$correlationId, codeLength=$codeLength)"
        override fun toUnsanitizedString(): String = toString()
    }

    /**
     * The server has sent a one-time code to the user's registered channel.
     * [continuationState] is the opaque mid-flow state to be passed back on the submit-code step.
     * May follow any supported interaction operation.
     */
    data class CodeRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    /**
     * The app must select an offered authentication method before a challenge is sent,
     * including when the server requires multi-factor authentication.
     */
    data class AuthMethodSelectionRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val authMethods: List<NativeAuthV2AuthMethod>,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId=$correlationId, authMethods=${authMethods.map { it.toUnsanitizedString() }})"

        override fun toString(): String =
            "NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId=$correlationId, authMethods=${authMethods.map { it.toString() }})"
    }

    /**
     * The server has accepted the OTP and is requesting a new password.
     * [continuationState] is the opaque mid-flow state to be passed back on the submit-new-password step.
     * Applies to the submit-code step.
     */
    data class NewPasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NewPasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The password reset has completed server-side. The app must explicitly invoke the
     * sign-in-after-reset command with [continuationState] to exchange it for tokens and persist
     * them to cache; no token exchange or cache write happens until then.
     *
     * Applies to the submit-new-password step, including its fast-forward and poll-completion
     * paths, and to enrollment or MFA verification after that submission. It is deliberately not
     * reachable before a new password has been supplied.
     */
    data class SignInAfterResetPasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The password reset flow has completed successfully.
     * [authenticationResult] may be non-null when the server returns a sign-in continuation token
     * that was redeemed inline; it is null when no automatic sign-in was performed.
     * Applies to any step that reaches terminal completion, including the explicit
     * sign-in-after-reset command.
     */
    data class Complete(
        override val correlationId: String,
        val authenticationResult: ILocalAuthenticationResult?,
        val continuationToken: String?,
        val expiresIn: Int?,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.Complete(correlationId=$correlationId, expiresIn=$expiresIn)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server rejected the submitted OTP.
     * The developer can retry through the state object used for the failed call.
     * Applies to the submit-code step.
     */
    data class IncorrectCode @JvmOverloads constructor(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId)"
    }

    /**
     * The server offered the password first factor and no password was supplied at the sign-in
     * entry point, so the app must collect one and submit it through [continuationState].
     * Applies to the sign-in start step.
     */
    data class PasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server sent a multi-factor challenge to the selected method and awaits the code.
     * Applies to authentication-method selection.
     */
    data class MFAVerificationRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : NativeAuthV2SelectAuthMethodCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.MFAVerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.MFAVerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    /**
     * The server rejected a password submitted from the password-required state. The developer can
     * retry through the state object used for the failed call.
     * Applies to the submit-password step.
     */
    data class IncorrectPassword @JvmOverloads constructor(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null
    ) : NativeAuthV2SubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.IncorrectPassword(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.IncorrectPassword(correlationId=$correlationId)"
    }

    /**
     * The supplied password did not meet policy requirements.
     * The developer can retry through the state object used for the failed call.
     * Applies to the submit-new-password step.
     */
    data class PasswordNotAccepted @JvmOverloads constructor(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null
    ) : NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordNotAccepted(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.PasswordNotAccepted(correlationId=$correlationId)"
    }

    /**
     * The server rejected the reset attempt after the new password was submitted.
     * Applies to the submit-new-password step.
     */
    data class PasswordResetFailed(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordResetFailed(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.PasswordResetFailed(correlationId=$correlationId)"
    }

    /**
     * The username provided is not registered in the tenant.
     * Applies to the start step.
     */
    data class UserNotFound @JvmOverloads constructor(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null,
        val subError: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.UserNotFound(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.UserNotFound(correlationId=$correlationId)"
    }

    /**
     * The sign-up flow requires the app to collect and submit one or more account attributes via
     * [continuationState]. [requiredAttributes] are the attributes the server requested, in server
     * order. Applies to the sign-up start, submit-attributes, and submit-code steps (the last when
     * the server requests further attributes after code verification).
     */
    data class AttributesRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val requiredAttributes: List<NativeAuthV2RequiredAttribute>,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.AttributesRequired(correlationId=$correlationId, requiredAttributes=${requiredAttributes.map { it.toUnsanitizedString() }})"

        override fun toString(): String =
            "NativeAuthV2CommandResult.AttributesRequired(correlationId=$correlationId, requiredAttributes=${requiredAttributes.map { it.toString() }})"
    }

    /**
     * One or more submitted attributes failed server-side validation. [invalidAttributes] are the
     * wire names of the rejected attributes, in server order; the app can retry through
     * [continuationState] with corrected values. Applies to the sign-up start and submit-attributes
     * steps, including initial attribute submission resumed after sign-up code verification.
     */
    data class AttributesInvalid @JvmOverloads constructor(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val invalidAttributes: List<String>,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>? = null,
        val subError: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.AttributesInvalid(correlationId=$correlationId, invalidAttributes=$invalidAttributes, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.AttributesInvalid(correlationId=$correlationId, invalidAttributes=$invalidAttributes)"
    }

    /**
     * An account already exists for the identifier supplied to sign-up.
     * Applies to the sign-up start and submit-attributes steps, including initial attribute
     * submission resumed after sign-up code verification.
     */
    data class UserAlreadyExists @JvmOverloads constructor(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>? = null,
        val subError: String? = null,
        val attributeValidationDetails: List<Map<String, Any?>>? = null,
        override val traceId: String? = null,
        override val timestamp: String? = null
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.UserAlreadyExists(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.UserAlreadyExists(correlationId=$correlationId)"
    }

    /**
     * The sign-up has completed server-side. The app must explicitly invoke the
     * sign-in-after-sign-up command with [continuationState] to exchange it for tokens and persist
     * them to cache; no token exchange or cache write happens until then. Applies to the
     * submit-attributes and submit-code steps (the last when a code-only sign-up completes without
     * a password).
     */
    data class SignInAfterSignUpRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The operation is not yet implemented server-side or is unsupported for this tenant.
     * May apply to any step.
     */
    data class NotImplemented(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : NativeAuthV2InteractionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId)"
    }
}
