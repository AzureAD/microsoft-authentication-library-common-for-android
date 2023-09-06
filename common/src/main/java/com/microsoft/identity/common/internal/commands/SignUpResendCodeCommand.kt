package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

class SignUpResendCodeCommand(
    private val parameters: SignUpResendCodeCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpResendCodeCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignUpResendCodeCommand::class.java.simpleName
    }

    override fun execute(): SignUpResendCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")

        val result = controller.signUpResendCode(
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
