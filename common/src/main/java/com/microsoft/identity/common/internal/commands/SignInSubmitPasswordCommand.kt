package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

class SignInSubmitPasswordCommand(
    private val parameters: SignInSubmitPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignInSubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignInSubmitPasswordCommand::class.java.simpleName
    }

    override fun execute(): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(TAG)

        val result = controller.signInSubmitPassword(
            parameters = parameters
        )

        LogSession.log(
            TAG,
            Logger.LogLevel.INFO,
            "Returning result: $result"
        )
        return result
    }
}
