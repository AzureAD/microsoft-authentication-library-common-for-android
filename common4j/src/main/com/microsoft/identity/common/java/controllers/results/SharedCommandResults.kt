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
        val error: String?,
        val errorDescription: String?,
        val details: List<Map<String, String>>? = null,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
        val errorCodes: List<Int>? = null
    ) :
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult, SignInResendCodeCommandResult,
        SignInSubmitPasswordCommandResult, SignUpStartCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult, SignUpResendCodeCommandResult,
        SignUpSubmitPasswordCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult, ResetPasswordSubmitNewPasswordCommandResult
}
