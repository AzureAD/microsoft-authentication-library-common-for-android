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
package com.microsoft.identity.common.internal.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

public class BrokerRemoveAccountRequest implements Serializable {

    @NonNull
    @SerializedName(ACCOUNT_CLIENTID_KEY)
    private String mClientId;

    @Nullable
    @SerializedName(ENVIRONMENT)
    private String mEnvironment;

    /**
     * Home account id of the user. Needs to be set for silent request
     */
    @Nullable
    @SerializedName(ACCOUNT_HOME_ACCOUNT_ID)
    private String mHomeAccountId;

    @NonNull
    public String getClientId() {
        return mClientId;
    }

    public void setClientId(@NonNull String clientId) {
        mClientId = clientId;
    }

    @Nullable
    public String getEnvironment() {
        return mEnvironment;
    }

    public void setEnvironment(@Nullable String environment) {
        mEnvironment = environment;
    }

    @Nullable
    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    public void setHomeAccountId(@Nullable String homeAccountId) {
        mHomeAccountId = homeAccountId;
    }
}
