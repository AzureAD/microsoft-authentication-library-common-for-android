package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

class SignUpSubmitCodeCommand(
    private val parameters: SignUpSubmitCodeCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitCodeCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignUpSubmitCodeCommand::class.java.simpleName
    }

    override fun execute(): SignUpSubmitCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")

        val result = controller.signUpSubmitCode(
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
