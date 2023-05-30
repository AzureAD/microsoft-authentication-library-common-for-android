package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitCodeCommandResult

class SignUpSubmitCodeCommand(
    private val parameters: SignUpSubmitCodeCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitCodeCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignUpSubmitCodeCommandResult {
        return controller.signUpSubmitCode(
            parameters = parameters
        )
    }
}
