package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;

/**
 * an interface for inter-process communication strategies.
 */
public interface IIpcStrategy {

    enum Type {
        BOUND_SERVICE("bound_service"),
        ACCOUNT_MANAGER_ADD_ACCOUNT("account_manager_add_account"),
        CONTENT_PROVIDER("content_provider"),
        LEGACY_ACCOUNT_MANAGER_FOR_BROKER_API("legacy_account_manager_for_broker_api"),
        LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API("legacy_account_authenticator_for_wpj_api");

        final String name;

        Type(@NonNull final String name) {
            this.name = name;
        }

        @Override
        public @NonNull String toString() {
            return this.name;
        }
    }

    /**
     * Communicates with the target broker.
     *
     * @param bundle a {@link BrokerOperationBundle} object.
     * @return a response bundle (returned from the active broker).
     */
    @Nullable Bundle communicateToBroker(final @NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException;

    /**
     * Gets this strategy type.
     */
    Type getType();
}
