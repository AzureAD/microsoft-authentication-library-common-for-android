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
package com.microsoft.identity.common.java.commands.parameters;

import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.request.BrokerRequestType;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An interface that describes token command parameters for broker.
 */
public interface IBrokerTokenCommandParameters {

    /**
     * Get the package name of the calling application.
     *
     * @return a String representing caller package name
     */
    String getCallerPackageName();

    /**
     * Get the uid of the calling application.
     *
     * @return an int representing caller uid
     */
    int getCallerUid();

    /**
     * Get the app version of the calling application.
     *
     * @return a String representing caller app version
     */
    String getCallerAppVersion();

    /**
     * Get the broker version.
     *
     * @return a String representing broker version
     */
    String getBrokerVersion();

    /**
     * Get the broker account to use for this request
     *
     * @return an {@link IBrokerAccount} to use
     */
    IBrokerAccount getBrokerAccount();

    /**
     * Get the home account id of account to use for the request
     *
     * @return a String representing home account id
     */
    String getHomeAccountId();

    /**
     * Get the local account id of account to use for the request
     *
     * @return a String representing local account id
     */
    String getLocalAccountId();

    /**
     * Get the {@link BrokerRequestType} for the request.
     *
     * @return the {@link BrokerRequestType}
     */
    BrokerRequestType getRequestType();

    /**
     * Get the home tenant id being used for this request. This is particularly tenant id
     * requesting BRT and this is here for FLW telemetry purposes.
     *
     * @return a String representing tenant id
     */
    @Nullable
    String getHomeTenantId();

    /**
     * Get the negotiated broker protocol version.
     *
     * @return a String representing broker protocol version
     */
    String getNegotiatedBrokerProtocolVersion();

    /**
     * Helper method to identify if the request originated from Broker itself or from client libraries.
     *
     * @return : true if request is the request is originated from Broker, false otherwise
     */
    default boolean isRequestFromBroker() {
        return getRequestType() == BrokerRequestType.BROKER_RT_REQUEST ||
                getRequestType() == BrokerRequestType.RESOLVE_INTERRUPT;
    }
}
