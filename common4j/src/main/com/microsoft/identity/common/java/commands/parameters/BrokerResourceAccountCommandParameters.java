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

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.request.BrokerRequestType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Command parameters used by the broker internally while executing
 * provision resource account request.
 */
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BrokerResourceAccountCommandParameters extends ResourceAccountCommandParameters
        implements IBrokerTokenCommandParameters {
    /**
     * UID of the calling application process as seen by the Broker.
     */
    @Expose
    private final int callerUid;

    /**
     * Version name/code of the calling application that initiated the request.
     */
    @Expose
    private final String callerAppVersion;

    /**
     * Version of the Broker this request.
     */
    @Expose
    private final String brokerVersion;

    /**
     * The AAD tenant id (home tenant) for the user account this request is for. It
     * is derived from the {@link #getHomeAccountId()} value and is used
     */
    @Expose
    @NonNull
    private final String homeTenantId;

    /**
     * The user id in home tenant retrieved from {@link #getHomeAccountId()}..
     */
    @NonNull
    private final String localAccountId;

    /**
     * {@link IBrokerAccount} if already present, otherwise null.
     */
    private final IBrokerAccount brokerAccount;

    /**
     * The protocol version that was negotiated between the calling app / library
     * and the Broker.
     */
    @Expose
    private final String negotiatedBrokerProtocolVersion;

    /**
     * For this parameter class it is always {@link BrokerRequestType#REGULAR} as this request not started in broker.
     */
    private final BrokerRequestType requestType = BrokerRequestType.REGULAR;
}
