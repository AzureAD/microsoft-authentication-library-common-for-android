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
package com.microsoft.identity.common.internal.broker.ipc

import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException

/**
 * an interface for inter-process communication strategies.
 */
interface IIpcStrategy {
    enum class Type(val value: String) {
        BOUND_SERVICE("bound_service"),
        ACCOUNT_MANAGER_ADD_ACCOUNT("account_manager_add_account"),
        CONTENT_PROVIDER("content_provider"),
        LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API("legacy_account_authenticator_for_wpj_api");

        override fun toString(): String {
            return value
        }
    }

    /**
     * Communicates with the target broker.
     *
     * NOTE: If the operation is not supported, a [BrokerCommunicationException]
     * [BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE] will be thrown.
     *
     * @param bundle a [BrokerOperationBundle] object.
     * @return a response bundle (returned from the active broker).
     */
    @Throws(BrokerCommunicationException::class)
    fun communicateToBroker(bundle: BrokerOperationBundle): Bundle?

    /**
     * Returns true if the target package name supports this strategy.
     */
    fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean

    /**
     * Gets this strategy type.
     */
    fun getType(): Type
}
