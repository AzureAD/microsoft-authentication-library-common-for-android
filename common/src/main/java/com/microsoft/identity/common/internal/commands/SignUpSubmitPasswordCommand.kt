package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

class SignUpSubmitPasswordCommand(
    private val parameters: SignUpSubmitPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignUpSubmitPasswordCommand::class.java.simpleName
    }

    override fun execute(): SignUpSubmitPasswordCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")

        val result = controller.signUpSubmitPassword(
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
