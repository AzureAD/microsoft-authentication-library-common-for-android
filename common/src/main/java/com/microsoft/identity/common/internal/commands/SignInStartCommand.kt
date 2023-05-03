package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInStartCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult

class SignInStartCommand(
    private val parameters: BaseSignInStartCommandParameters,
    private val controller: NativeAuthController,
    callback: BaseNativeAuthCommandCallback<SignInStartCommandResult>,
    publicApiId: String
) : BaseNativeAuthCommand<SignInStartCommandResult>(
    parameters,
    controller,
    callback,
    publicApiId
) {
    override fun execute(): SignInStartCommandResult {
        return controller.signInStart(
            parameters = parameters
        )
    }
}
