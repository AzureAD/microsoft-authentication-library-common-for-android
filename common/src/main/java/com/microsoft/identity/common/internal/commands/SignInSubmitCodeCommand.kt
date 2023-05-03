package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInSubmitCodeCommandResult

class SignInSubmitCodeCommand(
    private val parameters: SignInSubmitCodeCommandParameters,
    private val controller: NativeAuthController,
    callback: BaseNativeAuthCommandCallback<SignInSubmitCodeCommandResult>,
    publicApiId: String
) : BaseNativeAuthCommand<SignInSubmitCodeCommandResult>(
    parameters,
    controller,
    callback,
    publicApiId
) {
    override fun execute(): SignInSubmitCodeCommandResult {
        return controller.signInSubmitCode(
            parameters = parameters
        )
    }
}
