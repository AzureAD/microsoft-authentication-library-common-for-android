package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpResendCodeCommandResult

class SignUpResendCodeCommand(
    private val parameters: SignUpResendCodeCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpResendCodeCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignUpResendCodeCommandResult {
        return controller.signUpResendCode(
            parameters = parameters
        )
    }
}
