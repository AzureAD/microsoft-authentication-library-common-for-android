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

import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordSubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

/**
 * Command class to call controllers to submit the user's new password to the server in the self service password reset flow
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
class ResetPasswordSubmitNewPasswordCommand(
    private val parameters: ResetPasswordSubmitNewPasswordCommandParameters,
    private val controller: NativeAuthMsalController,
    publicApiId: String
) : BaseNativeAuthCommand<ResetPasswordSubmitNewPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = ResetPasswordSubmitNewPasswordCommand::class.java.simpleName
        const val POLL_COMPLETION_TIMEOUT_IN_MILISECONDS = 300000 // 5 minutes
        const val POLL_COMPLETION_TIMEOUT_ERROR_CODE = "timeout"
        const val POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION = "Command timed out while polling for password reset result."
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the resetPasswordSubmitNewPassword method of the native auth MSAL controller with the given parameters.
     */
    override fun execute(): ResetPasswordSubmitNewPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.execute"
        )

        val result = controller.resetPasswordSubmitNewPassword(
            parameters = parameters
        )

        Logger.info(
            TAG,
            parameters.getCorrelationId(),
            "Returning result: $result"
        )
        return result
    }
}
