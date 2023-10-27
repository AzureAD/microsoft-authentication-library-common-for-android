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

import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import com.microsoft.identity.common.java.controllers.BaseController
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.logging.Logger
import lombok.EqualsAndHashCode

/**
 * Command class to call controllers to check if QR code + PIN authorization is available.
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
@EqualsAndHashCode(callSuper = true)
class IsQrPinAvailableCommand : BaseCommand<Boolean?> {

    constructor(
        parameters: CommandParameters,
        controllers: List<BaseController?>,
        callback: CommandCallback<*, *>,
        publicApiId: String
    ) : super(parameters, controllers, callback, publicApiId)

    @Throws(Exception::class)
    override fun execute(): Boolean {
        val methodTag = "$TAG:execute"
        for (controller in getControllers()) {
            try {
                if (controller.isQrPinAvailable) {
                    return true
                }
            } catch (e: ClientException) {
                Logger.error(methodTag, "Failed to check if QR code + PIN authorization is available.", e)
                if (ErrorStrings.BROKER_BIND_SERVICE_FAILED.equals(e.errorCode, ignoreCase = true)) {
                    Logger.warn(methodTag, "Operation is not supported on the broker, returning false.")
                    return false
                }
            }
        }
        return false
    }

    override fun isEligibleForEstsTelemetry(): Boolean {
        return false
    }

    companion object {
        private val TAG = IsQrPinAvailableCommand::class.java.simpleName
    }
}