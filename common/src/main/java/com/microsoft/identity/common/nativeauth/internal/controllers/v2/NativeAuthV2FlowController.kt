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
package com.microsoft.identity.common.nativeauth.internal.controllers.v2

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.*
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2InteractionCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.*
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.ThreadUtils
import com.microsoft.identity.common.nativeauth.internal.controllers.BaseNativeAuthController
import lombok.EqualsAndHashCode

/**
 * V2 Native Auth controller. Request construction stays operation-specific; every interaction
 * response, including automatic follow-ups, is routed by [routeInteraction].
 * Authorize-challenge and token exchange retain their separate protocol contracts.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthV2FlowController : BaseNativeAuthController() {
    companion object {
        private val TAG = NativeAuthV2FlowController::class.java.simpleName
        const val MAX_POLL_ATTEMPTS = 5
        const val FALLBACK_POLL_DELAY_MS = 1500L
        const val MAX_POLL_DELAY_MS = 30_000L
        private const val UNEXPECTED_RESULT = "unexpected_api_result"
        private const val ATTRIBUTE_NAME_EMAIL = "email"
        private const val ATTRIBUTE_NAME_PASSWORD = "password"
        private const val ATTRIBUTE_ALREADY_SUBMITTED_ERROR = "attribute_already_submitted"
        private const val POLL_TIMEOUT_ERROR = "poll_timeout"
        private const val POLL_TIMEOUT_DESCRIPTION = "Password reset completion polling timed out after $MAX_POLL_ATTEMPTS attempts."
        private const val POLL_INTERRUPTED_ERROR = "poll_interrupted"
        private const val POLL_INTERRUPTED_DESCRIPTION = "Password reset completion polling was interrupted."
    }

    private inline fun operation(
        name: String,
        parameters: BaseSignInTokenCommandParameters,
        block: () -> NativeAuthV2InteractionCommandResult
    ): NativeAuthV2InteractionCommandResult {
        LogSession.logMethodCall(TAG, parameters.getCorrelationId(), "$TAG.$name")
        try {
            return block()
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in $name", e)
            throw e
        }
    }

    fun resetPasswordStart(parameters: ResetPasswordV2StartCommandParameters) =
        operation("resetPasswordStart", parameters) {
            start(parameters, parameters.username, NativeAuthV2FlowScenario.RESET_PASSWORD, NativeAuthV2LinkRelation.RESET_PASSWORD)
        }

    fun signInStart(parameters: SignInV2StartCommandParameters) =
        operation("signInStart", parameters) {
            start(parameters, parameters.username, NativeAuthV2FlowScenario.SIGN_IN, NativeAuthV2LinkRelation.SIGN_IN)
        }

    fun signUpStart(parameters: SignUpV2StartCommandParameters) =
        operation("signUpStart", parameters) {
            validateSignUpAttributeKeys(parameters.attributes, parameters.getCorrelationId())
                ?: start(parameters, parameters.username, NativeAuthV2FlowScenario.SIGN_UP, NativeAuthV2LinkRelation.SIGN_UP, parameters)
        }

    private fun start(
        parameters: BaseSignInTokenCommandParameters,
        username: String,
        scenario: NativeAuthV2FlowScenario,
        relation: NativeAuthV2LinkRelation,
        upfront: SignUpV2StartCommandParameters? = null
    ): NativeAuthV2InteractionCommandResult {
        val strategy = createNativeAuthV2Strategy(parameters)
        val entry = strategy.performAuthorizeChallengeStart(
            parameters.getCorrelationId(), relation.value, scenario,
            addV2DefaultScopes(parameters.scopes), parameters.claimsRequestJson
        )
        val state = when (entry) {
            is AuthorizeChallengeApiResult.ContinuationRequired -> entry.continuationState
            is AuthorizeChallengeApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                entry.correlationId, entry.redirectReason, entry.errorCodes, entry.traceId, entry.timestamp
            )
            is AuthorizeChallengeApiResult.AuthorizationCode -> return INativeAuthCommandResult.APIError(
                UNEXPECTED_RESULT, "AuthorizationCode returned unexpectedly at authorize-challenge start.",
                correlationId = entry.correlationId
            )
            is AuthorizeChallengeApiResult.UnknownError -> return INativeAuthCommandResult.APIError(
                entry.error, entry.errorDescription, correlationId = entry.correlationId,
                errorCodes = entry.errorCodes, subError = entry.subError,
                attributeValidationDetails = entry.attributeValidationDetails,
                traceId = entry.traceId, timestamp = entry.timestamp
            )
        }
        val result = when (scenario) {
            NativeAuthV2FlowScenario.SIGN_IN -> strategy.performSignInStart(username, state)
            NativeAuthV2FlowScenario.SIGN_UP -> strategy.performSignUpStart(username, state)
            NativeAuthV2FlowScenario.RESET_PASSWORD -> strategy.performResetPasswordStart(username, state)
        }
        return routeInteraction(strategy, parameters, result, state, upfront)
    }

    fun selectAuthMethod(parameters: NativeAuthV2SelectAuthMethodCommandParameters) =
        operation("selectAuthMethod", parameters) {
            val strategy = createNativeAuthV2Strategy(parameters)
            val result = strategy.performMethodChallenge(parameters.continuationState, parameters.methodId)
            routeInteraction(
                strategy, parameters, result, parameters.continuationState
            )
        }

    @Deprecated("Use submitResetPasswordCode for explicit flow naming.", ReplaceWith("submitResetPasswordCode(parameters)"))
    fun submitCode(parameters: NativeAuthV2SubmitCodeCommandParameters) = submitResetPasswordCode(parameters)

    fun submitResetPasswordCode(parameters: NativeAuthV2SubmitCodeCommandParameters) =
        submitCodeInteraction("submitResetPasswordCode", parameters)

    fun submitSignInCode(parameters: NativeAuthV2SubmitCodeCommandParameters) =
        submitCodeInteraction("submitSignInCode", parameters)

    fun submitSignUpCode(parameters: NativeAuthV2SubmitCodeCommandParameters) =
        submitCodeInteraction("submitSignUpCode", parameters)

    private fun submitCodeInteraction(name: String, parameters: NativeAuthV2SubmitCodeCommandParameters) =
        operation(name, parameters) {
            val strategy = createNativeAuthV2Strategy(parameters)
            routeInteraction(strategy, parameters, strategy.performVerify(parameters.continuationState, parameters.code), parameters.continuationState)
        }

    fun submitMFAChallenge(parameters: NativeAuthV2SubmitMFAChallengeCommandParameters) =
        operation("submitMFAChallenge", parameters) {
            val strategy = createNativeAuthV2Strategy(parameters)
            routeInteraction(strategy, parameters, strategy.performVerify(parameters.continuationState, parameters.code), parameters.continuationState)
        }

    fun resendCode(parameters: NativeAuthV2ResendCodeCommandParameters) =
        operation("resendCode", parameters) {
            val strategy = createNativeAuthV2Strategy(parameters)
            routeInteraction(strategy, parameters, strategy.performResend(parameters.continuationState), parameters.continuationState)
        }

    fun submitPassword(parameters: NativeAuthV2SubmitPasswordCommandParameters): NativeAuthV2InteractionCommandResult {
        try {
            return operation("submitPassword", parameters) {
                val state = parameters.continuationState
                when {
                    state.isPasswordAttributeCollection() ->
                        submitAttributesInteraction(parameters, state, emptyMap(), parameters.password)
                    state.isPasswordVerification() -> {
                        val strategy = createNativeAuthV2Strategy(parameters)
                        routeInteraction(strategy, parameters,
                            strategy.performPasswordVerify(state, parameters.password), state)
                    }
                    else -> INativeAuthCommandResult.APIError(
                        ApiErrorResult.INVALID_STATE, "Missing or unsupported password submission instruction.",
                        correlationId = parameters.getCorrelationId())
                }
            }
        } finally {
            StringUtil.overwriteWithNull(parameters.password)
        }
    }

    fun submitNewPassword(parameters: NativeAuthV2SubmitNewPasswordCommandParameters): NativeAuthV2InteractionCommandResult {
        try {
            return operation("submitNewPassword", parameters) {
                val strategy = createNativeAuthV2Strategy(parameters)
                routeInteraction(strategy, parameters,
                    strategy.performUpdatePassword(parameters.continuationState, parameters.newPassword),
                    parameters.continuationState)
            }
        } finally {
            StringUtil.overwriteWithNull(parameters.newPassword)
        }
    }

    fun submitAttributes(parameters: NativeAuthV2SubmitAttributesCommandParameters): NativeAuthV2InteractionCommandResult {
        try {
            return operation("submitAttributes", parameters) {
                submitAttributesInteraction(parameters, parameters.continuationState, parameters.attributes, parameters.password)
            }
        } finally {
            StringUtil.overwriteWithNull(parameters.password)
        }
    }

    private fun submitAttributesInteraction(
        parameters: BaseSignInTokenCommandParameters,
        state: NativeAuthV2ContinuationState,
        submittedAttributes: Map<String, String>?,
        password: CharArray?
    ): NativeAuthV2InteractionCommandResult {
        if (state.flowScenario() == NativeAuthV2FlowScenario.SIGN_UP) {
            validateSignUpAttributeKeys(submittedAttributes, parameters.getCorrelationId())?.let { return it }
        }
        val strategy = createNativeAuthV2Strategy(parameters)
        val attributes = LinkedHashMap(state.pendingSignUpAttributesForSubmission().orEmpty())
            .apply { putAll(submittedAttributes.orEmpty()) }
        val retry = state.withAdditionalSubmittedAttributes(
            attributes.keys + if (password?.isNotEmpty() == true) listOf(ATTRIBUTE_NAME_PASSWORD) else emptyList()
        )
        return routeInteraction(strategy, parameters, strategy.performSubmitAttributes(state, attributes, password), retry)
    }

    /** Enrolls the selected method and routes the next interaction response. */
    fun enroll(parameters: NativeAuthV2EnrollCommandParameters) =
        operation("enroll", parameters.tokenParameters) {
            val strategy = createNativeAuthV2Strategy(parameters.tokenParameters)
            routeInteraction(strategy, parameters.tokenParameters,
                strategy.performEnroll(parameters.continuationState, parameters.methodId, parameters.contact),
                parameters.continuationState)
        }

    fun activate(parameters: NativeAuthV2ActivateCommandParameters) =
        operation("activate", parameters.tokenParameters) {
            val strategy = createNativeAuthV2Strategy(parameters.tokenParameters)
            routeInteraction(strategy, parameters.tokenParameters,
                strategy.performActivate(parameters.continuationState, parameters.code), parameters.continuationState)
        }

    /**
     * The sole interaction-result router. Actions are not allowlisted by API operation or origin.
     * The continuation's origin determines completion only; factor, selected-method validation,
     * credential bookkeeping and service links determine the next request. Automatic transitions
     * are not globally capped; completion polling retains its five-attempt budget.
     */
    private fun routeInteraction(
        strategy: NativeAuthV2OAuth2Strategy,
        parameters: BaseSignInTokenCommandParameters,
        initialResult: NativeAuthV2InteractionApiResult,
        initialRetryState: NativeAuthV2ContinuationState,
        upfront: SignUpV2StartCommandParameters? = null
    ): NativeAuthV2InteractionCommandResult {
        var result = initialResult
        var retryState = initialRetryState
        var pending = upfront?.attributes
        var polls = 0
        while (true) {
            if (Thread.currentThread().isInterrupted) {
                return INativeAuthCommandResult.APIError(POLL_INTERRUPTED_ERROR, POLL_INTERRUPTED_DESCRIPTION, correlationId = result.correlationId)
            }
            fun retain(state: NativeAuthV2ContinuationState): NativeAuthV2ContinuationState =
                if (pending != null && state.flowScenario() == NativeAuthV2FlowScenario.SIGN_UP &&
                    !state.hasSubmittedAttribute(ATTRIBUTE_NAME_PASSWORD)) {
                    state.withPendingSignUpAttributes(pending.orEmpty())
                } else state

            when (val current = result) {
                is NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired ->
                    result = NativeAuthV2InteractionApiResult.ChallengeRequired(
                        current.correlationId, current.continuationState, hint = null, methods = current.methods)
                is NativeAuthV2InteractionApiResult.ChallengeRequired -> {
                    val state = retain(current.continuationState)
                    val methods = current.methods.filter { it.type == "email" || it.type == "sms" || it.type == "password" }
                    if (methods.isEmpty()) return INativeAuthCommandResult.APIError(
                        if (state.flowScenario() == NativeAuthV2FlowScenario.SIGN_IN) "unsupported_first_factor" else "unsupported_challenge_method",
                        "Native Auth V2 did not offer a recognized authentication method.", correlationId = current.correlationId)
                    if (methods.size > 1 || state.isMultiFactorAuthentication()) {
                        return NativeAuthV2CommandResult.AuthMethodSelectionRequired(current.correlationId, state, methods)
                    }
                    retryState = state
                    result = strategy.performMethodChallenge(state, methods.single().id)
                }
                is NativeAuthV2InteractionApiResult.CodeRequired -> {
                    val state = retain(current.continuationState)
                    return when {
                        state.isMultiFactorAuthentication() -> NativeAuthV2CommandResult.MFAVerificationRequired(
                            current.correlationId, state, current.codeLength, current.challengeTargetLabel, current.challengeChannel)
                        else -> NativeAuthV2CommandResult.CodeRequired(
                            current.correlationId, state, current.codeLength, current.challengeTargetLabel, current.challengeChannel)
                    }
                }
                is NativeAuthV2InteractionApiResult.PasswordRequired -> {
                    val state = retain(current.continuationState)
                    return NativeAuthV2CommandResult.PasswordRequired(current.correlationId, state)
                }
                is NativeAuthV2InteractionApiResult.UpdateRequired ->
                    return NativeAuthV2CommandResult.NewPasswordRequired(current.correlationId, retain(current.continuationState))
                is NativeAuthV2InteractionApiResult.RegistrationRequired ->
                    return NativeAuthV2CommandResult.StrongAuthRegistrationRequired(current.correlationId, retain(current.continuationState), current.methods)
                is NativeAuthV2InteractionApiResult.ActivationRequired ->
                    return NativeAuthV2CommandResult.StrongAuthVerificationRequired(current.correlationId,
                        retain(current.continuationState), current.codeLength, current.challengeTargetLabel,
                        current.challengeChannel)
                is NativeAuthV2InteractionApiResult.AttributesRequired -> {
                    val state = retain(current.continuationState)
                    if (state.flowScenario() == NativeAuthV2FlowScenario.SIGN_UP &&
                        current.requiredAttributes.any { it.name.equals(ATTRIBUTE_NAME_EMAIL, true) }) {
                        return attributeAlreadySubmitted(current.correlationId, "Email cannot be collected as an attribute.")
                    }
                    if (state.flowScenario() == NativeAuthV2FlowScenario.SIGN_UP &&
                        current.requiredAttributes.any { isSdkOwnedSignUpAttribute(it.name) && state.hasSubmittedAttribute(it.name) }) {
                        return attributeAlreadySubmitted(current.correlationId, "The server re-requested an already-submitted credential.")
                    }
                    val attributes = pending ?: state.pendingSignUpAttributesForSubmission().orEmpty()
                    if (attributes.isNotEmpty()) {
                        pending = null
                        retryState = state.withAdditionalSubmittedAttributes(attributes.keys)
                        result = strategy.performSubmitAttributes(state, attributes)
                    } else {
                        return if (current.requiredAttributes.any { it.name.equals(ATTRIBUTE_NAME_PASSWORD, true) }) {
                            NativeAuthV2CommandResult.PasswordRequired(current.correlationId, state)
                        } else NativeAuthV2CommandResult.AttributesRequired(current.correlationId, state, current.requiredAttributes)
                    }
                }
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> {
                    val state = if (current.continuationState.pendingSignUpAttributesForSubmission() != null) {
                        current.continuationState.withoutPendingSignUpAttributes()
                    } else current.continuationState
                    return when (state.flowScenario()) {
                        NativeAuthV2FlowScenario.SIGN_UP -> NativeAuthV2CommandResult.SignInAfterSignUpRequired(current.correlationId, state)
                        NativeAuthV2FlowScenario.RESET_PASSWORD ->
                            if (state.isResetPasswordUpdateSubmitted()) {
                                NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(current.correlationId, state)
                            } else mapInteractionError(current)
                        NativeAuthV2FlowScenario.SIGN_IN -> completeSignIn(strategy, parameters.toBuilder(),
                            state.scopesForTokenRequest(), state.claimsRequestJsonForTokenRequest(), state)
                    }
                }
                is NativeAuthV2InteractionApiResult.PollInProgress -> {
                    if (polls++ == MAX_POLL_ATTEMPTS) return NativeAuthV2CommandResult.PasswordResetFailed(
                        current.correlationId, POLL_TIMEOUT_ERROR, POLL_TIMEOUT_DESCRIPTION)
                    retryState = retain(current.continuationState)
                    ThreadUtils.sleepSafely((current.retryAfterMillis ?: FALLBACK_POLL_DELAY_MS)
                        .coerceIn(0L, MAX_POLL_DELAY_MS).toInt(), TAG, "Waiting between Native Auth V2 polls")
                    if (Thread.currentThread().isInterrupted) return INativeAuthCommandResult.APIError(
                        POLL_INTERRUPTED_ERROR, POLL_INTERRUPTED_DESCRIPTION, correlationId = current.correlationId)
                    result = strategy.performPoll(retryState)
                }
                is NativeAuthV2InteractionApiResult.InvalidCode -> return NativeAuthV2CommandResult.IncorrectCode(
                    current.correlationId, current.error, current.errorDescription, current.subError,
                    current.errorCodes, current.traceId, current.timestamp, current.attributeValidationDetails)
                is NativeAuthV2InteractionApiResult.InvalidCredentials -> return NativeAuthV2CommandResult.IncorrectPassword(
                    current.correlationId, current.error, current.errorDescription, current.subError,
                    current.errorCodes, current.traceId, current.timestamp, current.attributeValidationDetails)
                is NativeAuthV2InteractionApiResult.InvalidPassword -> return NativeAuthV2CommandResult.PasswordNotAccepted(
                    current.correlationId, current.error, current.errorDescription, current.subError,
                    current.errorCodes, current.traceId, current.timestamp, current.attributeValidationDetails)
                is NativeAuthV2InteractionApiResult.InvalidAttributes -> return NativeAuthV2CommandResult.AttributesInvalid(
                    current.correlationId, retryState, current.invalidAttributes, current.error, current.errorDescription,
                    current.errorCodes, current.subError, current.attributeValidationDetails, current.traceId, current.timestamp)
                is NativeAuthV2InteractionApiResult.UserAlreadyExists -> return NativeAuthV2CommandResult.UserAlreadyExists(
                    current.correlationId, current.error, current.errorDescription, current.errorCodes,
                    current.subError, current.attributeValidationDetails, current.traceId, current.timestamp)
                is NativeAuthV2InteractionApiResult.UserNotFound -> return NativeAuthV2CommandResult.UserNotFound(
                    current.correlationId, current.error, current.errorDescription, current.errorCodes,
                    current.traceId, current.timestamp, current.subError, current.attributeValidationDetails)
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    current.correlationId, current.redirectReason, current.errorCodes, current.traceId, current.timestamp)
                else -> return mapInteractionError(current)
            }
        }
    }

    private fun attributeAlreadySubmitted(correlationId: String, message: String) =
        INativeAuthCommandResult.APIError(ATTRIBUTE_ALREADY_SUBMITTED_ERROR, message, correlationId = correlationId)

    private fun validateSignUpAttributeKeys(attributes: Map<String, String>?, correlationId: String): SignUpCommandResult.InvalidAttributes? {
        val reserved = attributes?.keys?.filter(::isSdkOwnedSignUpAttribute).orEmpty()
        if (reserved.isEmpty()) return null
        return SignUpCommandResult.InvalidAttributes(
            error = "invalid_attributes",
            errorDescription = "The attribute names 'email' and 'password' are reserved by the SDK. Invalid attributes: ${reserved.joinToString()}.",
            invalidAttributes = reserved, correlationId = correlationId)
    }

    private fun isSdkOwnedSignUpAttribute(name: String) =
        name.equals(ATTRIBUTE_NAME_EMAIL, true) || name.equals(ATTRIBUTE_NAME_PASSWORD, true)

    fun signInAfterResetPassword(parameters: NativeAuthV2SignInAfterResetPasswordCommandParameters) =
        operation("signInAfterResetPassword", parameters) {
            val state = parameters.continuationState
            completeSignIn(createNativeAuthV2Strategy(parameters), parameters.toBuilder(),
                parameters.scopes ?: state.scopesForTokenRequest(),
                parameters.claimsRequestJson?.takeUnless { it.isBlank() } ?: state.claimsRequestJsonForTokenRequest(), state)
        }

    fun signInAfterSignUp(parameters: NativeAuthV2SignInAfterSignUpCommandParameters) =
        operation("signInAfterSignUp", parameters) {
            val state = parameters.continuationState
            completeSignIn(createNativeAuthV2Strategy(parameters), parameters.toBuilder(),
                parameters.scopes ?: state.scopesForTokenRequest(),
                parameters.claimsRequestJson?.takeUnless { it.isBlank() } ?: state.claimsRequestJsonForTokenRequest(), state)
        }

    private fun addV2DefaultScopes(scopes: List<String>?): List<String> {
        val requestedScopes = mutableListOf<String>()
        scopes.orEmpty().filterNotNull().filter { it.isNotEmpty() }.forEach { scope ->
            val defaultScope = AuthenticationConstants.DEFAULT_SCOPES.firstOrNull { it.equals(scope, ignoreCase = true) }
            if (defaultScope == null || defaultScope !in requestedScopes) requestedScopes.add(defaultScope ?: scope)
        }
        AuthenticationConstants.DEFAULT_SCOPES.forEach { if (it !in requestedScopes) requestedScopes.add(it) }
        return requestedScopes
    }

    private fun completeSignIn(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parametersBuilder: BaseSignInTokenCommandParameters.BaseSignInTokenCommandParametersBuilder<*, *>,
        scopes: List<String>?,
        claimsRequestJson: String?,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionCommandResult {
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            tokenCommandParameters = parametersBuilder
                .scopes(addV2DefaultScopes(scopes))
                .claimsRequestJson(claimsRequestJson?.takeUnless { it.isBlank() })
                .build(),
            state = state
        )
    }

    /**
     * Shared terminal path: continues the authorize-challenge interaction, exchanges the resulting
     * authorization code for tokens using [tokenCommandParameters]' merged scopes and claims, and
     * saves the account and tokens to the cache.
     */
    private fun exchangeCodeAndSaveTokens(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        tokenCommandParameters: BaseSignInTokenCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionCommandResult {
        LogSession.logMethodCall(tag = TAG, correlationId = state.correlationId, methodName = "$TAG.exchangeCodeAndSaveTokens")
        val continueResult = oAuth2Strategy.performAuthorizeChallengeContinue(state = state)
        val code = when (continueResult) {
            is AuthorizeChallengeApiResult.AuthorizationCode -> continueResult.code
            is AuthorizeChallengeApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                correlationId = continueResult.correlationId,
                traceId = continueResult.traceId,
                timestamp = continueResult.timestamp,
                errorCodes = continueResult.errorCodes,
                redirectReason = continueResult.redirectReason
            )
            is AuthorizeChallengeApiResult.ContinuationRequired -> return INativeAuthCommandResult.APIError(
                error = UNEXPECTED_RESULT,
                errorDescription = "ContinuationRequired returned unexpectedly at authorize-challenge continue.",
                correlationId = continueResult.correlationId
            )
            is AuthorizeChallengeApiResult.UnknownError -> return INativeAuthCommandResult.APIError(
                error = continueResult.error,
                errorDescription = continueResult.errorDescription,
                traceId = continueResult.traceId,
                timestamp = continueResult.timestamp,
                errorCodes = continueResult.errorCodes,
                subError = continueResult.subError,
                attributeValidationDetails = continueResult.attributeValidationDetails,
                correlationId = continueResult.correlationId
            )
        }
        val tokenResult = oAuth2Strategy.performTokenRequest(
            code = code,
            scopes = tokenCommandParameters.scopes ?: emptyList(),
            correlationId = state.correlationId,
            claimsRequestJson = tokenCommandParameters.claimsRequestJson
        )
        val successTokenResult = when (tokenResult) {
            is SignInTokenApiResult.Success -> tokenResult
            is SignInTokenApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                correlationId = tokenResult.correlationId,
                traceId = tokenResult.traceId,
                timestamp = tokenResult.timestamp,
                errorCodes = tokenResult.errorCodes,
                redirectReason = tokenResult.redirectReason
            )
            else -> {
                tokenResult as ApiErrorResult
                return INativeAuthCommandResult.APIError(
                    error = tokenResult.error,
                    errorDescription = tokenResult.errorDescription,
                    traceId = tokenResult.traceId,
                    timestamp = tokenResult.timestamp,
                    errorCodes = tokenResult.errorCodes,
                    subError = tokenResult.subError,
                    attributeValidationDetails = tokenResult.attributeValidationDetails,
                    correlationId = tokenResult.correlationId
                )
            }
        }
        val complete = saveAndReturnTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = tokenCommandParameters,
            tokenApiResult = successTokenResult
        )
        return NativeAuthV2CommandResult.Complete(complete.correlationId, complete.authenticationResult, null, null)
    }

    /** Only unsupported/malformed responses and domain-validation failures reach this helper. */
    private fun mapInteractionError(result: NativeAuthV2InteractionApiResult): INativeAuthCommandResult.APIError {
        Logger.warnWithObject(TAG, result.correlationId, "Unexpected interaction result: ", result)
        val apiError = result as? ApiErrorResult
        return INativeAuthCommandResult.APIError(
            error = apiError?.error ?: UNEXPECTED_RESULT,
            errorDescription = apiError?.errorDescription ?: "Unexpected API result: $result",
            errorCodes = apiError?.errorCodes,
            correlationId = result.correlationId,
            subError = apiError?.subError,
            attributeValidationDetails = apiError?.attributeValidationDetails,
            traceId = apiError?.traceId, timestamp = apiError?.timestamp
        )
    }
}
