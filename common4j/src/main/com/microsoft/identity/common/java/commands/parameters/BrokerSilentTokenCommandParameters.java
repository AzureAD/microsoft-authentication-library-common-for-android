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
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.util.StringUtil;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BrokerSilentTokenCommandParameters extends SilentTokenCommandParameters implements IBrokerTokenCommandParameters {

    @Expose
    private final int callerUid;

    @Expose
    private final String callerAppVersion;

    @Expose
    private final String brokerVersion;

    private final IBrokerAccount brokerAccount;
    private final String homeAccountId;
    private final String localAccountId;
    private final int sleepTimeBeforePrtAcquisition;

    @Expose
    private final String negotiatedBrokerProtocolVersion;

    // If this flag is true, we will send the x-ms-PKeyAuth Header to the token endpoint.
    // Note: this flag is transferred to a MicrosoftTokenRequest in BaseController.
    @Expose
    private final boolean pKeyAuthHeaderAllowed;

    @Expose
    private final BrokerRequestType requestType;

    @Expose
    private final String homeTenantId;

    private final IWorkplaceJoinData workplaceJoinData;

    @Override
    public void validate() throws ArgumentException {
        if (callerUid == 0) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerUId", "Caller Uid is not set"
            );
        }
        if (getAuthority() == null) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mAuthority", "Authority Url is not set"
            );
        }
        if (getScopes() == null || getScopes().isEmpty()) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mScopes", "Scope or resource is not set"
            );
        }
        if (StringUtil.isNullOrEmpty(getClientId())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mClientId", "Client Id is not set"
            );
        }

        if (!getPlatformComponents().getPlatformUtil().isValidCallingApp(getRedirectUri(), getCallerPackageName())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mRedirectUri", "The redirect URI doesn't match the uri" +
                    " generated with caller package name and signature"
            );
        }

        if (!(getOAuth2TokenCache() instanceof BrokerOAuth2TokenCache)) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "AcquireTokenSilentOperationParameters",
                    "OAuth2Cache not an instance of BrokerOAuth2TokenCache"
            );
        }
        if (null == brokerAccount) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                    "mCallerPackageName", "Broker Account is null"
            );
        }

    }
}
