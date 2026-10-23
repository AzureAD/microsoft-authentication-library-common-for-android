// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.nativeauth.internal.commands

import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ActivateCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2StrongAuthCommandResult
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController

/** Runs strong-auth activation through the existing native-auth command dispatcher. */
class NativeAuthV2ActivateCommand(
    private val parameters: NativeAuthV2ActivateCommandParameters,
    private val controller: NativeAuthV2FlowController,
    publicApiId: String
) : BaseNativeAuthCommand<NativeAuthV2StrongAuthCommandResult>(
    parameters.tokenParameters, controller, publicApiId
) {
    override fun execute(): NativeAuthV2StrongAuthCommandResult = controller.activate(parameters)
}
