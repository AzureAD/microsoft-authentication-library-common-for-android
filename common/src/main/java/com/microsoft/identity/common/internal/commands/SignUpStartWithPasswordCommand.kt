package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpStartCommandResult

class SignUpStartWithPasswordCommand(
    private val parameters: SignUpStartWithPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpStartCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignUpStartCommandResult {
        return controller.signUpStart(
            parameters = parameters
        )
    }
}
