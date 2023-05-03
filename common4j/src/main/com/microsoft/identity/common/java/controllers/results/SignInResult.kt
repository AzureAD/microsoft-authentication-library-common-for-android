package com.microsoft.identity.common.java.controllers.results

import com.microsoft.identity.common.java.result.ILocalAuthenticationResult

data class Complete(val authenticationResult: ILocalAuthenticationResult): SignInStartCommandResult,
    SignInSubmitCodeCommandResult
object InvalidAuthenticationType : SignInStartCommandResult

sealed interface SignInStartCommandResult
data class EmailVerificationRequired(val credentialToken: String, val codeLength: Int, val displayName: String): SignInStartCommandResult,
    SignInResendCodeCommandResult
data class UserNotFound(val errorCode: String, val errorDescription: String): SignInStartCommandResult
data class PasswordIncorrect(val errorCode: String, val errorDescription: String): SignInStartCommandResult

sealed interface SignInSubmitCodeCommandResult
data class IncorrectCode(val errorCode: String, val errorDescription: String):
    SignInSubmitCodeCommandResult

sealed interface SignInResendCodeCommandResult
