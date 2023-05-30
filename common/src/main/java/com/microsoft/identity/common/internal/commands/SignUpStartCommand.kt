package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignUpStartCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpStartCommandResult

class SignUpStartCommand(
    private val parameters: BaseSignUpStartCommandParameters,
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
