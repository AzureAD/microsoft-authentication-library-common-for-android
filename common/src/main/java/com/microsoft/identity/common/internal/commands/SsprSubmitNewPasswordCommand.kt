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
package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.internal.controllers.NativeAuthController
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.SsprSubmitNewPasswordCommandResult

class SsprSubmitNewPasswordCommand(
    private val parameters: SsprSubmitNewPasswordCommandParameters,
    private val controller: NativeAuthController,
    publicApiId: String
) : BaseNativeAuthCommand<SsprSubmitNewPasswordCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        const val DEFAULT_POLL_COMPLETION_INTERVAL_IN_MILISECONDS = 5000
        const val POLL_COMPLETION_TIMEOUT_IN_MILISECONDS = 300000 // 5 minutes
        const val POLL_COMPLETION_TIMEOUT_ERROR_CODE = "timeout"
        const val POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION = "Command timed out while polling for password reset result."
    }

    override fun execute(): SsprSubmitNewPasswordCommandResult {
        return controller.ssprSubmitNewPassword(
            parameters = parameters
        )
    }
}
