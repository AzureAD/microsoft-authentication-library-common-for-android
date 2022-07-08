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
    public static final boolean canCompressBrokerPayloads(@Nullable String negotiatedBrokerProtocol) {
        return isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                negotiatedBrokerProtocol,
                MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION);
    }

    /**
     * Verifies if negotiated broker protocol version allows FOCI apps to construct accounts from PRT Id token.
     *
     * @param clientRequiredBrokerProtocolVersion client protocol version.
     * @return true if the client protocol version is larger or equal than
     * the {@link BrokerProtocolVersionUtil#MSAL_TO_BROKER_PROTOCOL_ACCOUNT_FROM_PRT_CHANGES_MINIMUM_VERSION}.
     */
    public static final boolean canFociAppsConstructAccountsFromPrtIdTokens(@Nullable String clientRequiredBrokerProtocolVersion) {
        return isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                clientRequiredBrokerProtocolVersion,
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
    protected static final boolean isNegotiatedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
            @Nullable final String negotiatedBrokerProtocol,
            @NonNull final String requiredBrokerProtocol) {

        if (StringUtil.isNullOrEmpty(negotiatedBrokerProtocol)) {
            return false;
        }
        return isFirstVersionLargerOrEqual(
                negotiatedBrokerProtocol,
                requiredBrokerProtocol);
    }

    /**
     * Returns true if the first semantic version is smaller or equal to the second version.
     */
    public static final boolean isFirstVersionSmallerOrEqual(@NonNull final String first,
                                                             @Nullable final String second) {
        return compareSemanticVersion(first, second) <= 0;
    }

    /**
     * Returns true if the first semantic version is larger or equal to the second version.
     */
    public static final boolean isFirstVersionLargerOrEqual(@NonNull final String first,
                                                            @Nullable final String second) {
        return compareSemanticVersion(first, second) >= 0;
    }

    /**
     * The function to compare the two versions.
     *
     * @param thisVersion
     * @param thatVersion
     * @return int -1 if thisVersion is smaller than thatVersion,
     * 1 if thisVersion is larger than thatVersion,
     * 0 if thisVersion is equal to thatVersion.
     */
    public static final int compareSemanticVersion(@NonNull final String thisVersion,
                                                   @Nullable final String thatVersion) {
        if (thatVersion == null) {
            return 1;
        }

        final String[] thisParts = thisVersion.split("\\.");
        final String[] thatParts = thatVersion.split("\\.");

        final int length = Math.max(thisParts.length, thatParts.length);

        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;

            if (thisPart < thatPart) {
                return -1;
            }

            if (thisPart > thatPart) {
                return 1;
            }
        }

        return 0;
    }

}
