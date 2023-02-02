package com.microsoft.identity.common.exception

import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.java.exception.BaseException

/**
 * An exception that represents an error where MSAL cannot reach Broker (i.e. through Bind Service or AccountManager).
 */
class BrokerCommunicationException(
    val category: Category,
    val strategyType: IIpcStrategy.Type,
    errorMessage: String?,
    throwable: Throwable?) :
    BaseException(category.errorCode, errorMessage, throwable) {

    enum class Category(val errorCode: String) {
        // The operation is not supported on the client (calling) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE("ipc_operation_not_supported_on_client_side"),  // The operation is not supported on the server (target) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE("ipc_operation_not_supported_on_server_side"),  // IPC connection failed due to an error.
        CONNECTION_ERROR("ipc_connection_error");

        override fun toString(): String {
            return errorCode
        }
    }

    override fun isCacheable(): Boolean {
        return category != Category.CONNECTION_ERROR
    }

    companion object {
        private const val serialVersionUID = 4959278068787428329L
    }
}