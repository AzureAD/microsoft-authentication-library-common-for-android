// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc

import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.logging.Logger

/**
 * Abstract class for [IIpcStrategy] classes,
 * will verify the state of the service before making an actual request.
 *
 * @param shouldBypassSupportValidation if set to true, will bypass [isSupportedByTargetedApp]
 *        expose for testing only.
 **/
abstract class AbstractIpcStrategyWithServiceValidation(
    private val shouldBypassSupportValidation: Boolean = false): IIpcStrategy {
    companion object {
        val TAG = AbstractIpcStrategyWithServiceValidation::class.simpleName
    }

    /**
     * Contains the actual operation after verified by [isSupportedByTargetedApp]
     * that the operation is supported on the other side.
     *
     * @param bundle a [BrokerOperationBundle] object.
     * @return a response bundle (returned from the active broker).
     */
    @Throws(BrokerCommunicationException::class)
    protected abstract fun communicateToBrokerAfterValidation(bundle: BrokerOperationBundle): Bundle?

    /**
     * Returns true if the target package name supports this strategy.
     */
    abstract fun isSupportedByTargetedApp(targetedBrokerPackageName: String): Boolean

    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        if (!shouldBypassSupportValidation && !isSupportedByTargetedApp(bundle.targetBrokerAppPackageName)) {
            val message = "Operation $type is not supported on ${bundle.targetBrokerAppPackageName}"
            Logger.info(methodTag, message)
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                type, message, null
            )
        }

        return communicateToBrokerAfterValidation(bundle)
    }

}