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
