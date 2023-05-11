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
package com.microsoft.identity.common.exception

import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.java.exception.BaseException

/**
 * An exception that represents an error where MSAL cannot reach Broker (i.e. through Bind Service or AccountManager).
 *
 * @param errorMessage The error message contained in the exception.
 * @param throwable    The [Throwable] contains the cause for the exception.
 */
class BrokerCommunicationException(
    val category: Category,
    val strategyType: IIpcStrategy.Type,
    errorMessage: String?,
    throwable: Throwable?) :
    BaseException(category.toString(), errorMessage, throwable) {

    companion object {
        private const val serialVersionUID = 4959278068787428329L
    }

    enum class Category(private val categoryName: String) {
        // The operation is not supported on the client (calling) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE("ipc_operation_not_supported_on_client_side"),  // The operation is not supported on the server (target) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE("ipc_operation_not_supported_on_server_side"),  // IPC connection failed due to an error.
        CONNECTION_ERROR("ipc_connection_error");

        override fun toString(): String {
            return categoryName
        }
    }

    override val message: String
        // BaseException would try to emit telemetry event in its constructor.
        // At that point... variables here are not yet initialized..
        get() = String.format(
            "[%s] [%s] :%s",
            category?.toString(),
            strategyType?.toString(),
            super.message
        )

    override fun isCacheable(): Boolean {
        return category != Category.CONNECTION_ERROR
    }
}