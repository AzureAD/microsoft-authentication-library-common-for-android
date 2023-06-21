package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitUserAttributesCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

class SignUpSubmitUserAttributesCommand(
    private val parameters: SignUpSubmitUserAttributesCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitUserAttributesCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignUpSubmitUserAttributesCommand::class.java.simpleName
    }

    override fun execute(): SignUpSubmitUserAttributesCommandResult {
        LogSession.logMethodCall(TAG)

        val result = controller.signUpSubmitUserAttributes(
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
