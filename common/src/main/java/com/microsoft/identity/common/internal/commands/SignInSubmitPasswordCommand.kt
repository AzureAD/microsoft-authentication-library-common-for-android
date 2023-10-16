package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

/**
 * Command class to call controllers to submit the user's password to the server in the sign in flow
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
class SignInSubmitPasswordCommand(
    private val parameters: SignInSubmitPasswordCommandParameters,
    private val controller: NativeAuthMsalController,
    publicApiId: String
) : BaseNativeAuthCommand<SignInSubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignInSubmitPasswordCommand::class.java.simpleName
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the signInSubmitPassword method of the native auth MSAL controller with the given parameters.
     */
    override fun execute(): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")

        val result = controller.signInSubmitPassword(
            parameters = parameters
        )

        Logger.info(
            TAG,
            "Returning result: $result"
        )
        return result
    }
}
