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

import com.microsoft.identity.common.nativeauth.internal.controllers.BaseNativeAuthController
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.nativeauth.java.commands.parameters.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ClientException
import lombok.EqualsAndHashCode

/**
 * The implementation of the basis for native authentication commands.
 */
@EqualsAndHashCode(callSuper = true)
abstract class BaseNativeAuthCommand<T>(
    parameters: BaseNativeAuthCommandParameters,
    controller: BaseNativeAuthController,
    publicApiId: String
) : BaseCommand<T>(
    parameters,
    controller,
    object : CommandCallback<T, BaseException> {
        override fun onCancel() {
            onError(ClientException("onCancel not supported in native authentication flows"))
        }

        override fun onTaskCompleted(t: T) {
            // Do nothing. This class should be used in combination with CommandDispatcher.submitSilentReturningFuture
            // meaning the task result will be a future, and handled at call site.
        }

        override fun onError(error: BaseException) {
            // Do nothing. This class should be used in combination with CommandDispatcher.submitSilentReturningFuture
            // meaning the task result will be a future, and handled at call site.
        }
    },
    publicApiId
) {

    // TODO update this once telemetry is done
    override fun willReachTokenEndpoint(): Boolean {
        return false
    }

    // TODO update this once telemetry is done
    override fun isEligibleForEstsTelemetry(): Boolean {
        return false
    }
}
