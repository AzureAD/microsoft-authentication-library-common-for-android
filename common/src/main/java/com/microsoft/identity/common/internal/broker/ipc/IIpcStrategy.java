/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

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
        LEGACY_ACCOUNT_MANAGER_FOR_BROKER_API("legacy_account_manager_for_broker_api");

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
