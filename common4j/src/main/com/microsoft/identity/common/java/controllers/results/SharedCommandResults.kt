package com.microsoft.identity.common.java.controllers.results

import com.microsoft.identity.common.java.util.CommonUtils

interface CommandResult {
    data class Redirect(val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()) :
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult, SignInResendCodeCommandResult,
        SignInSubmitPasswordCommandResult, SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpResendCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult, ResetPasswordResendCodeCommandResult

    data class UnknownError(
        override val error: String?,
        override val errorDescription: String?,
        override val details: List<Map<String, String>>? = null,
        override val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
        override val errorCodes: List<Int>? = null
    ): Error(error, errorDescription, details, correlationId, errorCodes),
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult, SignInResendCodeCommandResult,
        SignInSubmitPasswordCommandResult, SignUpStartCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult, SignUpResendCodeCommandResult,
        SignUpSubmitPasswordCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult, ResetPasswordSubmitNewPasswordCommandResult

    open class Error(
        open val error: String?,
        open val errorDescription: String?,
        open val details: List<Map<String, String>>? = null,
        open val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
        open val errorCodes: List<Int>? = null
    )
}
