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

package com.microsoft.identity.common.exception;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.java.exception.BaseException;

import lombok.Getter;

/**
 * An exception that represents an error where MSAL cannot reach Broker (i.e. through Bind Service or AccountManager).
 */
public class BrokerCommunicationException extends BaseException {
    private static final long serialVersionUID = 4959278068787428329L;

    public enum Category {
        // The operation is not supported on the client (calling) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE("ipc_operation_not_supported_on_client_side"),

        // The operation is not supported on the server (target) side of IPC connection.
        OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE("ipc_operation_not_supported_on_server_side"),

        // IPC connection failed due to an error.
        CONNECTION_ERROR("ipc_connection_error");

        final String name;

        Category(@NonNull final String name) {
            this.name = name;
        }

        @Override
        public @NonNull
        String toString() {
            return this.name;
        }
    }

    @Getter
    private final Category category;

    @Getter
    private final IIpcStrategy.Type strategyType;

    /**
     * Initiates the {@link BrokerCommunicationException} with error message and throwable.
     *
     * @param errorMessage The error message contained in the exception.
     * @param throwable    The {@link Throwable} contains the cause for the exception.
     */
    public BrokerCommunicationException(final Category category,
                                        final IIpcStrategy.Type strategyType,
                                        final String errorMessage, final Throwable throwable) {
        super(category.toString(), errorMessage, throwable);
        this.category = category;
        this.strategyType = strategyType;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] [%s] :%s", category == null ? "" : category.toString(), strategyType == null ? "" : strategyType.toString(), super.getMessage());
    }

    @Override
    public boolean isCacheable() {
        return category != Category.CONNECTION_ERROR;
    }
}
