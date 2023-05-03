package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInResendCodeCommandResult

class SignInResendCodeCommand(
    private val parameters: SignInResendCodeCommandParameters,
    private val controller: NativeAuthController,
    callback: BaseNativeAuthCommandCallback<SignInResendCodeCommandResult>,
    publicApiId: String
) : BaseNativeAuthCommand<SignInResendCodeCommandResult>(
    parameters,
    controller,
    callback,
    publicApiId
) {
    override fun execute(): SignInResendCodeCommandResult {
        return controller.signInResendCode(
            parameters = parameters
        )
    }
}
