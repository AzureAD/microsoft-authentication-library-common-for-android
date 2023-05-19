package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult

class SignInSubmitPasswordCommand(
    private val parameters: SignInSubmitPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignInSubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignInSubmitPasswordCommandResult {
        return controller.signInSubmitPassword(
            parameters = parameters
        )
    }
}
