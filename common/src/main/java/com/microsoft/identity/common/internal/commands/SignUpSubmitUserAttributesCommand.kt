package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitUserAttributesCommandResult

class SignUpSubmitUserAttributesCommand(
    private val parameters: SignUpSubmitUserAttributesCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SignUpSubmitUserAttributesCommandResult>(
    parameters,
    controller,
    publicApiId
) {
    override fun execute(): SignUpSubmitUserAttributesCommandResult {
        return controller.signUpSubmitUserAttributes(
            parameters = parameters
        )
    }
}
