package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitPasswordCommandResult

class SignUpSubmitPasswordCommand(
    private val parameters: SignUpSubmitPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignUpSubmitPasswordCommandResult {
        return controller.signUpSubmitPassword(
            parameters = parameters
        )
    }
}
