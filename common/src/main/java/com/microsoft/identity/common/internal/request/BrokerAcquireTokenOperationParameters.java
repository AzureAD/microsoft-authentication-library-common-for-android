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
package com.microsoft.identity.common.internal.request;

import android.accounts.AccountAuthenticatorResponse;
import android.text.TextUtils;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.cache.BrokerOAuth2TokenCache;

public class BrokerAcquireTokenOperationParameters extends AcquireTokenOperationParameters {

    public enum RequestType{

        /**
         * Request type indicates regular acquire token request from adal or msal, Default value.
         */
        REGULAR,

        /**
         * Request type indicates a token request to get Broker Refresh Token while doing WPJ.
         */
        BROKER_RT_REQUEST,

        /**
         * Request type indicates a token request which is performed during an interrupt flow.
         */
        RESOLVE_INTERRUPT

    }

    private String mCallerPackageName;

    private int mCallerUId;

    private String mCallerAppVersion;

    private String mCorrelationId;

    private RequestType mRequestType = RequestType.REGULAR;

    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    public void setCallerPackageName(final String callerPackageName) {
        this.mCallerPackageName = callerPackageName;
    }

    public int getCallerUId() {
        return mCallerUId;
    }

    public void setCallerUId(int callerUId) {
        this.mCallerUId = callerUId;
    }

    public String getCallerAppVersion() {
        return mCallerAppVersion;
    }

    public void setCallerAppVersion(final String callerAppVersion) {
        this.mCallerAppVersion = callerAppVersion;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.mCorrelationId = correlationId;
    }

    public RequestType getRequestType() {
        return mRequestType;
    }

    public void setRequestType(RequestType requestType) {
        this.mRequestType = requestType;
    }


    @Override
    public void validate() throws ArgumentException {
        super.validate();
        if (getAuthority() == null) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                    "mAuthority", "Authority Url is not set"
            );
        }
        if (getScopes() == null || getScopes().isEmpty()) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                    "mScopes", "Scope or resource is not set"
            );
        }
        if (TextUtils.isEmpty(getClientId())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                    "mClientId", "Client Id is not set"
            );
        }
        if (TextUtils.isEmpty(getRedirectUri())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                    "mRedirectUri", "Redirect Uri is not set"
            );
        }
        // If the request type is BROKER_RT_REQUEST, it means the caller here would be broker itself so
        // calling package name and calling uid will be null, otherwise we need to validate that these are
        // not null for successfully storing tokens in cache.
        if(!isRequestFromBroker()) {
            if (mCallerUId == 0) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "mCallerUId", "Caller Uid is not set"
                );
            }
            if (TextUtils.isEmpty(mCallerPackageName)) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "mCallerPackageName", "Caller package name is not set"
                );
            }
            if (!(getTokenCache() instanceof BrokerOAuth2TokenCache)) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "AcquireTokenSilentOperationParameters",
                        "OAuth2Cache not an instance of BrokerOAuth2TokenCache"
                );
            }
        }
    }

    /**
     * Helper method to identify if the request originated from Broker itself or from client libraries.
     *
     * @return : true if request is the request is originated from Broker, false otherwise
     */
    public boolean isRequestFromBroker() {
        return mRequestType == BrokerAcquireTokenOperationParameters.RequestType.BROKER_RT_REQUEST ||
                mRequestType == BrokerAcquireTokenOperationParameters.RequestType.RESOLVE_INTERRUPT;
    }

}
