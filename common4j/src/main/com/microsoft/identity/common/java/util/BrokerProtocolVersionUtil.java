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

package com.microsoft.identity.common.java.util;


import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Class to provide util methods to compare different features with respect to Broker Protocol version.
 */
public class BrokerProtocolVersionUtil {

    public static final String MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION = "5.0";
    public static final String MSAL_TO_BROKER_PROTOCOL_ACCOUNT_FROM_PRT_CHANGES_MINIMUM_VERSION = "8.0";

    /**
     * Verifies if negotiated broker protocol version allows to decompressing/compressing broker payloads.
     *
     * @param negotiatedBrokerProtocol negotiated protocol version, result of hello handshake.
     * @return true if the negotiated protocol version is larger or equal than
     * the {@link BrokerProtocolVersionUtil#MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION}.
     */
    public static boolean canCompressBrokerPayloads(@Nullable String negotiatedBrokerProtocol) {
        return isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                negotiatedBrokerProtocol,
                MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION);
    }

    /**
     * Verifies if negotiated broker protocol version allows FOCI apps to construct accounts from PRT Id token.
     *
     * @param negotiatedBrokerProtocol negotiated protocol version, result of hello handshake.
     * @return true if the negotiated protocol version is larger or equal than
     * the {@link BrokerProtocolVersionUtil#MSAL_TO_BROKER_PROTOCOL_ACCOUNT_FROM_PRT_CHANGES_MINIMUM_VERSION}.
     */
    public static boolean canFociAppsConstructAccountsFromPrtIdTokens(@Nullable String negotiatedBrokerProtocol) {
        return isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                negotiatedBrokerProtocol,
                MSAL_TO_BROKER_PROTOCOL_ACCOUNT_FROM_PRT_CHANGES_MINIMUM_VERSION);
    }

    /**
     * Verify if the negotiated broker protocol id larger or equal that the requiredBrokerProtocol.
     *
     * @param negotiatedBrokerProtocol negotiated protocol version, result of hello handshake
     * @param requiredBrokerProtocol   minimun required protocol version for the feature.
     * @return true if the negotiated broker protocol larger or equal than required broker protocol,
     * false otherwise.
     */
    protected static boolean isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
            @Nullable final String negotiatedBrokerProtocol,
            @NonNull final String requiredBrokerProtocol) {

        if (StringUtil.isNullOrEmpty(negotiatedBrokerProtocol)) {
            return false;
        }
        return StringUtil.isFirstVersionLargerOrEqual(
                negotiatedBrokerProtocol,
                requiredBrokerProtocol);
    }
}
