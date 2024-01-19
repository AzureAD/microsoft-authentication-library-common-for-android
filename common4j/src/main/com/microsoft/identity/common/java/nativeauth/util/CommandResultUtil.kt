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
package com.microsoft.identity.common.java.nativeauth.util

import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.exception.ClientException

const val UNSUCCESSFUL_COMMAND_ERROR = "unsuccessful_command"
const val INVALID_USERNAME_ERROR = "username"

/**
 * CommandResult.result value can be of an expected type (ExpectedType), or, if an underlying exception
 * happened, CommandResult.result is an exception. This method checks for the runtime value of
 * CommandResult.result and returns the result value if it's of ExpectedType, or it wraps it in an
 * UnknownError in all other cases.
 */
inline fun <reified ExpectedType: com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult> CommandResult<Any>.checkAndWrapCommandResultType() : ExpectedType {
    // We first check the command status. If this is not COMPLETED, then most likely an exception
    // happened which we want to wrap
     if (this.status != ICommandResult.ResultStatus.COMPLETED) {
        var exception: Exception? = null
        var exceptionMessage: String? = ""

        if (this.result is Exception) {
            exception = this.result as Exception
            exceptionMessage = exception.message
        }

         return if (exception is ClientException && exception.errorCode == INVALID_USERNAME_ERROR) {
             com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult.InvalidUsername(
                 error = exception.errorCode,
                 errorDescription = exceptionMessage,
                 exception = exception,
                 correlationId = this.correlationId
             ) as ExpectedType
         } else {
             com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult.UnknownError(
                 error = UNSUCCESSFUL_COMMAND_ERROR,
                 errorDescription = exceptionMessage,
                 exception = exception,
                 correlationId = this.correlationId
             ) as ExpectedType
         }
    } else {
        return this.result.let { result ->
            when {
                // Extra check in case the status is COMPLETED, but the CommandResult.result value
                // is not of type ExpectedType
                (result is Exception) -> {
                    return@let com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult.UnknownError(
                        error = UNSUCCESSFUL_COMMAND_ERROR,
                        errorDescription = "Type casting error: result of $this is of type Exception, even though the command was marked as COMPLETED",
                        correlationId = this.correlationId
                    ) as ExpectedType
                }
                else -> {
                    return@let try {
                        result as ExpectedType
                    } catch (exception: java.lang.ClassCastException) {
                        com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult.UnknownError(
                            error = UNSUCCESSFUL_COMMAND_ERROR,
                            errorDescription = "Type casting error: result of $this is not of type ${ExpectedType::class}, but of type ${result::class}, even though the command was marked as COMPLETED",
                            correlationId = this.correlationId
                        ) as ExpectedType
                    }
                }
            }
        }
    }
}
