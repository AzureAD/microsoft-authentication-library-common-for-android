//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.nativeauth.internal.commands

import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitCodeCommandResult
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController

/**
 * Compatibility wrapper for reset-password submit-code consumers using the original generic name.
 *
 * TODO: Remove after MSAL and other consumers migrate to
 * [NativeAuthV2ResetPasswordSubmitCodeCommand].
 */
@Deprecated(
    message = "Use NativeAuthV2ResetPasswordSubmitCodeCommand for explicit flow naming.",
    replaceWith = ReplaceWith(
        "NativeAuthV2ResetPasswordSubmitCodeCommand(parameters, controller, publicApiId)"
    )
)
class NativeAuthV2SubmitCodeCommand(
    private val parameters: NativeAuthV2SubmitCodeCommandParameters,
    private val controller: NativeAuthV2FlowController,
    publicApiId: String
) : BaseNativeAuthCommand<NativeAuthV2SubmitCodeCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = NativeAuthV2SubmitCodeCommand::class.java.simpleName
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the submitCode method of the native auth V2 controller with the given parameters.
     */
    override fun execute(): NativeAuthV2SubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.execute"
        )

        val result = controller.submitCode(
            parameters = parameters
        )

        Logger.infoWithObject(
            TAG,
            parameters.getCorrelationId(),
            "Returning result: ",
            result
        )
        return result
    }
}
