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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitPasswordCommandResult
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController

/**
 * Command class to call controllers to submit a password from the V2 password-required state.
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
class NativeAuthV2SubmitPasswordCommand(
    private val parameters: NativeAuthV2SubmitPasswordCommandParameters,
    private val controller: NativeAuthV2FlowController,
    publicApiId: String
) : BaseNativeAuthCommand<NativeAuthV2SubmitPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = NativeAuthV2SubmitPasswordCommand::class.java.simpleName
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the submitPassword method of the native auth V2 controller with the given parameters.
     */
    override fun execute(): NativeAuthV2SubmitPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.execute"
        )

        val result = controller.submitPassword(
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
