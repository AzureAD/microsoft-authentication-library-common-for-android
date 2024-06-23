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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInWithContinuationTokenCommandResult
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger

/**
 * Command class to call controllers to use continuation token generated from sign in/self-service-password-reset flow to get the user authenticated.
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
class SignInWithContinuationTokenCommand(
    private val parameters: SignInWithContinuationTokenCommandParameters,
    private val controller: NativeAuthMsalController,
    publicApiId: String
) : BaseNativeAuthCommand<SignInWithContinuationTokenCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = SignInWithContinuationTokenCommand::class.java.simpleName
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the signInWithContinuationToken method of the native auth MSAL controller with the given parameters.
     */
    override fun execute(): SignInWithContinuationTokenCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.execute"
        )

        val result = controller.signInWithContinuationToken(
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
