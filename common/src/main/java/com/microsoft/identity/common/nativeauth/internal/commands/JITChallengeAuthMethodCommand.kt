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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITChallengeAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.JITChallengeAuthMethodCommandResult
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController

/**
 * Command class to call controllers to trigger register/challenge JIT endpoint.
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
class JITChallengeAuthMethodCommand(
    private val parameters: JITChallengeAuthMethodCommandParameters,
    private val controller: NativeAuthMsalController,
    publicApiId: String
) : BaseNativeAuthCommand<JITChallengeAuthMethodCommandResult>(
    parameters,
    controller,
    publicApiId
) {

    companion object {
        private val TAG = JITChallengeAuthMethodCommand::class.java.simpleName
    }

    /**
     * The execution part of the command, to be run on the background thread.
     * It calls the signInChallenge method of the native auth MSAL controller with the given parameters.
     */
    override fun execute(): JITChallengeAuthMethodCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.execute"
        )
        val result = controller.jitChallengeAuthMethod(
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