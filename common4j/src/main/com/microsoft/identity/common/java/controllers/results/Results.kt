package com.microsoft.identity.common.java.controllers.results

object Redirect: SignInStartCommandResult, SignInSubmitCodeCommandResult, SignInResendCodeCommandResult
data class UnknownError(val errorCode: String?, val errorDescription: String?): SignInStartCommandResult,
    SignInSubmitCodeCommandResult, SignInResendCodeCommandResult